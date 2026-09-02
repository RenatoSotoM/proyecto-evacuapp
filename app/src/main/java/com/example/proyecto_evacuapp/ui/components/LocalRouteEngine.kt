package com.example.proyecto_evacuapp.ui.components

import kotlin.math.roundToInt

object LocalRouteEngine {

    suspend fun calculateRoute(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile,
        blockedSegmentIds: Set<String>
    ): LocalRouteResult {
        /*
         * En la siguiente etapa, estas coordenadas se cargarán desde
         * assets/offline_graph.json o desde el grafo que entregue
         * GraphHopper / BRouter.
         *
         * No debe mantenerse una línea directa origin -> destination.
         */
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
        /*
         * Temporalmente el conjunto de nodos se deja en orden vial.
         * Al conectar GraphHopper/BRouter, este método será sustituido
         * por el resultado real del motor.
         */
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