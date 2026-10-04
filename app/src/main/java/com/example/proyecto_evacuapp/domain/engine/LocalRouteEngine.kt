package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.LocalRouteResult
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile
import com.example.proyecto_evacuapp.ui.components.RouteVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

private const val TAG = "LocalRouteEngine"

/**
 * Motor de ruteo local optimizado con BBox bajo demanda, buffer anti-invasión de 20m
 * y penalización estricta de aristas para garantizar rutas alternativas reales (calles paralelas).
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

    fun snapToNearestEdge(latitude: Double, longitude: Double, maxDistanceMeters: Double = 25.0): String? {
        val repo = repository ?: return null
        return runBlocking(Dispatchers.IO) {
            repo.snapToNearestEdge(appContext, latitude, longitude, maxDistanceMeters)
        }
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

    /**
     * Calcula alternativas con BBox dinámico, bloqueando instantáneamente cualquier incidente activo
     * mediante el buffer anti-invasión de 20m y forzando rutas alternativas reales mediante penalización.
     */
    suspend fun calculateRouteAlternatives(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile = RouteMobilityProfile.VEHICLE,
        blockedSegmentIds: Set<String> = emptySet(),
        startBearing: Float? = null
    ): List<LocalRouteResult> = withContext(Dispatchers.Default) {
        val repo = requireRepository()
        repo.ensureLoadedForRoute(appContext, origin, destination)

        // 1. Intento inicial con BBox estándar (margin = 0.03 ~ 3km) con medición de tiempo
        val loadStartMs = System.currentTimeMillis()
        var graph = repo.loadTransientGraphForRoute(appContext, origin, destination, margin = 0.03)
        var usedExpandedBBox = false

        if (graph == null || graph.isEmpty()) {
            Log.d("EVAC_DEBUG", "LocalRouteEngine: BBox estándar vacío. Ampliando BBox un 50% (margin = 0.045)...")
            graph = repo.loadTransientGraphForRoute(appContext, origin, destination, margin = 0.045)
            usedExpandedBBox = true
        }

        val loadMs = System.currentTimeMillis() - loadStartMs

        if (graph == null || graph.isEmpty()) {
            Log.d("EVAC_DEBUG", "LocalRouteEngine: BBox transitorio sin nodos ni aristas.")
            return@withContext listOf(
                emptyRouteResult(origin, destination).copy(
                    statusMessage = "⚠️ El destino seleccionado está fuera de la cartografía offline disponible. Por favor, selecciona un punto dentro del área respaldada."
                )
            )
        }

        val nodesCount = graph.nodes.size
        val edgesCount = graph.edges.size

        // 2. Medición de Inyección de Bloqueos, Buffer Anti-Invasión (20m) y Dijkstra K-Shortest Paths
        val calcStartMs = System.currentTimeMillis()

        // 2. Inyección Inmediata de Bloqueos (Cualquier incidente activo bloquea la vía y activa el buffer de 20m)
        val activeIncidents = IncidentSharedState.incidents
        var hasActiveBlocks = false
        for (incident in activeIncidents) {
            val isActive = incident.status != IncidentStatus.REJECTED && incident.status != IncidentStatus.SYNC_FAILED
            if (isActive) {
                hasActiveBlocks = true
                val reportCoord = RouteCoordinate(incident.latitude, incident.longitude)
                // Buffer anti-invasión de 20 metros alrededor del reporte
                val bufferEdges = graph.edgesWithinRadius(reportCoord, radiusMeters = 20.0)
                for (edge in bufferEdges) {
                    edge.isBlocked = true
                    edge.weight = Double.POSITIVE_INFINITY
                    Log.d("EVAC_DEBUG", "Arista bloqueada por buffer anti-invasión (20m): ${edge.id}")
                }
                incident.affectedEdgeId?.let { edgeId ->
                    graph.edges[edgeId]?.let { edge ->
                        edge.isBlocked = true
                        edge.weight = Double.POSITIVE_INFINITY
                        Log.d("EVAC_DEBUG", "Arista afectada directa bloqueada: ${edge.id}")
                    }
                }
            }
        }

        val startNode = graph.findNearestNode(origin.latitude, origin.longitude, maxRadiusMeters = 3000.0)
        val endNode = graph.findNearestNode(destination.latitude, destination.longitude, maxRadiusMeters = 3000.0)

        val startDist = if (startNode != null) haversineMeters(origin, startNode.coordinate) else Double.MAX_VALUE
        val endDist = if (endNode != null) haversineMeters(destination, endNode.coordinate) else Double.MAX_VALUE

        if (startNode == null || startDist > 5000.0) {
            Log.w("EVAC_DEBUG", "LocalRouteEngine: Origen fuera de cobertura ($startDist m)")
            return@withContext listOf(
                emptyRouteResult(origin, destination).copy(
                    statusMessage = "⚠️ Tu ubicación actual está fuera de la cartografía offline disponible. Por favor, acércate al área respaldada."
                )
            )
        }

        if (endNode == null || endDist > 5000.0) {
            Log.w("EVAC_DEBUG", "LocalRouteEngine: Destino fuera de cobertura ($endDist m)")
            return@withContext listOf(
                emptyRouteResult(origin, destination).copy(
                    statusMessage = "⚠️ El destino seleccionado está fuera de la cartografía offline disponible. Por favor, selecciona un punto dentro del área respaldada."
                )
            )
        }

        val startNodeId = startNode.id ?: return@withContext emptyList()
        val endNodeId = endNode.id ?: return@withContext emptyList()

        val sessionPenalties = blockedSegmentIds.associateWith { HARD_BLOCK_COST }

        // --- ALGORITMO K-SHORTEST PATHS CON PENALIZACIÓN ESTRICTA DE CORREDORES ---

        // 1. Cálculo de Ruta Principal / Segura (P1)
        val segura = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.SAFE_WEIGHTS,
            profile = profile,
            avoidVerifiedRisk = true,
            avoidInaccessible = false,
            edgePenalties = sessionPenalties,
            startBearing = startBearing
        )

        // Si la ruta segura falla con BBox estándar, intentamos con BBox un 50% más amplio automáticamente
        if (segura == null && !usedExpandedBBox) {
            Log.d("EVAC_DEBUG", "Ruta segura no encontrada con BBox estándar. Reintentando con BBox ampliado al 50%...")
            val expandedGraph = repo.loadTransientGraphForRoute(appContext, origin, destination, margin = 0.045)
            if (expandedGraph != null && !expandedGraph.isEmpty()) {
                for (incident in activeIncidents) {
                    if (incident.status != IncidentStatus.REJECTED && incident.status != IncidentStatus.SYNC_FAILED) {
                        val reportCoord = RouteCoordinate(incident.latitude, incident.longitude)
                        expandedGraph.edgesWithinRadius(reportCoord, radiusMeters = 20.0).forEach {
                            it.isBlocked = true
                            it.weight = Double.POSITIVE_INFINITY
                        }
                    }
                }
                val expStart = expandedGraph.findNearestNode(origin.latitude, origin.longitude, 1000.0)
                val expEnd = expandedGraph.findNearestNode(destination.latitude, destination.longitude, 1000.0)
                if (expStart != null && expEnd != null) {
                    val expSegura = expandedGraph.shortestPath(
                        startNodeId = expStart.id!!,
                        endNodeId = expEnd.id!!,
                        weights = CostProfiles.SAFE_WEIGHTS,
                        profile = profile,
                        avoidVerifiedRisk = true,
                        avoidInaccessible = false,
                        edgePenalties = sessionPenalties,
                        startBearing = startBearing
                    )
                    if (expSegura != null) {
                        return@withContext listOf(
                            expSegura.toRouteResult(
                                variant = RouteVariant.SEGURA,
                                label = "Ruta Segura",
                                sessionBlocks = blockedSegmentIds,
                                tacticalMsg = "Buscando vía segura fuera de la zona de riesgo..."
                            )
                        )
                    }
                }
            }
        }

        val primaryEdgeIds = segura?.edgeIds ?: emptyList()

        // 2. Cálculo de Ruta Alternativa 1 (P2): Forzar desvío total marcando aristas de P1 con Double.POSITIVE_INFINITY
        val originalWeightsP1 = mutableMapOf<String, Double>()
        for (edgeId in primaryEdgeIds) {
            graph.edges[edgeId]?.let { edge ->
                originalWeightsP1[edgeId] = edge.weight
                edge.weight = Double.POSITIVE_INFINITY // Desvío total de las calles de la ruta principal
            }
        }

        var alt1 = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.weightsFor(profile),
            profile = profile,
            avoidVerifiedRisk = true,
            avoidInaccessible = false,
            edgePenalties = sessionPenalties,
            startBearing = startBearing
        )

        // Restaurar pesos originales de P1
        for ((edgeId, origWeight) in originalWeightsP1) {
            graph.edges[edgeId]?.let { edge ->
                edge.weight = origWeight
            }
        }

        // Si alt1 es nula o idéntica a segura, usar penalización pesada de 1,000,000 en edgePenalties
        if (alt1 == null || (segura != null && alt1.edgeIds == segura.edgeIds)) {
            val heavyPenalties = sessionPenalties + primaryEdgeIds.associateWith { 1_000_000.0 }
            alt1 = graph.shortestPath(
                startNodeId = startNodeId,
                endNodeId = endNodeId,
                weights = CostProfiles.ACCESSIBLE_WEIGHTS,
                profile = profile,
                avoidVerifiedRisk = true,
                avoidInaccessible = false,
                edgePenalties = heavyPenalties,
                startBearing = startBearing
            )
            if (alt1 == null || (segura != null && alt1.edgeIds == segura.edgeIds)) {
                alt1 = segura
            }
        }

        // 3. Cálculo de Ruta Alternativa 2 (P3): Penalización combinada de P1 y P2
        val alt1EdgeIds = alt1?.edgeIds ?: emptyList()
        val combinedEdgeIds = (primaryEdgeIds + alt1EdgeIds).distinct()
        val heavyPenaltiesP3 = sessionPenalties + combinedEdgeIds.associateWith { 1_000_000.0 }
        var alt2 = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.weightsFor(profile),
            profile = profile,
            avoidVerifiedRisk = true,
            avoidInaccessible = false,
            edgePenalties = heavyPenaltiesP3,
            startBearing = startBearing
        )

        if (alt2 == null || (segura != null && alt2.edgeIds == segura.edgeIds) || (alt1 != null && alt2.edgeIds == alt1.edgeIds)) {
            alt2 = alt1 ?: segura
        }

        // 4. Ruta Offline / Accesible
        val offline = graph.shortestPath(
            startNodeId = startNodeId,
            endNodeId = endNodeId,
            weights = CostProfiles.ACCESSIBLE_WEIGHTS,
            profile = profile,
            avoidVerifiedRisk = true,
            avoidInaccessible = false,
            edgePenalties = sessionPenalties,
            startBearing = startBearing
        ) ?: alt2 ?: alt1 ?: segura

        val results = mutableListOf<LocalRouteResult>()
        val tacticalMsg = when {
            hasActiveBlocks -> "⚠️ Calle bloqueada, recalculando ruta alternativa..."
            else -> null
        }

        segura?.let {
            results += it.toRouteResult(RouteVariant.SEGURA, "Ruta Segura", blockedSegmentIds, tacticalMsg)
        }
        alt1?.let {
            results += it.toRouteResult(RouteVariant.ALTERNATIVA_1, "Ruta Alternativa 1", blockedSegmentIds, tacticalMsg)
        }
        alt2?.let {
            results += it.toRouteResult(RouteVariant.ALTERNATIVA_2, "Ruta Alternativa 2", blockedSegmentIds, tacticalMsg)
        }
        offline?.let {
            results += it.toRouteResult(RouteVariant.OFFLINE, "Ruta Local", blockedSegmentIds, tacticalMsg)
        }

        val distinctResults = results.distinctBy { it.variant }

        val calcMs = System.currentTimeMillis() - calcStartMs
        val runtime = Runtime.getRuntime()
        val heapUsedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val payloadSizeKB = String.format(java.util.Locale.US, "%.1f", (nodesCount * 64 + edgesCount * 128) / 1024.0)

        for (res in distinctResults) {
            val distMeters = res.distanceMeters.toInt()
            val turns = res.points.size
            Log.d(
                "EVAC_METRIC",
                "[ROUTING] Mode: ${profile.name} | RouteName: ${res.label} | NetworkState: OFFLINE_ROOM_DB | Nodes: $nodesCount | Edges: $edgesCount | BBoxLoadTime: ${loadMs}ms | RouteCalcTime: ${calcMs}ms | HeapMemoryUsedMB: ${heapUsedMB}MB | PayloadSizeKB: ${payloadSizeKB}KB | RouteDistanceMeters: ${distMeters}m | TurnCount: $turns"
            )
        }

        if (distinctResults.isEmpty()) {
            return@withContext listOf(
                emptyRouteResult(origin, destination).copy(
                    statusMessage = "⚠️ Se sugiere giro en U o búsqueda de vía secundaria fuera del área de peligro."
                )
            )
        }

        distinctResults
    }

    private fun PathResult.toRouteResult(
        variant: RouteVariant,
        label: String,
        sessionBlocks: Set<String>,
        tacticalMsg: String?
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
            engineName = "Local road graph (offline OSM BBox)",
            warnings = warnings,
            variant = variant,
            label = label,
            avoidsVerifiedRisk = maxRiskOnPath < RISK_VERIFIED_THRESHOLD,
            maxAccessibilityPenaltyOnPath = maxAccessibilityPenaltyOnPath,
            statusMessage = tacticalMsg
        )
    }

    private fun emptyRouteResult(origin: RouteCoordinate, destination: RouteCoordinate) = LocalRouteResult(
        points = listOf(origin, destination),
        distanceMeters = 0.0,
        durationSeconds = 0.0,
        engineName = "Local road graph (offline OSM BBox)",
        warnings = listOf("No fue posible calcular una ruta sobre el grafo local."),
        variant = RouteVariant.SEGURA,
        label = "Ruta no disponible",
        statusMessage = "⚠️ Se sugiere giro en U o búsqueda de vía secundaria fuera del área de peligro."
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
