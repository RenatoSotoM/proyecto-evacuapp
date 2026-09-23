package com.example.proyecto_evacuapp.domain.engine

import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GraphNode(
    val id: String,
    val coordinate: RouteCoordinate
)

/** Arista mutable en memoria: riskWeight/isBlocked se actualizan en caliente ante incidentes. */
data class GraphEdge(
    val id: String,
    val fromId: String,
    val toId: String,
    val distanceMeters: Double,
    var riskWeight: Double,
    var accessibilityPenalty: Double,
    var isBlocked: Boolean,
    val bidirectional: Boolean,
    var blockingIncidentLocalId: String? = null
)

data class PathResult(
    val edgeIds: List<String>,
    val points: List<RouteCoordinate>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maxRiskOnPath: Double,
    val maxAccessibilityPenaltyOnPath: Double
)

/**
 * Grafo vial en memoria. Se carga una vez desde Room (vía RoadNetworkRepository) y se
 * mantiene sincronizado con los incidentes verificados sin necesidad de recargar todo.
 */
class RoadGraph {

    private val nodes = HashMap<String, GraphNode>()
    private val edges = HashMap<String, GraphEdge>()
    private val adjacency = HashMap<String, MutableList<String>>() // nodeId -> edgeIds salientes

    fun load(nodeList: List<GraphNode>, edgeList: List<GraphEdge>) {
        nodes.clear()
        edges.clear()
        adjacency.clear()
        nodeList.forEach { nodes[it.id] = it }
        edgeList.forEach { edge ->
            edges[edge.id] = edge
            adjacency.getOrPut(edge.fromId) { mutableListOf() }.add(edge.id)
            if (edge.bidirectional) {
                adjacency.getOrPut(edge.toId) { mutableListOf() }.add(edge.id)
            }
        }
    }

    fun isEmpty(): Boolean = nodes.isEmpty() || edges.isEmpty()

    fun edgeById(edgeId: String): GraphEdge? = edges[edgeId]

    fun allEdges(): Collection<GraphEdge> = edges.values

    fun applyEdgeRisk(edgeId: String, riskWeight: Double, blocked: Boolean, incidentLocalId: String?): Boolean {
        val edge = edges[edgeId] ?: return false
        val changed = edge.riskWeight != riskWeight || edge.isBlocked != blocked
        edge.riskWeight = riskWeight
        edge.isBlocked = blocked
        edge.blockingIncidentLocalId = incidentLocalId
        return changed
    }

    fun applyEdgeAccessibility(edgeId: String, penalty: Double, incidentLocalId: String?): Boolean {
        val edge = edges[edgeId] ?: return false
        val changed = edge.accessibilityPenalty != penalty
        edge.accessibilityPenalty = penalty
        edge.blockingIncidentLocalId = incidentLocalId
        return changed
    }

    fun clearEdgeRisk(edgeId: String): Boolean {
        val edge = edges[edgeId] ?: return false
        val changed = edge.riskWeight != 0.0 || edge.isBlocked || edge.accessibilityPenalty != 0.0
        edge.riskWeight = 0.0
        edge.isBlocked = false
        edge.accessibilityPenalty = 0.0
        edge.blockingIncidentLocalId = null
        return changed
    }

    fun findEdgesByIncidentId(incidentLocalId: String): List<GraphEdge> =
        edges.values.filter { it.blockingIncidentLocalId == incidentLocalId }

    fun nearestNode(point: RouteCoordinate): GraphNode? =
        nodes.values.minByOrNull { haversineMeters(it.coordinate, point) }

    /** Arista cuyo tramo (fromNode-toNode) está más cerca del punto dado, dentro de un radio. */
    fun nearestEdge(point: RouteCoordinate, maxDistanceMeters: Double = 60.0): GraphEdge? {
        var best: GraphEdge? = null
        var bestDistance = Double.MAX_VALUE
        for (edge in edges.values) {
            val from = nodes[edge.fromId]?.coordinate ?: continue
            val to = nodes[edge.toId]?.coordinate ?: continue
            val dist = distancePointToSegmentMeters(point, from, to)
            if (dist < bestDistance) {
                bestDistance = dist
                best = edge
            }
        }
        return if (bestDistance <= maxDistanceMeters) best else null
    }

    /**
     * Dijkstra sobre el costo adaptativo C(e) (pesos no negativos). [edgePenalties] permite
     * penalizar aristas ya usadas por otra alternativa, para favorecer diversidad entre rutas.
     */
    fun shortestPath(
        startNodeId: String,
        endNodeId: String,
        weights: CostWeights,
        profile: RouteMobilityProfile,
        avoidVerifiedRisk: Boolean = false,
        avoidInaccessible: Boolean = false,
        edgePenalties: Map<String, Double> = emptyMap()
    ): PathResult? {
        if (!nodes.containsKey(startNodeId) || !nodes.containsKey(endNodeId)) return null
        if (startNodeId == endNodeId) return null

        val distTo = HashMap<String, Double>().withDefault { Double.MAX_VALUE }
        val edgeUsedToReach = HashMap<String, String>()
        val prevNode = HashMap<String, String>()
        val visited = HashSet<String>()

        distTo[startNodeId] = 0.0
        val queue = PriorityQueue<Pair<String, Double>>(compareBy { it.second })
        queue.add(startNodeId to 0.0)

        while (queue.isNotEmpty()) {
            val (currentId, currentDist) = queue.poll()
            if (currentId in visited) continue
            visited += currentId
            if (currentId == endNodeId) break

            val outgoingEdgeIds = adjacency[currentId] ?: continue
            for (edgeId in outgoingEdgeIds) {
                val edge = edges[edgeId] ?: continue
                val neighborId = if (edge.fromId == currentId) edge.toId else edge.fromId
                if (neighborId in visited) continue

                val penalty = edgePenalties[edgeId] ?: 0.0
                val cost = edgeCost(edge, weights, profile, avoidVerifiedRisk, avoidInaccessible, penalty)
                if (cost >= HARD_BLOCK_COST) continue

                val candidateDist = currentDist + cost
                if (candidateDist < distTo.getValue(neighborId)) {
                    distTo[neighborId] = candidateDist
                    prevNode[neighborId] = currentId
                    edgeUsedToReach[neighborId] = edgeId
                    queue.add(neighborId to candidateDist)
                }
            }
        }

        if (endNodeId !in prevNode) return null

        val nodePath = ArrayList<String>()
        val edgePath = ArrayList<String>()
        var cursor = endNodeId
        nodePath.add(cursor)
        while (cursor != startNodeId) {
            val prev = prevNode[cursor] ?: return null
            val usedEdge = edgeUsedToReach[cursor] ?: return null
            edgePath.add(usedEdge)
            cursor = prev
            nodePath.add(cursor)
        }
        nodePath.reverse()
        edgePath.reverse()

        var totalDistance = 0.0
        var maxRisk = 0.0
        var maxAccessibility = 0.0
        for (edgeId in edgePath) {
            val edge = edges.getValue(edgeId)
            totalDistance += edge.distanceMeters
            maxRisk = maxOf(maxRisk, edge.riskWeight)
            maxAccessibility = maxOf(maxAccessibility, edge.accessibilityPenalty)
        }
        val totalDuration = totalDistance / speedMetersPerSecondFor(profile)
        val points = nodePath.mapNotNull { nodes[it]?.coordinate }

        return PathResult(
            edgeIds = edgePath,
            points = points,
            distanceMeters = totalDistance,
            durationSeconds = totalDuration,
            maxRiskOnPath = maxRisk,
            maxAccessibilityPenaltyOnPath = maxAccessibility
        )
    }
}

fun haversineMeters(a: RouteCoordinate, b: RouteCoordinate): Double {
    val earthRadiusMeters = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)

    val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
    val c = 2 * atan2(sqrt(h), sqrt(1 - h))
    return earthRadiusMeters * c
}

/** Distancia aproximada (metros) de un punto a un segmento [start, end], en proyección plana local. */
fun distancePointToSegmentMeters(point: RouteCoordinate, start: RouteCoordinate, end: RouteCoordinate): Double {
    val latRef = Math.toRadians(start.latitude)
    fun toXY(p: RouteCoordinate): DoubleArray {
        val x = Math.toRadians(p.longitude) * cos(latRef) * 6_371_000.0
        val y = Math.toRadians(p.latitude) * 6_371_000.0
        return doubleArrayOf(x, y)
    }

    val p = toXY(point)
    val a = toXY(start)
    val b = toXY(end)

    val abx = b[0] - a[0]
    val aby = b[1] - a[1]
    val lengthSq = abx * abx + aby * aby

    val t = if (lengthSq == 0.0) 0.0 else {
        val apx = p[0] - a[0]
        val apy = p[1] - a[1]
        ((apx * abx + apy * aby) / lengthSq).coerceIn(0.0, 1.0)
    }

    val closestX = a[0] + t * abx
    val closestY = a[1] + t * aby
    val dx = p[0] - closestX
    val dy = p[1] - closestY
    return sqrt(dx * dx + dy * dy)
}