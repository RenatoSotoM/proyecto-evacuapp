package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface IncidentApiService {
    @POST("incidents")
    suspend fun createIncident(@Body incident: IncidentNetworkDto): Response<IncidentResponseDto>

    @POST("incidents/{id}/confirm")
    suspend fun confirmIncident(@Path("id") remoteId: String): Response<Unit>

    @POST("incidents/{id}/reject")
    suspend fun rejectIncident(@Path("id") remoteId: String): Response<Unit>
}