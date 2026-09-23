package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SafeZonesApiService {

    @GET("safe-zones")
    suspend fun getAllSafeZones(): Response<List<SafeZoneDto>>

    @GET("safe-zones/nearby")
    suspend fun getNearbySafeZones(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radiusMeters: Double? = 5000.0
    ): Response<List<SafeZoneNearbyDto>>
}