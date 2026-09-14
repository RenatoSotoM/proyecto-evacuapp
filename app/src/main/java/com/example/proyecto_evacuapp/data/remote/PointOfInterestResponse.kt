package com.example.proyecto_evacuapp.data.remote

data class PointOfInterestResponse(
    val id: String,
    val name: String,
    val type: String,
    val address: String?,
    val active: Boolean,
    val latitude: Double,
    val longitude: Double
)