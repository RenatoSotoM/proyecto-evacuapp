package com.example.proyecto_evacuapp.data.remote

data class IncidentNetworkDto(
    val type: String,
    val severity: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double
)