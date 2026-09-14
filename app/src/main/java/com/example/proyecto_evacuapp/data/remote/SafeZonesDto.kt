package com.example.proyecto_evacuapp.data.remote

data class GeometryPoint(
    val type: String,
    val coordinates: List<Double> // [longitud, latitud]
)

data class SafeZoneDto(
    val id: String,
    val name: String,
    val description: String?,
    val capacity: Int?,
    val active: Boolean,
    val location: GeometryPoint?
) {
    val latitude: Double? get() = location?.coordinates?.getOrNull(1)
    val longitude: Double? get() = location?.coordinates?.getOrNull(0)
}

data class SafeZoneNearbyDto(
    val id: String,
    val name: String,
    val description: String?,
    val capacity: Int?,
    val active: Boolean,
    val latitude: Double,
    val longitude: Double,
    val distance_meters: Double?
)