package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
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
 * Motor de ruteo local offline-first[cite: 5].
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
        return alternatives.firstOrNull { it.variant == RouteVariant.PRINCIPAL }
            ?: alternatives.firstOrNull()
            ?: emptyRouteResult(origin, destination)
    }

    suspend fun calculateRouteAlternatives(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile,
        blockedSegmentIds: Set<String> = emptySet()
    ): List<LocalRouteResult> {
        val repo = requireRepository()
        // Llamada correcta al repositorio dinámico con origen y destino
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
        val avoidInaccessibleHard = profile == RouteMobilityProfile.REDUCED_MOBILITY

        val principalWeights = CostProfiles.weightsFor(profile)
        val principal = graph.shortestPath(
            startNodeId = startNode.id,
            endNodeId = endNode.id,
            weights = principalWeights,
            profile = profile,
            edgePenalties = sessionPenalties
        )

        var segura = graph.shortestPath(
            startNodeId = startNode.id,
            endNodeId = endNode.id,
            weights = CostProfiles.SAFE_WEIGHTS,
            profile = profile,
            avoidVerifiedRisk = true,
            edgePenalties = sessionPenalties
        )
        if (segura != null && principal != null && segura.edgeIds == principal.edgeIds) {
            val diversityPenalties = sessionPenalties + principal.edgeIds.associateWith { 5_000.0 }
            segura = graph.shortestPath(
                startNodeId = startNode.id,
                endNodeId = endNode.id,
                weights = CostProfiles.SAFE_WEIGHTS,
                profile = profile,
                avoidVerifiedRisk = true,
                edgePenalties = diversityPenalties
            ) ?: segura
        }

        var accesible = graph.shortestPath(
            startNodeId = startNode.id,
            endNodeId = endNode.id,
            weights = CostProfiles.ACCESSIBLE_WEIGHTS,
            profile = profile,
            avoidInaccessible = avoidInaccessibleHard,
            edgePenalties = sessionPenalties
        )
        if (accesible != null && principal != null && accesible.edgeIds == principal.edgeIds) {
            val diversityPenalties = sessionPenalties + principal.edgeIds.associateWith { 5_000.0 }
            accesible = graph.shortestPath(
                startNodeId = startNode.id,
                endNodeId = endNode.id,
                weights = CostProfiles.ACCESSIBLE_WEIGHTS,
                profile = profile,
                avoidInaccessible = avoidInaccessibleHard,
                edgePenalties = diversityPenalties
            ) ?: accesible
        }

        val results = mutableListOf<LocalRouteResult>()
        principal?.let { results += it.toRouteResult(RouteVariant.PRINCIPAL, "Ruta Rápida", blockedSegmentIds) }
        segura?.let { results += it.toRouteResult(RouteVariant.SEGURA, "Ruta Evitando Riesgo", blockedSegmentIds) }
        accesible?.let { results += it.toRouteResult(RouteVariant.ACCESIBLE, "Ruta Accesible", blockedSegmentIds) }

        return results.distinctBy { it.points }
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
        variant = RouteVariant.PRINCIPAL,
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