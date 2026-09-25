package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.example.proyecto_evacuapp.data.remote.MapGraphResponseDto
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.IncidentType
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

private const val TAG = "RoadNetworkRepository"
private const val MAX_MATCH_DISTANCE_METERS = 60.0

private val RISK_INCIDENT_TYPES = setOf(
    IncidentType.BLOQUEO_VIAL,
    IncidentType.INCENDIO,
    IncidentType.INUNDACION,
    IncidentType.DERRUMBE,
    IncidentType.ACCIDENTE
)

private fun parseIncidentStatus(value: String): IncidentStatus? =
    IncidentStatus.entries.find { it.name.equals(value, ignoreCase = true) }

private fun parseAffectedSegmentIds(raw: String): Set<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

/**
 * Repositorio espacial: Deserializa directamente el JSON comprimido del grafo de 30 km del backend NestJS.
 * REGLA ESTRICTA: Si el JSON está vacío o no hay red local guardada, devuelve emptyList() y alerta
 * 'Ruta no encontrada sobre la red vial'. NUNCA dibuja líneas rectas ni fallbacks simplificados.
 */
class RoadNetworkRepository {
    private val graph = RoadGraph()
    private val mutex = Mutex()
    private var lastLoadedCenter: RouteCoordinate? = null

    suspend fun ensureLoadedForRoute(context: Context?, origin: RouteCoordinate, destination: RouteCoordinate) {
        withContext(Dispatchers.IO) {
            val center = lastLoadedCenter
            val distFromCenterMeters = if (center != null) haversineMeters(origin, center) else Double.MAX_VALUE

            if (center == null || distFromCenterMeters >= 25_000.0 || graph.isEmpty()) {
                mutex.withLock {
                    var loadedFromJson = false
                    if (context != null) {
                        val jsonFile = MapDownloadManager.getLocalMapFile(context)
                        val parsed = parseJsonFileToGraph(jsonFile)
                        if (parsed != null && parsed.first.isNotEmpty() && parsed.second.isNotEmpty()) {
                            graph.load(parsed.first, parsed.second)
                            loadedFromJson = true
                            Log.d(TAG, "Grafo vectorial cargado con éxito desde JSON local: ${parsed.first.size} nodos, ${parsed.second.size} aristas.")
                        }
                    }

                    if (!loadedFromJson) {
                        Log.e(TAG, "FALLO CRÍTICO: Archivo JSON local de grafo no encontrado o vacío. PROHIBIDO LÍNEA RECTA. Grafo vacío.")
                        graph.load(emptyList(), emptyList())
                    }

                    lastLoadedCenter = origin
                }
            }
        }
    }

    private fun parseJsonFileToGraph(file: File): Pair<List<GraphNode>, List<GraphEdge>>? {
        if (!file.exists() || file.length() == 0L) return null
        try {
            val response = FileReader(file).use { reader ->
                Gson().fromJson(reader, MapGraphResponseDto::class.java)
            }

            val nodesList = response?.nodes?.map {
                GraphNode(id = it.id, coordinate = RouteCoordinate(it.lat, it.lon))
            } ?: emptyList()

            val edgesList = response?.edges?.map { edgeDto ->
                val geometryCoords = edgeDto.geometry?.map { RouteCoordinate(it.lat, it.lon) } ?: emptyList()
                GraphEdge(
                    id = edgeDto.id,
                    fromId = edgeDto.fromNodeId,
                    toId = edgeDto.toNodeId,
                    distanceMeters = edgeDto.distanceMeters,
                    riskWeight = edgeDto.riskWeight ?: 0.0,
                    accessibilityPenalty = edgeDto.accessibilityPenalty ?: 0.05,
                    isBlocked = edgeDto.isBlocked ?: false,
                    bidirectional = edgeDto.bidirectional ?: true,
                    highwayType = edgeDto.highwayType ?: "residential",
                    geometry = geometryCoords
                )
            } ?: emptyList()

            if (nodesList.isNotEmpty() && edgesList.isNotEmpty()) {
                return Pair(nodesList, edgesList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local graph JSON: ${e.message}", e)
        }
        return null
    }

    suspend fun applyIncidents(incidents: List<IncidentEntity>): Boolean = withContext(Dispatchers.IO) {
        var anyChanged = false
        mutex.withLock {
            val activeIncidentIds = HashSet<String>()
            for (incident in incidents) {
                val status = parseIncidentStatus(incident.status)
                val type = IncidentType.fromApiValue(incident.type)
                val severity = IncidentSeverity.fromApiValue(incident.severity)

                val isVerified = status == IncidentStatus.VERIFIED
                val isCriticalOrHigh = severity == IncidentSeverity.CRITICA || severity == IncidentSeverity.ALTA
                val isBlockingType = type in RISK_INCIDENT_TYPES || type == IncidentType.RUTA_INACCESIBLE

                if (isBlockingType && (isVerified || isCriticalOrHigh)) {
                    activeIncidentIds += incident.localId
                    val targetEdges = resolveAffectedEdges(incident)
                    for (edge in targetEdges) {
                        val changed = if (type == IncidentType.RUTA_INACCESIBLE) {
                            graph.applyEdgeAccessibility(edge.id, penalty = 1.0, incidentLocalId = incident.localId)
                        } else {
                            graph.applyEdgeRisk(edge.id, riskWeight = 1.0, blocked = true, incidentLocalId = incident.localId)
                        }
                        if (changed) anyChanged = true
                    }
                }
            }
        }
        anyChanged
    }

    fun findMatchingEdge(
        point: RouteCoordinate,
        bearing: Float? = null,
        speedMps: Double? = null,
        currentEdgeId: String? = null
    ): GraphEdge? {
        return graph.nearestMatchingEdge(
            point = point,
            bearing = bearing,
            speedMps = speedMps,
            currentEdgeId = currentEdgeId,
            maxDistanceMeters = MAX_MATCH_DISTANCE_METERS
        )
    }

    private fun resolveAffectedEdges(incident: IncidentEntity): List<GraphEdge> {
        val explicitIds = parseAffectedSegmentIds(incident.affectedSegmentIds)
        if (explicitIds.isNotEmpty()) {
            return explicitIds.mapNotNull { graph.edgeById(it) }
        }
        val point = RouteCoordinate(incident.latitude, incident.longitude)
        return graph.edgesWithinRadius(point, radiusMeters = 20.0)
    }

    fun graphSnapshot(): RoadGraph = graph
}
