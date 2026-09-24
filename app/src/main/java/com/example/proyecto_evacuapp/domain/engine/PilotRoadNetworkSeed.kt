package com.example.proyecto_evacuapp.domain.engine

import com.example.proyecto_evacuapp.ui.components.RoadEdgeEntity
import com.example.proyecto_evacuapp.ui.components.RoadNodeEntity
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate

data class RoadNetworkSeed(
    val nodes: List<RoadNodeEntity>,
    val edges: List<RoadEdgeEntity>
)

/**
 * Generador dinámico de red vial ortogonal (manzanas urbanas).
 * Construye tramos calle por calle con esquinas a 90° (Eje Norte-Sur y Eje Este-Oeste),
 * evitando líneas rectas diagonales que atraviesan manzanas.
 */
object PilotRoadNetworkSeed {

    private const val HUB_ORIGIN = "hub_origin"
    private const val HUB_DESTINATION = "hub_destination"

    fun buildDynamic(origin: RouteCoordinate, destination: RouteCoordinate): RoadNetworkSeed {
        val nodes = mutableListOf<RoadNodeEntity>()
        val edges = mutableListOf<RoadEdgeEntity>()

        nodes += RoadNodeEntity(HUB_ORIGIN, origin.latitude, origin.longitude)
        nodes += RoadNodeEntity(HUB_DESTINATION, destination.latitude, destination.longitude)

        val midLat = (origin.latitude + destination.latitude) / 2.0

        // Cadena 1: Av. Norte-Sur primero, luego Calle Este-Oeste
        addGridChain(
            nodes, edges, prefix = "grid_ns_ew",
            waypoints = listOf(
                origin,
                RouteCoordinate(destination.latitude, origin.longitude),
                destination
            ),
            accessibilityPenalty = 0.10
        )

        // Cadena 2: Calle Este-Oeste primero, luego Av. Norte-Sur
        addGridChain(
            nodes, edges, prefix = "grid_ew_ns",
            waypoints = listOf(
                origin,
                RouteCoordinate(origin.latitude, destination.longitude),
                destination
            ),
            accessibilityPenalty = 0.20
        )

        // Cadena 3: Zig-Zag por avenida intermedia
        addGridChain(
            nodes, edges, prefix = "grid_zigzag",
            waypoints = listOf(
                origin,
                RouteCoordinate(midLat, origin.longitude),
                RouteCoordinate(midLat, destination.longitude),
                destination
            ),
            accessibilityPenalty = 0.05
        )

        // Cadena 4: Rodeo exterior por manzana paralela
        val latOffset = if (destination.latitude >= origin.latitude) 0.0012 else -0.0012
        addGridChain(
            nodes, edges, prefix = "grid_bypass",
            waypoints = listOf(
                origin,
                RouteCoordinate(origin.latitude + latOffset, origin.longitude),
                RouteCoordinate(origin.latitude + latOffset, destination.longitude),
                destination
            ),
            accessibilityPenalty = 0.15
        )

        return RoadNetworkSeed(nodes, edges)
    }

    private fun addGridChain(
        nodes: MutableList<RoadNodeEntity>,
        edges: MutableList<RoadEdgeEntity>,
        prefix: String,
        waypoints: List<RouteCoordinate>,
        accessibilityPenalty: Double
    ) {
        val fullNodeIds = mutableListOf<String>()
        fullNodeIds += HUB_ORIGIN

        val detailedCoords = mutableListOf<RouteCoordinate>()
        detailedCoords += waypoints.first()

        for (w in 0 until waypoints.size - 1) {
            val start = waypoints[w]
            val end = waypoints[w + 1]
            val subSteps = 3

            for (s in 1..subSteps) {
                val fraction = s.toDouble() / subSteps
                val lat = start.latitude + (end.latitude - start.latitude) * fraction
                val lon = start.longitude + (end.longitude - start.longitude) * fraction

                if (w == waypoints.size - 2 && s == subSteps) {
                    fullNodeIds += HUB_DESTINATION
                    detailedCoords += end
                } else {
                    val nodeId = "${prefix}_node_${w}_$s"
                    nodes += RoadNodeEntity(nodeId, lat, lon)
                    fullNodeIds += nodeId
                    detailedCoords += RouteCoordinate(lat, lon)
                }
            }
        }

        for (i in 0 until fullNodeIds.size - 1) {
            val fromId = fullNodeIds[i]
            val toId = fullNodeIds[i + 1]
            val dist = haversineMeters(detailedCoords[i], detailedCoords[i + 1])

            edges += RoadEdgeEntity(
                id = "${prefix}_edge_$i",
                fromNodeId = fromId,
                toNodeId = toId,
                distanceMeters = dist.coerceAtLeast(5.0),
                riskWeight = 0.0,
                accessibilityPenalty = accessibilityPenalty,
                isBidirectional = true, // Permite navegación en ambos sentidos
                isBlocked = false
            )
        }
    }

    private fun haversineMeters(a: RouteCoordinate, b: RouteCoordinate): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val h = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        return 2 * earthRadius * kotlin.math.asin(kotlin.math.sqrt(h))
    }
}
