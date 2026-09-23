package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PointsOfInterestApi {
    @GET("points-of-interest/nearby")
    suspend fun getNearbyPointsOfInterest(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 30000.0,
        @Query("type") type: String? = null
    ): Response<List<PointOfInterestResponse>>
}