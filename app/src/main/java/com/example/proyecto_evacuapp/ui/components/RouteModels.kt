package com.example.proyecto_evacuapp.ui.components

data class RouteCoordinate(
    val latitude: Double,
    val longitude: Double
)

enum class RouteMobilityProfile {
    VEHICLE,
    WALKING,
    BICYCLE,
    REDUCED_MOBILITY;

    companion object {
        fun fromUiMode(mode: String): RouteMobilityProfile = when (mode) {
            "Vehículo" -> VEHICLE
            "Bicicleta" -> BICYCLE
            "Movilidad reducida" -> REDUCED_MOBILITY
            else -> WALKING
        }
    }
}

data class LocalRouteResult(
    val points: List<RouteCoordinate>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val engineName: String,
    val warnings: List<String> = emptyList()
)

data class LocalIncident(
    val id: String,
    val title: String,
    val alpha: Double,
    val beta: Double,
    val severity: Int,
    val affectedSegmentId: String
) {
    val confidence: Double
        get() = alpha / (alpha + beta)

    val isVerified: Boolean
        get() = confidence >= 0.75
}