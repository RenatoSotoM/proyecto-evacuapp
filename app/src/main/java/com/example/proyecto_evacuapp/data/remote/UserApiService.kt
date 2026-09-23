package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserApiService {
    @GET("users/me")
    suspend fun getMe(): Response<UserMeResponse>

    @PUT("users/me/mobility-profile")
    suspend fun updateMobilityProfile(@Body dto: UpdateMobilityProfileRequest): Response<MobilityProfileResponse>
}