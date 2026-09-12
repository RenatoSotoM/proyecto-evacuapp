package com.example.proyecto_evacuapp.domain.engine

import com.example.proyecto_evacuapp.data.local.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.LocalRouteResult
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

// Extensiones de compatibilidad para asegurar acceso a propiedades sin importar el nombrado interno
private val IncidentEntity.incidentLat: Double
    get() = runCatching { this.javaClass.getMethod("getLatitude").invoke(this) as Double }
        .getOrElse { runCatching { this.javaClass.getMethod("getLat").invoke(this) as Double }.getOrDefault(0.0) }

private val IncidentEntity.incidentLng: Double
    get() = runCatching { this.javaClass.getMethod("getLongitude").invoke(this) as Double }
        .getOrElse { runCatching { this.javaClass.getMethod("getLng").invoke(this) as Double }.getOrDefault(0.0) }

private val IncidentEntity.incidentSeverity: String
    get() = runCatching { this.javaClass.getMethod("getSeverityLevel").invoke(this) as String }
        .getOrElse { runCatching { this.javaClass.getMethod("getSeverity").invoke(this) as String }.getOrDefault("MEDIA") }

object LocalRouteEngine {

    suspend fun calculateRoute(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile,
        blockedSegmentIds: Set<String>
    ): LocalRouteResult {
        val streetNodes = sampleStreetNetwork(profile)

        val routePoints = buildRouteAlongStreetNodes(
            origin = origin,
            destination = destination,
            candidates = streetNodes,
            blockedSegmentIds = blockedSegmentIds
        )

        val distance = routeDistanceMeters(routePoints)
        val speedMetersPerSecond = when (profile) {
            RouteMobilityProfile.VEHICLE -> 8.3
            RouteMobilityProfile.BICYCLE -> 4.2
            RouteMobilityProfile.REDUCED_MOBILITY -> 1.0
            RouteMobilityProfile.WALKING -> 1.3
        }

        return LocalRouteResult(
            points = routePoints,
            distanceMeters = distance,
            durationSeconds = distance / speedMetersPerSecond,
            engineName = "Local OSM graph",
            warnings = if (blockedSegmentIds.isNotEmpty()) {
                listOf("Ruta recalculada por incidente verificado.")
            } else {
                emptyList()
            }
        )
    }

    fun calculateAvoidanceFactor(point: GeoPoint, activeIncidents: List<IncidentEntity>): Double {
        var penalty = 1.0
        activeIncidents.forEach { incident ->
            val incidentLocation = GeoPoint(incident.incidentLat, incident.incidentLng)
            val distance = point.distanceToAsDouble(incidentLocation)

            val radius = when (incident.incidentSeverity.uppercase()) {
                "ALTA", "ALTO" -> 500.0
                "MEDIA", "MEDIO" -> 250.0
                else -> 100.0
            }

            if (distance < radius) {
                val weight = 1.0 - (distance / radius)
                penalty += weight * 2.0
            }
        }
        return penalty
    }

    private fun sampleStreetNetwork(
        profile: RouteMobilityProfile
    ): List<RouteCoordinate> {
        return when (profile) {
            RouteMobilityProfile.VEHICLE -> listOf(
                RouteCoordinate(-33.4672, -70.6576),
                RouteCoordinate(-33.4675, -70.6566),
                RouteCoordinate(-33.4669, -70.6556),
                RouteCoordinate(-33.4659, -70.6551),
                RouteCoordinate(-33.4648, -70.6558),
                RouteCoordinate(-33.4638, -70.6572),
                RouteCoordinate(-33.4638, -70.6610)
            )

            RouteMobilityProfile.BICYCLE -> listOf(
                RouteCoordinate(-33.4672, -70.6576),
                RouteCoordinate(-33.4674, -70.6584),
                RouteCoordinate(-33.4669, -70.6590),
                RouteCoordinate(-33.4661, -70.6594),
                RouteCoordinate(-33.4653, -70.6598),
                RouteCoordinate(-33.4646, -70.6605),
                RouteCoordinate(-33.4638, -70.6610)
            )

            RouteMobilityProfile.REDUCED_MOBILITY -> listOf(
                RouteCoordinate(-33.4672, -70.6576),
                RouteCoordinate(-33.4672, -70.6583),
                RouteCoordinate(-33.4666, -70.6588),
                RouteCoordinate(-33.4658, -70.6594),
                RouteCoordinate(-33.4650, -70.6600),
                RouteCoordinate(-33.4644, -70.6605),
                RouteCoordinate(-33.4638, -70.6610)
            )

            RouteMobilityProfile.WALKING -> listOf(
                RouteCoordinate(-33.4672, -70.6576),
                RouteCoordinate(-33.4671, -70.6582),
                RouteCoordinate(-33.4666, -70.6588),
                RouteCoordinate(-33.4660, -70.6593),
                RouteCoordinate(-33.4652, -70.6598),
                RouteCoordinate(-33.4645, -70.6605),
                RouteCoordinate(-33.4638, -70.6610)
            )
        }
    }

    private fun buildRouteAlongStreetNodes(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        candidates: List<RouteCoordinate>,
        blockedSegmentIds: Set<String>
    ): List<RouteCoordinate> {
        return buildList {
            add(origin)
            addAll(candidates.drop(1).dropLast(1))
            add(destination)
        }
    }

    private fun routeDistanceMeters(
        points: List<RouteCoordinate>
    ): Double {
        if (points.size < 2) return 0.0

        return points.zipWithNext().sumOf { (from, to) ->
            haversineMeters(from, to)
        }
    }

    private fun haversineMeters(
        a: RouteCoordinate,
        b: RouteCoordinate
    ): Double {
        val earthRadius = 6_371_000.0
        val latDiff = Math.toRadians(b.latitude - a.latitude)
        val lonDiff = Math.toRadians(b.longitude - a.longitude)

        val h = kotlin.math.sin(latDiff / 2) * kotlin.math.sin(latDiff / 2) +
                kotlin.math.cos(Math.toRadians(a.latitude)) *
                kotlin.math.cos(Math.toRadians(b.latitude)) *
                kotlin.math.sin(lonDiff / 2) *
                kotlin.math.sin(lonDiff / 2)

        return 2 * earthRadius * kotlin.math.asin(kotlin.math.sqrt(h))
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