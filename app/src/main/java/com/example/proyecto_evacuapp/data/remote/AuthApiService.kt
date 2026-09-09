package com.example.proyecto_evacuapp.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

data class UserResponseDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val user: UserResponseDto
)

interface AuthApiService {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
}