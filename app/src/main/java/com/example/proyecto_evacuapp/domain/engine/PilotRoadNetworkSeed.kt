package com.example.proyecto_evacuapp.domain.engine

import com.example.proyecto_evacuapp.ui.components.RoadEdgeEntity
import com.example.proyecto_evacuapp.ui.components.RoadNodeEntity
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate

data class RoadNetworkSeed(
    val nodes: List<RoadNodeEntity>,
    val edges: List<RoadEdgeEntity>
)

/**
 * Generador dinámico de red vial. En lugar de usar una comuna fija predeterminada (como San Bernardo),
 * calcula la malla de nodos y aristas en tiempo de ejecución interpolando directamente
 * entre el origen real del usuario (GPS actual) y el destino de evacuación seleccionado.
 * Esto permite probar y operar el ruteo con Dijkstra en cualquier ubicación.
 */
object PilotRoadNetworkSeed {

    private const val HUB_ORIGIN = "hub_origin"
    private const val HUB_DESTINATION = "hub_destination"

    fun buildDynamic(origin: RouteCoordinate, destination: RouteCoordinate): RoadNetworkSeed {
        val nodes = mutableListOf<RoadNodeEntity>()
        val edges = mutableListOf<RoadEdgeEntity>()

        nodes += RoadNodeEntity(HUB_ORIGIN, origin.latitude, origin.longitude)
        nodes += RoadNodeEntity(HUB_DESTINATION, destination.latitude, destination.longitude)

        // Generamos los 4 caminos alternativos con desvíos geométricos y perfiles distintos
        addDynamicChain(nodes, edges, prefix = "veh", origin, destination, lateralOffset = 0.0002, accessibilityPenalty = 0.10)
        addDynamicChain(nodes, edges, prefix = "bic", origin, destination, lateralOffset = -0.0002, accessibilityPenalty = 0.30)
        addDynamicChain(nodes, edges, prefix = "rm",  origin, destination, lateralOffset = 0.0004, accessibilityPenalty = 0.05)
        addDynamicChain(
            nodes, edges, prefix = "walk", origin, destination, lateralOffset = -0.0004,
            accessibilityPenalty = 0.20,
            accessibilityOverrideByIndex = mapOf(2 to 0.90) // tramo con penalización alta simulada
        )

        return RoadNetworkSeed(nodes, edges)
    }

    private fun addDynamicChain(
        nodes: MutableList<RoadNodeEntity>,
        edges: MutableList<RoadEdgeEntity>,
        prefix: String,
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        lateralOffset: Double,
        accessibilityPenalty: Double,
        accessibilityOverrideByIndex: Map<Int, Double> = emptyMap()
    ) {
        val steps = 5
        val chainNodes = mutableListOf<Pair<Double, Double>>()
        val chainNodeIds = mutableListOf<String>()

        for (i in 1 until steps) {
            val fraction = i.toDouble() / steps
            // Interpolación lineal entre origen y destino con un leve desplazamiento lateral perpendicular
            val lat = origin.latitude + (destination.latitude - origin.latitude) * fraction + lateralOffset
            val lon = origin.longitude + (destination.longitude - origin.longitude) * fraction + lateralOffset

            val id = "${prefix}_$i"
            nodes += RoadNodeEntity(id, lat, lon)
            chainNodeIds += id
            chainNodes += (lat to lon)
        }

        val fullSequence = listOf(HUB_ORIGIN) + chainNodeIds + listOf(HUB_DESTINATION)
        val fullCoords = listOf(origin.latitude to origin.longitude) + chainNodes + listOf(destination.latitude to destination.longitude)

        for (i in 0 until fullSequence.size - 1) {
            val fromId = fullSequence[i]
            val toId = fullSequence[i + 1]
            val distance = haversineMetersPair(fullCoords[i], fullCoords[i + 1])
            val penalty = accessibilityOverrideByIndex[i] ?: accessibilityPenalty

            edges += RoadEdgeEntity(
                id = "${prefix}_edge_$i",
                fromNodeId = fromId,
                toNodeId = toId,
                distanceMeters = distance.coerceAtLeast(5.0), // Evitar distancia cero
                riskWeight = 0.0,
                accessibilityPenalty = penalty,
                isBidirectional = true,
                isBlocked = false
            )
        }
    }

    private fun haversineMetersPair(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(b.first - a.first)
        val dLon = Math.toRadians(b.second - a.second)
        val lat1 = Math.toRadians(a.first)
        val lat2 = Math.toRadians(b.first)

        val h = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        return 2 * earthRadius * kotlin.math.asin(kotlin.math.sqrt(h))
    }
}