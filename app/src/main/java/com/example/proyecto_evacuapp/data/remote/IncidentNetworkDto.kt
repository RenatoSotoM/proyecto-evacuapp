package com.example.proyecto_evacuapp.data.remote

import com.google.gson.annotations.SerializedName

data class IncidentNetworkDto(
    @SerializedName("type") val type: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class IncidentResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("status") val status: String
)