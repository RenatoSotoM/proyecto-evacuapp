package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IncidentApiService {

    @POST("incidents")
    suspend fun createIncident(@Body dto: IncidentNetworkDto): Response<IncidentResponseDto>

    @GET("incidents")
    suspend fun getIncidents(): Response<List<IncidentResponseDto>>
}