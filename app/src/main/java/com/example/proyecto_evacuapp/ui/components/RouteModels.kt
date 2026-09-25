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
 * Variantes de ruta calculadas por LocalRouteEngine.calculateRouteAlternatives.
 * SEGURA: 100% libre de riesgos/bloqueos.
 * ALTERNATIVA_1: Vía secundaria desviando aristas de Ruta Segura.
 * ALTERNATIVA_2: Tercera opción por vías secundarias.
 * OFFLINE: Generada puramente desde el grafo local en Room DB / memoria.
 */
enum class RouteVariant {
    SEGURA,
    ALTERNATIVA_1,
    ALTERNATIVA_2,
    OFFLINE,
    PRINCIPAL,
    ACCESIBLE
}

data class LocalRouteResult(
    val points: List<RouteCoordinate>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val engineName: String,
    val warnings: List<String> = emptyList(),
    val variant: RouteVariant = RouteVariant.SEGURA,
    val label: String = "Ruta",
    val avoidsVerifiedRisk: Boolean = true,
    val maxAccessibilityPenaltyOnPath: Double = 0.0,
    val statusMessage: String? = null
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
