package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IncidentApiService {

    @POST("incidents")
    suspend fun createIncident(@Body dto: IncidentNetworkDto): Response<IncidentResponseDto>

    @GET("incidents")
    suspend fun getIncidents(): Response<List<IncidentResponseDto>>

    @GET("incidents/active")
    suspend fun getActiveIncidents(): Response<List<IncidentResponseDto>>

    @GET("incidents/nearby")
    suspend fun getNearbyIncidents(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radius: Double? = 30000.0
    ): Response<List<IncidentResponseDto>>

    @POST("incidents/{id}/vote")
    suspend fun voteIncident(
        @Path("id") remoteId: String,
        @Body voteDto: VoteDto
    ): Response<IncidentResponseDto>
}
