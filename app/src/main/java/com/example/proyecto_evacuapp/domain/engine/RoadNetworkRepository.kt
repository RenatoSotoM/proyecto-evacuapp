package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.example.proyecto_evacuapp.data.remote.MapGraphResponseDto
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.RoadEdgeEntity
import com.example.proyecto_evacuapp.ui.components.RoadGraphDao
import com.example.proyecto_evacuapp.ui.components.RoadNodeEntity
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader

private const val TAG = "RoadNetworkRepository"

/**
 * Repositorio espacial bajo demanda con Bounding Box (BBox) ajustable sobre Room DB.
 */
class RoadNetworkRepository {

    suspend fun ensureLoadedForRoute(context: Context?, origin: RouteCoordinate, destination: RouteCoordinate) {
        if (context == null) return
        withContext(Dispatchers.IO) {
            try {
                val db = EvacuAppDatabase.getInstance(context)
                val dao = db.roadGraphDao()
                ensureJsonImportedIntoRoom(context, dao)
            } catch (e: Exception) {
                Log.e(TAG, "Error in ensureLoadedForRoute: ${e.message}", e)
            }
        }
    }

    private suspend fun ensureJsonImportedIntoRoom(context: Context, dao: RoadGraphDao) {
        val file30 = File(context.filesDir, "local_graph_30km.json")
        val file15 = File(context.filesDir, "local_graph_15km.json")
        val fileToRead = if (file30.exists() && file30.length() > 0L) file30 else if (file15.exists() && file15.length() > 0L) file15 else null

        if (fileToRead != null) {
            val currentCount = dao.countNodes()
            val fileModified = fileToRead.lastModified()
            val prefs = context.getSharedPreferences("road_repo_prefs", Context.MODE_PRIVATE)
            val lastImportedTime = prefs.getLong("last_imported_${fileToRead.name}", 0L)

            if (currentCount == 0 || fileModified > lastImportedTime || (fileToRead == file30 && currentCount < 1000)) {
                try {
                    val response = FileReader(fileToRead).use { reader ->
                        Gson().fromJson(reader, MapGraphResponseDto::class.java)
                    }
                    val nodeEntities = response?.nodes?.mapNotNull {
                        val id = it.id ?: return@mapNotNull null
                        val lat = it.lat ?: 0.0
                        val lon = it.lon ?: 0.0
                        RoadNodeEntity(id = id, latitude = lat, longitude = lon)
                    } ?: emptyList()

                    val edgeEntities = response?.edges?.mapNotNull { edgeDto ->
                        val hType = edgeDto.highwayType?.lowercase() ?: "residential"
                        val id = edgeDto.id ?: return@mapNotNull null
                        val fromId = edgeDto.fromNodeId ?: return@mapNotNull null
                        val toId = edgeDto.toNodeId ?: return@mapNotNull null
                        val dist = edgeDto.distanceMeters ?: 10.0
                        val isOneway = edgeDto.oneway ?: false
                        val bidirectional = (edgeDto.bidirectional ?: true) && !isOneway

                        val accessibilityPenalty = when (hType) {
                            "steps" -> 1.0 // Escaleras catalogadas con la máxima penalización de accesibilidad
                            "footway", "pedestrian", "path" -> 0.0 // Senderos planos accesibles
                            else -> edgeDto.accessibilityPenalty ?: 0.05
                        }

                        RoadEdgeEntity(
                            id = id,
                            fromNodeId = fromId,
                            toNodeId = toId,
                            distanceMeters = dist,
                            riskWeight = edgeDto.riskWeight ?: 0.0,
                            accessibilityPenalty = accessibilityPenalty,
                            isBlocked = edgeDto.isBlocked ?: false,
                            isBidirectional = bidirectional
                        )
                    } ?: emptyList()

                    if (nodeEntities.isNotEmpty()) {
                        dao.insertNodes(nodeEntities)
                    }
                    if (edgeEntities.isNotEmpty()) {
                        dao.insertEdges(edgeEntities)
                    }
                    prefs.edit().putLong("last_imported_${fileToRead.name}", fileModified).apply()
                    Log.d(TAG, "Importados ${nodeEntities.size} nodos y ${edgeEntities.size} aristas desde ${fileToRead.name} a Room DB.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing local graph JSON into Room: ${e.message}", e)
                }
            }
        }
    }

    suspend fun loadTransientGraphForRoute(
        context: Context?,
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        margin: Double = 0.03
    ): RoadGraph? = withContext(Dispatchers.IO) {
        if (context == null) return@withContext null
        try {
            val db = EvacuAppDatabase.getInstance(context)
            val dao = db.roadGraphDao()
            ensureJsonImportedIntoRoom(context, dao)

            // Cálculo Bounding Box Bidireccional envolviendo origen y destino con margen adaptativo de seguridad proporcional
            val distLat = Math.abs(origin.latitude - destination.latitude)
            val distLon = Math.abs(origin.longitude - destination.longitude)
            val maxDist = maxOf(distLat, distLon)
            val adaptiveMargin = maxOf(margin, maxDist * 0.35).coerceAtLeast(0.05)

            val minLat = minOf(origin.latitude, destination.latitude) - adaptiveMargin
            val maxLat = maxOf(origin.latitude, destination.latitude) + adaptiveMargin
            val minLon = minOf(origin.longitude, destination.longitude) - adaptiveMargin
            val maxLon = maxOf(origin.longitude, destination.longitude) + adaptiveMargin

            val nodeEntities = dao.getNodesInBBox(minLat, maxLat, minLon, maxLon)
            val edgeEntities = dao.getEdgesInBBox(minLat, maxLat, minLon, maxLon)

            if (nodeEntities.isEmpty() || edgeEntities.isEmpty()) {
                Log.w(TAG, "No se encontraron nodos/aristas en el BBox (margin=$adaptiveMargin) para origin=$origin, dest=$destination")
                return@withContext null
            }

            val graph = RoadGraph()
            val nodes = nodeEntities.map { GraphNode(id = it.id, coordinate = RouteCoordinate(it.latitude, it.longitude)) }
            val edges = edgeEntities.map {
                GraphEdge(
                    id = it.id,
                    fromId = it.fromNodeId,
                    toId = it.toNodeId,
                    distanceMeters = it.distanceMeters,
                    riskWeight = it.riskWeight,
                    accessibilityPenalty = it.accessibilityPenalty,
                    isBlocked = it.isBlocked,
                    bidirectional = it.isBidirectional,
                    blockingIncidentLocalId = it.blockingIncidentLocalId
                )
            }
            graph.load(nodes, edges)
            Log.d("EVAC_DEBUG", "Subgrafo transitorio BBox cargado: ${nodes.size} nodos, ${edges.size} aristas.")
            return@withContext graph
        } catch (e: Exception) {
            Log.e(TAG, "Error loading transient graph for route: ${e.message}", e)
            null
        }
    }

    suspend fun snapToNearestEdge(context: Context?, latitude: Double, longitude: Double, maxDistanceMeters: Double = 25.0): String? = withContext(Dispatchers.IO) {
        if (context == null) return@withContext null
        try {
            val db = EvacuAppDatabase.getInstance(context)
            val dao = db.roadGraphDao()
            ensureJsonImportedIntoRoom(context, dao)

            val minLat = latitude - 0.02
            val maxLat = latitude + 0.02
            val minLon = longitude - 0.02
            val maxLon = longitude + 0.02

            val nodeEntities = dao.getNodesInBBox(minLat, maxLat, minLon, maxLon)
            val edgeEntities = dao.getEdgesInBBox(minLat, maxLat, minLon, maxLon)

            if (nodeEntities.isEmpty() || edgeEntities.isEmpty()) return@withContext null

            val graph = RoadGraph()
            val nodes = nodeEntities.map { GraphNode(id = it.id, coordinate = RouteCoordinate(it.latitude, it.longitude)) }
            val edges = edgeEntities.map {
                GraphEdge(
                    id = it.id,
                    fromId = it.fromNodeId,
                    toId = it.toNodeId,
                    distanceMeters = it.distanceMeters,
                    riskWeight = it.riskWeight,
                    accessibilityPenalty = it.accessibilityPenalty,
                    isBlocked = it.isBlocked,
                    bidirectional = it.isBidirectional,
                    blockingIncidentLocalId = it.blockingIncidentLocalId
                )
            }
            graph.load(nodes, edges)
            val point = RouteCoordinate(latitude, longitude)
            val nearestEdge = graph.nearestEdge(point, maxDistanceMeters)
            return@withContext nearestEdge?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error in snapToNearestEdge: ${e.message}", e)
            null
        }
    }

    suspend fun applyIncidents(incidents: List<IncidentEntity>): Boolean = true

    suspend fun snapCoordinateToRoad(context: Context?, latitude: Double, longitude: Double, maxDistanceMeters: Double = 35.0): RouteCoordinate = withContext(Dispatchers.IO) {
        if (context == null) return@withContext RouteCoordinate(latitude, longitude)
        try {
            val db = EvacuAppDatabase.getInstance(context)
            val dao = db.roadGraphDao()
            ensureJsonImportedIntoRoom(context, dao)

            val minLat = latitude - 0.02
            val maxLat = latitude + 0.02
            val minLon = longitude - 0.02
            val maxLon = longitude + 0.02

            val nodeEntities = dao.getNodesInBBox(minLat, maxLat, minLon, maxLon)
            val edgeEntities = dao.getEdgesInBBox(minLat, maxLat, minLon, maxLon)

            if (nodeEntities.isEmpty() || edgeEntities.isEmpty()) return@withContext RouteCoordinate(latitude, longitude)

            val graph = RoadGraph()
            val nodes = nodeEntities.map { GraphNode(id = it.id, coordinate = RouteCoordinate(it.latitude, it.longitude)) }
            val edges = edgeEntities.map {
                GraphEdge(
                    id = it.id,
                    fromId = it.fromNodeId,
                    toId = it.toNodeId,
                    distanceMeters = it.distanceMeters,
                    riskWeight = it.riskWeight,
                    accessibilityPenalty = it.accessibilityPenalty,
                    isBlocked = it.isBlocked,
                    bidirectional = it.isBidirectional,
                    blockingIncidentLocalId = it.blockingIncidentLocalId
                )
            }
            graph.load(nodes, edges)
            val point = RouteCoordinate(latitude, longitude)
            val nearestEdge = graph.nearestEdge(point, maxDistanceMeters)
            if (nearestEdge != null) {
                val fromNode = graph.nodes[nearestEdge.fromId]
                val toNode = graph.nodes[nearestEdge.toId]
                if (fromNode != null && toNode != null) {
                    val d1 = haversineMeters(point, fromNode.coordinate)
                    val d2 = haversineMeters(point, toNode.coordinate)
                    return@withContext if (d1 <= d2) fromNode.coordinate else toNode.coordinate
                }
            }
            return@withContext RouteCoordinate(latitude, longitude)
        } catch (e: Exception) {
            Log.e(TAG, "Error in snapCoordinateToRoad: ${e.message}", e)
            RouteCoordinate(latitude, longitude)
        }
    }
}
