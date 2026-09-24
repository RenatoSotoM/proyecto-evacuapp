package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.LocalRouteResult
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile
import com.example.proyecto_evacuapp.ui.components.RouteVariant
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

private const val TAG = "LocalRouteEngine"

/**
 * Motor de ruteo local offline-first.
 * Aplica restricciones duras (Hard Constraints) de bloqueo para todas las variantes de ruta
 * y garantiza la generación de las 4 opciones seleccionables (Ruta Segura, Ruta Alternativa 1, Ruta Alternativa 2, Ruta Offline).
 */
object LocalRouteEngine {

    private var repository: RoadNetworkRepository? = null

    fun initialize(context: Context) {
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

    /**
     * Calcula las 4 opciones obligatorias dentro del radio de búsqueda:
     * 1. Ruta Segura (SEGURA): Dijkstra evitando 100% riesgos y bloqueos (Restricción Dura HARD_BLOCK_COST).
     * 2. Ruta Alternativa 1 (ALTERNATIVA_1): Aplica penalización de +10000.0m a las aristas de Ruta Segura.
     * 3. Ruta Alternativa 2 (ALTERNATIVA_2): Aplica penalización a las aristas de Ruta Segura y Ruta Alternativa 1.
     * 4. Ruta Offline (OFFLINE): Calculada puramente usando el grafo local en Room DB / memoria.
     */
    suspend fun calculateRouteAlternatives(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile,
        blockedSegmentIds: Set<String> = emptySet(),
        startBearing: Float? = null
    ): List<LocalRouteResult> {
        val repo = requireRepository()
        repo.ensureLoadedForRoute(origin, destination)
        val graph = repo.graphSnapshot()

        if (graph.isEmpty()) {
            Log.w(TAG, "Grafo vial vacío, no es posible calcular rutas")
            return emptyList()
        }

        val startNode = graph.nearestNode(origin)
        val endNode = graph.nearestNode(destination)
        if (startNode == null || endNode == null) {
            Log.w(TAG, "No se encontraron nodos cercanos al origen/destino")
            return emptyList()
        }

        val sessionPenalties = blockedSegmentIds.associateWith { HARD_BLOCK_COST }

        // 1. RUTA 1: RUTA SEGURA (100% libre de riesgos y bloqueos con restricción dura C(e) = infinity)
        val segura = graph.shortestPath(
            startNodeId = startNode.id,
            endNodeId = endNode.id,
            weights = CostProfiles.SAFE_WEIGHTS,
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = sessionPenalties,
            startBearing = startBearing
        )

        // 2. RUTA 2: RUTA ALTERNATIVA 1 (Penaliza +10000.0m las aristas de Ruta Segura)
        val seguraEdgePenalties = sessionPenalties + (segura?.edgeIds?.associateWith { 10_000.0 } ?: emptyMap())
        var alt1 = graph.shortestPath(
            startNodeId = startNode.id,
            endNodeId = endNode.id,
            weights = CostProfiles.weightsFor(profile),
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = seguraEdgePenalties,
            startBearing = startBearing
        )
        if (alt1 == null || (segura != null && alt1.edgeIds == segura.edgeIds)) {
            alt1 = segura
        }

        // 3. RUTA 3: RUTA ALTERNATIVA 2 (Penaliza aristas de Ruta Segura y Ruta Alternativa 1)
        val alt2Penalties = sessionPenalties +
                (segura?.edgeIds?.associateWith { 10_000.0 } ?: emptyMap()) +
                (alt1?.edgeIds?.associateWith { 10_000.0 } ?: emptyMap())
        var alt2 = graph.shortestPath(
            startNodeId = startNode.id,
            endNodeId = endNode.id,
            weights = CostProfiles.weightsFor(profile),
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = alt2Penalties,
            startBearing = startBearing
        )
        if (alt2 == null || (segura != null && alt2.edgeIds == segura.edgeIds)) {
            alt2 = alt1 ?: segura
        }

        // 4. RUTA 4: RUTA OFFLINE (LOCAL) (Generada estrictamente usando el grafo local Room DB / memoria)
        val offline = graph.shortestPath(
            startNodeId = startNode.id,
            endNodeId = endNode.id,
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
            val isolatedResult = emptyRouteResult(origin, destination).copy(
                statusMessage = "⚠️ Sin acceso: No existen rutas posibles hacia el destino debido a bloqueos totales."
            )
            return listOf(isolatedResult)
        }

        val statusMessage = if (count < 4) {
            "⚠️ Se encontraron $count opciones de ruta disponibles en esta zona."
        } else {
            null
        }

        return distinctResults.map { it.copy(statusMessage = statusMessage) }
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
            engineName = "Local road graph (offline)",
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
        engineName = "Local road graph (offline)",
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
