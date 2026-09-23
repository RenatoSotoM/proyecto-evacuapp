package com.example.proyecto_evacuapp.data.remote.dto

data class EmergencyResponse(
    val id: String?,
    val type: String?,
    val status: String?,
    val title: String?,
    val description: String?,
    val startedAt: String?,
    val affectedArea: GeoJsonPolygon?,
    val active: Boolean?
)

data class GeoJsonPolygon(
    val type: String,
    val coordinates: List<List<List<Double>>>
)