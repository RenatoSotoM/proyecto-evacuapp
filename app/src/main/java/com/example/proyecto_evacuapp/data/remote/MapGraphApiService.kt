package com.example.proyecto_evacuapp.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class GraphNodeDto(
    @SerializedName("id", alternate = ["nodeId", "node_id"]) val id: String? = null,
    @SerializedName("lat", alternate = ["latitude"]) val lat: Double? = 0.0,
    @SerializedName("lon", alternate = ["longitude"]) val lon: Double? = 0.0
)

data class GeoPointDto(
    @SerializedName("lat", alternate = ["latitude"]) val lat: Double? = 0.0,
    @SerializedName("lon", alternate = ["longitude"]) val lon: Double? = 0.0
)

data class GraphEdgeDto(
    @SerializedName("id", alternate = ["edgeId", "edge_id"]) val id: String? = null,
    @SerializedName("fromNodeId", alternate = ["from_node_id", "fromId", "from_id", "source", "u", "from"]) val fromNodeId: String? = null,
    @SerializedName("toNodeId", alternate = ["to_node_id", "toId", "to_id", "target", "v", "to"]) val toNodeId: String? = null,
    @SerializedName("distanceMeters", alternate = ["distance_meters", "distance", "length", "weight"]) val distanceMeters: Double? = 0.0,
    @SerializedName("riskWeight", alternate = ["risk_weight", "risk"]) val riskWeight: Double? = 0.0,
    @SerializedName("accessibilityPenalty", alternate = ["accessibility_penalty", "accessibility"]) val accessibilityPenalty: Double? = 0.0,
    @SerializedName("isBlocked", alternate = ["is_blocked", "blocked"]) val isBlocked: Boolean? = false,
    @SerializedName("bidirectional", alternate = ["is_bidirectional", "two_way"]) val bidirectional: Boolean? = true,
    @SerializedName("oneway", alternate = ["is_oneway"]) val oneway: Boolean? = false,
    @SerializedName("highwayType", alternate = ["highway_type", "type", "highway"]) val highwayType: String? = "residential",
    @SerializedName("geometry", alternate = ["geom", "coordinates"]) val geometry: List<GeoPointDto>? = null
)

data class MapGraphResponseDto(
    @SerializedName("nodes") val nodes: List<GraphNodeDto> = emptyList(),
    @SerializedName("edges") val edges: List<GraphEdgeDto> = emptyList()
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
