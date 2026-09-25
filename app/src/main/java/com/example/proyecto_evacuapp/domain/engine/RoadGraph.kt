package com.example.proyecto_evacuapp.domain.engine

import android.util.Log
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GraphNode(
    val id: String? = null,
    val coordinate: RouteCoordinate = RouteCoordinate(0.0, 0.0)
)

/** Arista mutable en memoria: riskWeight/isBlocked se actualizan en caliente ante incidentes. */
data class GraphEdge(
    val id: String? = null,
    val fromId: String? = null,
    val toId: String? = null,
    val distanceMeters: Double = 0.0,
    var riskWeight: Double = 0.0,
    var accessibilityPenalty: Double = 0.0,
    var isBlocked: Boolean = false,
    val bidirectional: Boolean = true,
    var blockingIncidentLocalId: String? = null,
    val highwayType: String = "residential",
    val geometry: List<RouteCoordinate> = emptyList()
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

    private val _nodes = HashMap<String, GraphNode>()
    private val _edges = HashMap<String, GraphEdge>()
    private val adjacency = HashMap<String, MutableList<String>>() // nodeId -> edgeIds salientes

    val nodes: Map<String, GraphNode> get() = _nodes
    val edges: Map<String, GraphEdge> get() = _edges

    fun load(nodeList: List<GraphNode>, edgeList: List<GraphEdge>) {
        _nodes.clear()
        _edges.clear()
        adjacency.clear()
        nodeList.forEach { node ->
            val id = node.id ?: return@forEach
            _nodes[id] = node
        }
        edgeList.forEach { edge ->
            val id = edge.id ?: return@forEach
            val fromId = edge.fromId ?: return@forEach
            val toId = edge.toId ?: return@forEach
            _edges[id] = edge
            adjacency.getOrPut(fromId) { mutableListOf() }.add(id)
            if (edge.bidirectional) {
                adjacency.getOrPut(toId) { mutableListOf() }.add(id)
            }
        }
    }

    fun isEmpty(): Boolean = _nodes.isEmpty() || _edges.isEmpty()

    fun edgeById(edgeId: String): GraphEdge? = _edges[edgeId]

    fun allEdges(): Collection<GraphEdge> = _edges.values

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

    fun findNearestNode(lat: Double, lon: Double, maxRadiusMeters: Double = 1000.0): GraphNode? {
        var closestNode: GraphNode? = null
        var minDistance = Double.MAX_VALUE

        for (node in nodes.values) {
            val dist = haversineDistance(lat, lon, node.coordinate.latitude, node.coordinate.longitude)
            if (dist < minDistance && dist <= maxRadiusMeters) {
                minDistance = dist
                closestNode = node
            }
        }

        if (closestNode != null) {
            Log.d("EVAC_DEBUG", "Nodo encontrado a $minDistance metros (ID: ${closestNode.id})")
        } else {
            Log.e("EVAC_DEBUG", "No se encontró ningún nodo a menos de $maxRadiusMeters m de ($lat, $lon)")
        }

        return closestNode
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radio de la Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

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
     * Retorna TODAS las aristas/segmentos situados dentro de un radio de seguridad (buffer) de 15-20 metros
     * alrededor de las coordenadas de un reporte. Garantiza que reportes cerca de esquinas e intersecciones
     * bloqueen todos los accesos inmediatos a ese tramo vial.
     */
    fun edgesWithinRadius(point: RouteCoordinate, radiusMeters: Double = 20.0): List<GraphEdge> {
        val matchingEdges = mutableListOf<GraphEdge>()
        for (edge in edges.values) {
            val from = nodes[edge.fromId]?.coordinate ?: continue
            val to = nodes[edge.toId]?.coordinate ?: continue

            val distToSegment = distancePointToSegmentMeters(point, from, to)
            val distToFromNode = haversineMeters(point, from)
            val distToToNode = haversineMeters(point, to)

            if (distToSegment <= radiusMeters || distToFromNode <= radiusMeters || distToToNode <= radiusMeters) {
                matchingEdges.add(edge)
            }
        }

        return matchingEdges.ifEmpty {
            listOfNotNull(nearestEdge(point, maxDistanceMeters = 60.0))
        }
    }

    /**
     * Map Snapping Avanzado: Enganche por vector de movimiento (bearing), velocidad y tipo de vía (Autopista vs Caletera vs Local)
     */
    fun nearestMatchingEdge(
        point: RouteCoordinate,
        bearing: Float? = null,
        speedMps: Double? = null,
        currentEdgeId: String? = null,
        maxDistanceMeters: Double = 60.0
    ): GraphEdge? {
        var bestEdge: GraphEdge? = null
        var bestScore = Double.MAX_VALUE

        for (edge in edges.values) {
            val from = nodes[edge.fromId]?.coordinate ?: continue
            val to = nodes[edge.toId]?.coordinate ?: continue

            val dist = distancePointToSegmentMeters(point, from, to)
            if (dist > maxDistanceMeters) continue

            val edgeAzimuth = calculateAzimuthDegrees(from, to)

            // 1. Filtrado de Aristas por Rumbo (Sensible al Sentido de Marcha)
            val angleDiff = if (bearing != null && bearing >= 0) {
                if (edge.bidirectional) {
                    val forwardDiff = angularDifferenceDegrees(bearing.toDouble(), edgeAzimuth)
                    val reverseDiff = angularDifferenceDegrees(bearing.toDouble(), (edgeAzimuth + 180.0) % 360.0)
                    minOf(forwardDiff, reverseDiff)
                } else {
                    angularDifferenceDegrees(bearing.toDouble(), edgeAzimuth)
                }
            } else {
                0.0
            }

            // Descartar si la diferencia angular es > 60° respecto al movimiento actual (evita salto a carril opuesto)
            if (bearing != null && bearing >= 0 && angleDiff > 60.0) continue

            // 2. Desambiguación entre Autopista, Caletera y Calle Local según Velocidad
            var score = dist + (angleDiff * 0.4)

            if (speedMps != null && speedMps > 13.88) { // > 50 km/h: Autopista
                if (edge.highwayType in setOf("service", "residential", "tertiary")) {
                    score += 50.0 // Penalizar caletera o calle local a alta velocidad
                }
            } else if (speedMps != null && speedMps <= 8.33) { // <= 30 km/h: Caletera / Salida
                if (edge.highwayType in setOf("service", "residential")) {
                    score -= 10.0 // Permitir enganche a caletera a baja velocidad
                }
            }

            // 3. Continuidad de Trayectoria: Mantener posición sobre la vía actual
            if (currentEdgeId != null && edge.id == currentEdgeId) {
                score -= 15.0 // Bonificación de inercia sobre la vía actual
            }

            if (score < bestScore) {
                bestScore = score
                bestEdge = edge
            }
        }

        return bestEdge ?: nearestEdge(point, maxDistanceMeters)
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
        edgePenalties: Map<String, Double> = emptyMap(),
        startBearing: Float? = null
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
            val element = queue.poll() ?: break
            val currentId = element.first
            val currentDist = element.second
            if (currentId in visited) continue
            visited += currentId
            if (currentId == endNodeId) break

            val outgoingEdgeIds = adjacency[currentId] ?: continue
            for (edgeId in outgoingEdgeIds) {
                val edge = edges[edgeId] ?: continue

                // Respetar sentido único en vías unidireccionales
                if (!edge.bidirectional && currentId != edge.fromId) {
                    continue
                }

                val neighborId = (if (edge.fromId == currentId) edge.toId else edge.fromId) ?: continue
                if (neighborId in visited) continue

                var extraPenalty = edgePenalties[edgeId] ?: 0.0

                // Penalización Angular Dinámica (Permitir retorno si el frente está bloqueado):
                if (currentId == startNodeId && startBearing != null && startBearing >= 0) {
                    val fromCoord = nodes[edge.fromId]?.coordinate
                    val toCoord = nodes[edge.toId]?.coordinate
                    if (fromCoord != null && toCoord != null) {
                        val edgeAzimuth = calculateAzimuthDegrees(fromCoord, toCoord)
                        val angleDiff = angularDifferenceDegrees(startBearing.toDouble(), edgeAzimuth)

                        if (angleDiff > 90.0) {
                            // Verifica si existe alguna vía saliente directa (<= 90°) que esté abierta (sin bloqueo)
                            val hasOpenForwardEdge = outgoingEdgeIds.any { id ->
                                val e = edges[id] ?: return@any false
                                if (e.isBlocked) return@any false
                                val fc = nodes[e.fromId]?.coordinate ?: return@any false
                                val tc = nodes[e.toId]?.coordinate ?: return@any false
                                val az = calculateAzimuthDegrees(fc, tc)
                                val diff = angularDifferenceDegrees(startBearing.toDouble(), az)
                                diff <= 90.0 && edgeCost(e, weights, profile, avoidVerifiedRisk, avoidInaccessible, 0.0) < HARD_BLOCK_COST
                            }

                            if (hasOpenForwardEdge) {
                                // Vía directa despejada: Mantiene penalización de U-turn (+5000m) para evitar bucles
                                extraPenalty += 5000.0
                            } else {
                                // Vía directa bloqueada por reporte: ANULA la penalización (extraPenalty = 0) para ordenar dar la vuelta
                                extraPenalty += 0.0
                            }
                        }
                    }
                }

                val cost = edgeCost(edge, weights, profile, avoidVerifiedRisk, avoidInaccessible, extraPenalty)
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
        val points = ArrayList<RouteCoordinate>()
        for (edgeId in edgePath) {
            val edge = edges.getValue(edgeId)
            if (edge.geometry.isNotEmpty()) {
                if (points.isEmpty()) {
                    points.addAll(edge.geometry)
                } else {
                    val tail = points.last()
                    val startGeo = edge.geometry.first()
                    val endGeo = edge.geometry.last()
                    if (haversineMeters(tail, startGeo) < 5.0) {
                        points.addAll(edge.geometry.drop(1))
                    } else if (haversineMeters(tail, endGeo) < 5.0) {
                        points.addAll(edge.geometry.reversed().drop(1))
                    } else {
                        points.addAll(edge.geometry)
                    }
                }
            } else {
                val fromCoord = nodes[edge.fromId]?.coordinate
                val toCoord = nodes[edge.toId]?.coordinate
                if (fromCoord != null && (points.isEmpty() || points.last() != fromCoord)) {
                    points.add(fromCoord)
                }
                if (toCoord != null && (points.isEmpty() || points.last() != toCoord)) {
                    points.add(toCoord)
                }
            }
        }
        if (points.isEmpty()) {
            points.addAll(nodePath.mapNotNull { nodes[it]?.coordinate })
        }

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

/** Calcula el azimut angular en grados [0°, 360°) entre dos coordenadas. */
fun calculateAzimuthDegrees(start: RouteCoordinate, end: RouteCoordinate): Double {
    val dLon = Math.toRadians(end.longitude - start.longitude)
    val lat1 = Math.toRadians(start.latitude)
    val lat2 = Math.toRadians(end.latitude)

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    val brng = Math.toDegrees(atan2(y, x))
    return (brng + 360.0) % 360.0
}

/** Diferencia angular mínima en grados [0°, 180°] entre dos ángulos. */
fun angularDifferenceDegrees(angle1: Double, angle2: Double): Double {
    val diff = kotlin.math.abs(angle1 - angle2) % 360.0
    return if (diff > 180.0) 360.0 - diff else diff
}