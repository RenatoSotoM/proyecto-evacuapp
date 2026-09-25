package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.data.remote.MapGraphResponseDto
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.LocalRouteResult
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile
import com.example.proyecto_evacuapp.ui.components.RouteVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

private const val TAG = "LocalRouteEngine"

/**
 * Motor de ruteo local offline-first instrumentado con EVAC_DEBUG y carga segura de respaldo desde disco.
 */
object LocalRouteEngine {

    private var repository: RoadNetworkRepository? = null
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (repository != null) return
        repository = RoadNetworkRepository()
        Log.d(TAG, "LocalRouteEngine inicializado")
    }

    private fun requireRepository(): RoadNetworkRepository =
        repository ?: error(
            "LocalRouteEngine.initialize(context) debe llamarse antes de calcular rutas"
        )

    suspend fun syncIncidents(incidents: List<IncidentEntity>): Boolean {
        return requireRepository().applyIncidents(incidents)
    }

    suspend fun calculateRoute(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile,
        blockedSegmentIds: Set<String> = emptySet()
    ): LocalRouteResult {
        val alternatives = calculateRouteAlternatives(origin, destination, profile, blockedSegmentIds)
        return alternatives.firstOrNull { it.variant == RouteVariant.SEGURA }
            ?: alternatives.firstOrNull()
            ?: emptyRouteResult(origin, destination)
    }

    private fun loadGraphIfNeeded(context: Context) {
        val repo = requireRepository()
        if (repo.graphSnapshot().isEmpty()) {
            try {
                val file = java.io.File(context.filesDir, "local_graph_30km.json")
                if (file.exists() && file.length() > 0L) {
                    val jsonString = file.readText()
                    val parsedGraph = com.google.gson.Gson().fromJson(jsonString, MapGraphResponseDto::class.java)

                    val nodesList = parsedGraph?.nodes?.mapNotNull {
                        val id = it.id ?: return@mapNotNull null
                        val lat = it.lat ?: 0.0
                        val lon = it.lon ?: 0.0
                        GraphNode(id = id, coordinate = RouteCoordinate(lat, lon))
                    } ?: emptyList()

                    val edgesList = parsedGraph?.edges?.mapNotNull { edgeDto ->
                        val id = edgeDto.id ?: return@mapNotNull null
                        val fromId = edgeDto.fromNodeId ?: return@mapNotNull null
                        val toId = edgeDto.toNodeId ?: return@mapNotNull null
                        val dist = edgeDto.distanceMeters ?: 0.0
                        val geometryCoords = edgeDto.geometry?.mapNotNull { g ->
                            if (g.lat != null && g.lon != null) RouteCoordinate(g.lat, g.lon) else null
                        } ?: emptyList()

                        GraphEdge(
                            id = id,
                            fromId = fromId,
                            toId = toId,
                            distanceMeters = dist,
                            riskWeight = edgeDto.riskWeight ?: 0.0,
                            accessibilityPenalty = edgeDto.accessibilityPenalty ?: 0.05,
                            isBlocked = edgeDto.isBlocked ?: false,
                            bidirectional = edgeDto.bidirectional ?: true,
                            highwayType = edgeDto.highwayType ?: "residential",
                            geometry = geometryCoords
                        )
                    } ?: emptyList()

                    repo.graphSnapshot().load(nodesList, edgesList)
                    val graph = repo.graphSnapshot()
                    Log.d("EVAC_DEBUG", "Grafo local cargado: ${graph.nodes.size} nodos, ${graph.edges.size} aristas.")
                } else {
                    Log.e("EVAC_DEBUG", "El archivo local_graph_30km.json no existe en filesDir o está vacío.")
                }
            } catch (e: Exception) {
                Log.e("EVAC_DEBUG", "Error al cargar el grafo local: ${e.message}", e)
            }
        }
    }

    /**
     * Calcula las opciones de ruta instrumentadas con EVAC_DEBUG para diagnóstico paso a paso.
     */
    suspend fun calculateRouteAlternatives(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile,
        blockedSegmentIds: Set<String> = emptySet(),
        startBearing: Float? = null
    ): List<LocalRouteResult> = withContext(Dispatchers.IO) {
        appContext?.let { loadGraphIfNeeded(it) }

        val repo = requireRepository()
        repo.ensureLoadedForRoute(appContext, origin, destination)
        val graph = repo.graphSnapshot()

        if (graph.isEmpty()) {
            Log.d("EVAC_DEBUG", "LocalRouteEngine: [ABORT] Graph is empty! Road network was not loaded or OSM parsing failed.")
            return@withContext emptyList()
        }

        val startNode = graph.findNearestNode(origin.latitude, origin.longitude, maxRadiusMeters = 1000.0)
        val startDist = if (startNode != null) haversineMeters(origin, startNode.coordinate) else -1.0
        Log.d("EVAC_DEBUG", "LocalRouteEngine ORIGIN -> Coords: (${origin.latitude}, ${origin.longitude}) | NearestNode: ${startNode?.id ?: "NONE"} | Distance: ${startDist}m")

        val endNode = graph.findNearestNode(destination.latitude, destination.longitude, maxRadiusMeters = 1000.0)
        val endDist = if (endNode != null) haversineMeters(destination, endNode.coordinate) else -1.0
        Log.d("EVAC_DEBUG", "LocalRouteEngine DESTINATION -> Coords: (${destination.latitude}, ${destination.longitude}) | NearestNode: ${endNode?.id ?: "NONE"} | Distance: ${endDist}m")

        if (startNode == null || endNode == null) {
            Log.d("EVAC_DEBUG", "LocalRouteEngine: [ABORT] Origin node (${startNode?.id}) or Destination node (${endNode?.id}) not found within range.")
            return@withContext emptyList()
        }

        val startNodeId = startNode.id ?: return@withContext emptyList()
        val endNodeId = endNode.id ?: return@withContext emptyList()

        Log.d("EVAC_DEBUG", "LocalRouteEngine DIJKSTRA START -> startNodeId: $startNodeId, endNodeId: $endNodeId")

        val sessionPenalties = blockedSegmentIds.associateWith { HARD_BLOCK_COST }

        // 1. RUTA SEGURA
        val segura = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.SAFE_WEIGHTS,
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = sessionPenalties,
            startBearing = startBearing
        )
        Log.d("EVAC_DEBUG", "LocalRouteEngine DIJKSTRA RESULT -> Segura path found: ${segura != null}, edgeCount: ${segura?.edgeIds?.size ?: 0}, distance: ${segura?.distanceMeters ?: 0.0}m")

        // 2. RUTA ALTERNATIVA 1
        val seguraEdgePenalties = sessionPenalties + (segura?.edgeIds?.associateWith { 10_000.0 } ?: emptyMap())
        var alt1 = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.weightsFor(profile),
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = seguraEdgePenalties,
            startBearing = startBearing
        )
        if (alt1 == null || (segura != null && alt1.edgeIds == segura.edgeIds)) {
            alt1 = segura
        }

        // 3. RUTA ALTERNATIVA 2
        val alt2Penalties = sessionPenalties +
                (segura?.edgeIds?.associateWith { 10_000.0 } ?: emptyMap()) +
                (alt1?.edgeIds?.associateWith { 10_000.0 } ?: emptyMap())
        var alt2 = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.weightsFor(profile),
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = alt2Penalties,
            startBearing = startBearing
        )
        if (alt2 == null || (segura != null && alt2.edgeIds == segura.edgeIds)) {
            alt2 = alt1 ?: segura
        }

        // 4. RUTA OFFLINE (LOCAL)
        val offline = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.ACCESSIBLE_WEIGHTS,
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = sessionPenalties,
            startBearing = startBearing
        ) ?: alt2 ?: alt1 ?: segura

        val results = mutableListOf<LocalRouteResult>()

        segura?.let {
            results += it.toRouteResult(
                variant = RouteVariant.SEGURA,
                label = "Ruta Segura",
                sessionBlocks = blockedSegmentIds
            )
        }

        alt1?.let {
            results += it.toRouteResult(
                variant = RouteVariant.ALTERNATIVA_1,
                label = "Ruta Alternativa 1",
                sessionBlocks = blockedSegmentIds
            )
        }

        alt2?.let {
            results += it.toRouteResult(
                variant = RouteVariant.ALTERNATIVA_2,
                label = "Ruta Alternativa 2",
                sessionBlocks = blockedSegmentIds
            )
        }

        offline?.let {
            results += it.toRouteResult(
                variant = RouteVariant.OFFLINE,
                label = "Ruta Offline (Local)",
                sessionBlocks = blockedSegmentIds
            )
        }

        val distinctResults = results.distinctBy { it.variant }
        val count = distinctResults.size

        if (count == 0) {
            Log.d("EVAC_DEBUG", "LocalRouteEngine: [ABORT] Graph is disconnected or Dijkstra returned no valid paths between origin ($startNodeId) and destination ($endNodeId).")
            val isolatedResult = emptyRouteResult(origin, destination).copy(
                statusMessage = "⚠️ Sin acceso: No existen rutas posibles hacia el destino debido a bloqueos totales."
            )
            return@withContext listOf(isolatedResult)
        }

        val statusMessage = if (count < 4) {
            "⚠️ Se encontraron $count opciones de ruta disponibles en esta zona."
        } else {
            null
        }

        distinctResults.map { it.copy(statusMessage = statusMessage) }
    }

    private fun PathResult.toRouteResult(
        variant: RouteVariant,
        label: String,
        sessionBlocks: Set<String>
    ): LocalRouteResult {
        val warnings = if (maxRiskOnPath >= RISK_VERIFIED_THRESHOLD || sessionBlocks.isNotEmpty()) {
            listOf("Ruta recalculada por incidente verificado.")
        } else {
            emptyList()
        }
        return LocalRouteResult(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            engineName = "Local road graph (offline OSM 30km)",
            warnings = warnings,
            variant = variant,
            label = label,
            avoidsVerifiedRisk = maxRiskOnPath < RISK_VERIFIED_THRESHOLD,
            maxAccessibilityPenaltyOnPath = maxAccessibilityPenaltyOnPath
        )
    }

    private fun emptyRouteResult(origin: RouteCoordinate, destination: RouteCoordinate) = LocalRouteResult(
        points = listOf(origin, destination),
        distanceMeters = 0.0,
        durationSeconds = 0.0,
        engineName = "Local road graph (offline OSM 30km)",
        warnings = listOf("No fue posible calcular una ruta sobre el grafo local."),
        variant = RouteVariant.SEGURA,
        label = "Ruta no disponible"
    )

    fun calculateAvoidanceFactor(point: GeoPoint, activeIncidents: List<IncidentEntity>): Double {
        var penalty = 1.0
        activeIncidents.forEach { incident ->
            val incidentLocation = GeoPoint(incident.latitude, incident.longitude)
            val distance = point.distanceToAsDouble(incidentLocation)

            val severity = IncidentSeverity.fromApiValue(incident.severity)
            val radius = when (severity) {
                IncidentSeverity.CRITICA, IncidentSeverity.ALTA -> 500.0
                IncidentSeverity.MEDIA -> 250.0
                IncidentSeverity.BAJA -> 100.0
            }

            if (distance < radius) {
                val weight = 1.0 - (distance / radius)
                penalty += weight * 2.0
            }
        }
        return penalty
    }

    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters >= 1_000) {
            "${"%.1f".format(distanceMeters / 1_000)} km"
        } else {
            "${distanceMeters.roundToInt()} m"
        }
    }

    fun formatDuration(durationSeconds: Double): String {
        val minutes = (durationSeconds / 60).roundToInt().coerceAtLeast(1)
        return "$minutes min"
    }
}
