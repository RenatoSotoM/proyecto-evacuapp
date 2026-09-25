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
 * Valida robustamente esquemas DTO con anotaciones @SerializedName y fallbacks.
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
                            Log.d("EVAC_DEBUG", "Grafo local cargado: ${parsed.first.size} nodos, ${parsed.second.size} aristas.")
                        }
                    }

                    if (!loadedFromJson) {
                        Log.e(TAG, "FALLO CRÍTICO: Archivo JSON local de grafo no encontrado o vacío. Grafo vacío.")
                        graph.load(emptyList(), emptyList())
                    }

                    lastLoadedCenter = origin
                }
            }
        }
    }

    private fun parseJsonFileToGraph(file: File): Pair<List<GraphNode>, List<GraphEdge>>? {
        if (!file.exists() || file.length() == 0L) {
            Log.d("EVAC_DEBUG", "parseJsonFileToGraph: File does not exist or size is 0.")
            return null
        }
        try {
            val response = FileReader(file).use { reader ->
                Gson().fromJson(reader, MapGraphResponseDto::class.java)
            }

            Log.d("EVAC_DEBUG", "parseJsonFileToGraph raw parsed DTO -> nodes count: ${response?.nodes?.size ?: 0}, edges count: ${response?.edges?.size ?: 0}")

            val nodesList = response?.nodes?.mapNotNull {
                val id = it.id ?: return@mapNotNull null
                val lat = it.lat ?: 0.0
                val lon = it.lon ?: 0.0
                GraphNode(id = id, coordinate = RouteCoordinate(lat, lon))
            } ?: emptyList()

            val edgesList = response?.edges?.mapNotNull { edgeDto ->
                val id = edgeDto.id ?: "edge_${System.nanoTime()}_${Math.random()}"
                val fromId = edgeDto.fromNodeId
                val toId = edgeDto.toNodeId
                if (fromId == null || toId == null) {
                    Log.d("EVAC_DEBUG", "Edge dropped due to null endpoint: id=$id, from=$fromId, to=$toId")
                    return@mapNotNull null
                }
                val dist = edgeDto.distanceMeters ?: 10.0
                val isOneway = edgeDto.oneway ?: false
                val bidirectional = (edgeDto.bidirectional ?: true) && !isOneway
                val geometryCoords = edgeDto.geometry?.mapNotNull { g ->
                    val glat = g.lat
                    val glon = g.lon
                    if (glat != null && glon != null) RouteCoordinate(glat, glon) else null
                } ?: emptyList()

                GraphEdge(
                    id = id,
                    fromId = fromId,
                    toId = toId,
                    distanceMeters = dist,
                    riskWeight = edgeDto.riskWeight ?: 0.0,
                    accessibilityPenalty = edgeDto.accessibilityPenalty ?: 0.05,
                    isBlocked = edgeDto.isBlocked ?: false,
                    bidirectional = bidirectional,
                    highwayType = edgeDto.highwayType ?: "residential",
                    geometry = geometryCoords
                )
            } ?: emptyList()

            Log.d("EVAC_DEBUG", "Grafo local cargado: ${nodesList.size} nodos, ${edgesList.size} aristas.")

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
                        val edgeId = edge.id ?: continue
                        val changed = if (type == IncidentType.RUTA_INACCESIBLE) {
                            graph.applyEdgeAccessibility(edgeId, penalty = 1.0, incidentLocalId = incident.localId)
                        } else {
                            graph.applyEdgeRisk(edgeId, riskWeight = 1.0, blocked = true, incidentLocalId = incident.localId)
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
