package com.example.proyecto_evacuapp.ui.components

import org.osmdroid.util.GeoPoint

data class RouteCoordinate(
    val latitude: Double,
    val longitude: Double
)

fun RouteCoordinate.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
fun GeoPoint.toRouteCoordinate(): RouteCoordinate = RouteCoordinate(latitude, longitude)

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

/**
 * Variante de ruta calculada por LocalRouteEngine.calculateRouteAlternatives.
 * PRINCIPAL: menor costo general. SEGURA: evita tramos con riesgo VERIFIED/PROBABLE.
 * ACCESIBLE: minimiza la penalización de accesibilidad para perfiles vulnerables.
 */
enum class RouteVariant {
    PRINCIPAL,
    SEGURA,
    ACCESIBLE
}

data class LocalRouteResult(
    val points: List<RouteCoordinate>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val engineName: String,
    val warnings: List<String> = emptyList(),
    // Campos nuevos con default: no rompen construcciones existentes de LocalRouteResult.
    val variant: RouteVariant = RouteVariant.PRINCIPAL,
    val label: String = "Ruta",
    val avoidsVerifiedRisk: Boolean = true,
    val maxAccessibilityPenaltyOnPath: Double = 0.0
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