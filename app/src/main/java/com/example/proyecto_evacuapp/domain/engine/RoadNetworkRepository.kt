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
        if (dao.countNodes() == 0) {
            val file = File(context.filesDir, "local_graph_30km.json")
            if (file.exists() && file.length() > 0L) {
                try {
                    val response = FileReader(file).use { reader ->
                        Gson().fromJson(reader, MapGraphResponseDto::class.java)
                    }
                    val nodeEntities = response?.nodes?.mapNotNull {
                        val id = it.id ?: return@mapNotNull null
                        val lat = it.lat ?: 0.0
                        val lon = it.lon ?: 0.0
                        RoadNodeEntity(id = id, latitude = lat, longitude = lon)
                    } ?: emptyList()

                    val nonVehicularHighways = setOf("footway", "pedestrian", "steps", "path", "bridleway", "cycleway", "corridor")
                    val edgeEntities = response?.edges?.mapNotNull { edgeDto ->
                        val hType = edgeDto.highwayType?.lowercase() ?: "residential"
                        if (hType in nonVehicularHighways) return@mapNotNull null
                        val id = edgeDto.id ?: return@mapNotNull null
                        val fromId = edgeDto.fromNodeId ?: return@mapNotNull null
                        val toId = edgeDto.toNodeId ?: return@mapNotNull null
                        val dist = edgeDto.distanceMeters ?: 10.0
                        val isOneway = edgeDto.oneway ?: false
                        val bidirectional = (edgeDto.bidirectional ?: true) && !isOneway

                        RoadEdgeEntity(
                            id = id,
                            fromNodeId = fromId,
                            toNodeId = toId,
                            distanceMeters = dist,
                            riskWeight = edgeDto.riskWeight ?: 0.0,
                            accessibilityPenalty = edgeDto.accessibilityPenalty ?: 0.05,
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
                    Log.d(TAG, "Importados ${nodeEntities.size} nodos y ${edgeEntities.size} aristas desde JSON a Room DB.")
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

            val minLat = minOf(origin.latitude, destination.latitude) - margin
            val maxLat = maxOf(origin.latitude, destination.latitude) + margin
            val minLon = minOf(origin.longitude, destination.longitude) - margin
            val maxLon = maxOf(origin.longitude, destination.longitude) + margin

            val nodeEntities = dao.getNodesInBBox(minLat, maxLat, minLon, maxLon)
            val edgeEntities = dao.getEdgesInBBox(minLat, maxLat, minLon, maxLon)

            if (nodeEntities.isEmpty() || edgeEntities.isEmpty()) {
                Log.w(TAG, "No se encontraron nodos/aristas en el BBox (margin=$margin) para origin=$origin, dest=$destination")
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

            val coord = RouteCoordinate(latitude, longitude)
            val matchedEdge = graph.nearestMatchingEdge(coord, maxDistanceMeters = maxDistanceMeters)
                ?: graph.edgesWithinRadius(coord, radiusMeters = maxDistanceMeters).firstOrNull()
            return@withContext matchedEdge?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error in snapToNearestEdge: ${e.message}", e)
            null
        }
    }

    suspend fun applyIncidents(incidents: List<IncidentEntity>): Boolean = true
}
