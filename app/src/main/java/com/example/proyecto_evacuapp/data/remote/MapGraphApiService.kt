package com.example.proyecto_evacuapp.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class GraphNodeDto(
    val id: String,
    val lat: Double,
    val lon: Double
)

data class GeoPointDto(
    val lat: Double,
    val lon: Double
)

data class GraphEdgeDto(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Double,
    val riskWeight: Double?,
    val accessibilityPenalty: Double?,
    val isBlocked: Boolean?,
    val bidirectional: Boolean?,
    val highwayType: String?,
    val geometry: List<GeoPointDto>?
)

data class MapGraphResponseDto(
    val nodes: List<GraphNodeDto>?,
    val edges: List<GraphEdgeDto>?
)

/**
 * Servicio Retrofit para consumir grafos viales optimizados por anillos desde el backend NestJS + PostGIS.
 */
interface MapGraphApiService {
    @GET("map-data/graph")
    suspend fun getGraphRing(
        @Header("Accept-Encoding") acceptEncoding: String = "gzip",
        @Query("centerLat") centerLat: Double,
        @Query("centerLon") centerLon: Double,
        @Query("ringMin") ringMin: Int,
        @Query("ringMax") ringMax: Int,
        @Query("travelMode") travelMode: String,
        @Query("isReducedMobility") isReducedMobility: Boolean,
        @Query("avoidIncidents") avoidIncidents: Boolean
    ): ResponseBody
}
