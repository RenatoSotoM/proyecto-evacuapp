package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

data class IncidentResponseDto(
    val id: String,
    val type: String,
    val severity: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val status: String
)

interface IncidentApiService {
    @POST("api/v1/incidents")
    suspend fun createIncident(@Body incident: IncidentNetworkDto): Response<IncidentResponseDto>

    @POST("api/v1/incidents/{id}/confirm")
    suspend fun confirmIncident(@Path("id") remoteId: String): Response<Unit>

    @POST("api/v1/incidents/{id}/reject")
    suspend fun rejectIncident(@Path("id") remoteId: String): Response<Unit>
}