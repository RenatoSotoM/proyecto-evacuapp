package com.example.proyecto_evacuapp.domain.engine

import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile

/** Pesos w_d, w_t, w_r, w_a de C(e) = w_d*D(e) + w_t*T(e) + w_r*R(e) + w_a*A(e). */
data class CostWeights(
    val wDistance: Double,
    val wTime: Double,
    val wRisk: Double,
    val wAccessibility: Double
)

/** Umbral de riesgo que equivale a un incidente VERIFIED sobre una arista. */
const val RISK_VERIFIED_THRESHOLD = 0.85

/** Costo "infinito" práctico para aristas que deben tratarse como intransitables. */
const val HARD_BLOCK_COST = 1.0e12

/**
 * Velocidad de desplazamiento (m/s) por modo de transporte. Mismos valores que ya usaba
 * el stub original de LocalRouteEngine, para no alterar los tiempos estimados existentes.
 */
fun speedMetersPerSecondFor(profile: RouteMobilityProfile): Double = when (profile) {
    RouteMobilityProfile.VEHICLE -> 8.3
    RouteMobilityProfile.BICYCLE -> 4.2
    RouteMobilityProfile.REDUCED_MOBILITY -> 1.0
    RouteMobilityProfile.WALKING -> 1.3
}

/**
 * Escalas para llevar R(e) y A(e) (normalizados en [0,1]) a un orden de magnitud comparable
 * con distancia (metros) y tiempo (segundos), de modo que w_r y w_a tengan efecto perceptible.
 */
private const val RISK_SCALE_METERS_EQUIVALENT = 600.0
private const val ACCESSIBILITY_SCALE_METERS_EQUIVALENT = 600.0

object CostProfiles {

    /** Pesos por defecto según el perfil de movilidad del usuario (opción Principal). */
    fun weightsFor(profile: RouteMobilityProfile): CostWeights = when (profile) {
        RouteMobilityProfile.VEHICLE -> CostWeights(
            wDistance = 0.35, wTime = 0.40, wRisk = 0.20, wAccessibility = 0.05
        )
        RouteMobilityProfile.BICYCLE -> CostWeights(
            wDistance = 0.30, wTime = 0.35, wRisk = 0.25, wAccessibility = 0.10
        )
        RouteMobilityProfile.WALKING -> CostWeights(
            wDistance = 0.30, wTime = 0.30, wRisk = 0.25, wAccessibility = 0.15
        )
        RouteMobilityProfile.REDUCED_MOBILITY -> CostWeights(
            wDistance = 0.15, wTime = 0.15, wRisk = 0.20, wAccessibility = 0.50
        )
    }

    /** Pesos para la variante "Segura": prioriza fuertemente evitar riesgo. */
    val SAFE_WEIGHTS = CostWeights(wDistance = 0.15, wTime = 0.15, wRisk = 0.65, wAccessibility = 0.05)

    /** Pesos para la variante "Accesible": prioriza fuertemente minimizar A(e). */
    val ACCESSIBLE_WEIGHTS = CostWeights(wDistance = 0.15, wTime = 0.15, wRisk = 0.15, wAccessibility = 0.55)
}

/**
 * Calcula C(e) para una arista, dado un set de pesos, el modo de transporte (para T(e)) y
 * las restricciones duras de la variante que se está calculando.
 *
 * @param avoidVerifiedRisk si es true, cualquier arista con R(e) >= RISK_VERIFIED_THRESHOLD
 *   se trata como intransitable (usada por la variante Segura).
 * @param avoidInaccessible si es true, cualquier arista con A(e) >= 0.9 se trata como
 *   intransitable (usada por la variante Accesible cuando el perfil es REDUCED_MOBILITY).
 */
fun edgeCost(
    edge: GraphEdge,
    weights: CostWeights,
    profile: RouteMobilityProfile,
    avoidVerifiedRisk: Boolean = false,
    avoidInaccessible: Boolean = false,
    extraPenalty: Double = 0.0
): Double {
    if (edge.isBlocked) return HARD_BLOCK_COST
    if (avoidVerifiedRisk && edge.riskWeight >= RISK_VERIFIED_THRESHOLD) return HARD_BLOCK_COST
    if (avoidInaccessible && edge.accessibilityPenalty >= 0.9) return HARD_BLOCK_COST

    val d = edge.distanceMeters
    val t = edge.distanceMeters / speedMetersPerSecondFor(profile)
    val r = edge.riskWeight * RISK_SCALE_METERS_EQUIVALENT
    val a = edge.accessibilityPenalty * ACCESSIBILITY_SCALE_METERS_EQUIVALENT

    val baseCost = weights.wDistance * d + weights.wTime * t + weights.wRisk * r + weights.wAccessibility * a
    return baseCost + extraPenalty
}