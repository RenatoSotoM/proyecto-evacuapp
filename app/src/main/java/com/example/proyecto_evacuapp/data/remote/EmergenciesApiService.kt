package com.example.proyecto_evacuapp.data.remote

import com.example.proyecto_evacuapp.data.remote.dto.EmergencyResponse
import retrofit2.Response
import retrofit2.http.GET

interface EmergenciesApiService {
    @GET("emergencies/active")
    suspend fun getActiveEmergency(): Response<EmergencyResponse>
}