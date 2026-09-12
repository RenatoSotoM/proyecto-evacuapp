package com.example.proyecto_evacuapp.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone") val phone: String? = null
)

data class UserResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String? = "USER"
)

data class AuthResponse(
    // Maneja si NestJS responde accessToken o access_token
    @SerializedName("accessToken", alternate = ["access_token"])
    val accessToken: String,
    @SerializedName("tokenType", alternate = ["token_type"])
    val tokenType: String? = "Bearer",
    @SerializedName("user")
    val user: UserResponseDto
)

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
}