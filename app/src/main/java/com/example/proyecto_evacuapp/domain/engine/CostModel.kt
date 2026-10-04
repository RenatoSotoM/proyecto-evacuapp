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
 * Velocidad de desplazamiento (m/s) por modo de transporte.
 */
fun speedMetersPerSecondFor(profile: RouteMobilityProfile): Double = when (profile) {
    RouteMobilityProfile.VEHICLE -> 8.3            // ~30 km/h velocidad media vehicular
    RouteMobilityProfile.BICYCLE -> 4.2            // ~15 km/h bicicleta
    RouteMobilityProfile.WALKING -> 1.3            // ~4.7 km/h caminata peatonal
    RouteMobilityProfile.REDUCED_MOBILITY -> 1.0   // ~3.6 km/h movilidad reducida / silla de ruedas
}

private const val RISK_SCALE_METERS_EQUIVALENT = 600.0
private const val ACCESSIBILITY_SCALE_METERS_EQUIVALENT = 600.0

object CostProfiles {

    /** Pesos adaptativos según el perfil de movilidad seleccionado por el usuario. */
    fun weightsFor(profile: RouteMobilityProfile): CostWeights = when (profile) {
        RouteMobilityProfile.VEHICLE -> CostWeights(
            wDistance = 0.35, wTime = 0.40, wRisk = 0.20, wAccessibility = 0.05
        )
        RouteMobilityProfile.WALKING -> CostWeights(
            wDistance = 0.25, wTime = 0.25, wRisk = 0.30, wAccessibility = 0.20
        )
        RouteMobilityProfile.REDUCED_MOBILITY -> CostWeights(
            wDistance = 0.10, wTime = 0.10, wRisk = 0.25, wAccessibility = 0.55
        )
        RouteMobilityProfile.BICYCLE -> CostWeights(
            wDistance = 0.30, wTime = 0.35, wRisk = 0.25, wAccessibility = 0.10
        )
    }

    /** Pesos para la variante "Segura": prioriza fuertemente evitar riesgo. */
    val SAFE_WEIGHTS = CostWeights(wDistance = 0.15, wTime = 0.15, wRisk = 0.65, wAccessibility = 0.05)

    /** Pesos para la variante "Accesible": prioriza fuertemente minimizar A(e). */
    val ACCESSIBLE_WEIGHTS = CostWeights(wDistance = 0.15, wTime = 0.15, wRisk = 0.15, wAccessibility = 0.55)
}

/**
 * Ponderación adaptativa C(e) diferenciando perfiles Vehicular, Peatonal y Movilidad Reducida.
 */
fun edgeCost(
    edge: GraphEdge,
    weights: CostWeights,
    profile: RouteMobilityProfile,
    avoidVerifiedRisk: Boolean = false,
    avoidInaccessible: Boolean = false,
    extraPenalty: Double = 0.0
): Double {
    if (edge.isBlocked || edge.weight == Double.POSITIVE_INFINITY) return HARD_BLOCK_COST
    if (avoidVerifiedRisk && edge.riskWeight >= RISK_VERIFIED_THRESHOLD) return HARD_BLOCK_COST

    val hType = edge.highwayType.lowercase()

    // 1. RESTRICCIÓN DURA DE ACCESIBILIDAD (Movilidad Reducida / Sin Escaleras):
    if (profile == RouteMobilityProfile.REDUCED_MOBILITY || avoidInaccessible) {
        // Bloquear completamente escaleras, pendientes extremas y barreras físicas
        if (hType == "steps" || edge.accessibilityPenalty >= 0.8) {
            return HARD_BLOCK_COST
        }
    }

    // 2. RESTRICCIÓN DURA DE SEGURIDAD PEATONAL:
    if (profile == RouteMobilityProfile.WALKING || profile == RouteMobilityProfile.REDUCED_MOBILITY) {
        // Descartar autopistas y vías de alta velocidad sin aceras peatonales
        if (hType in setOf("motorway", "motorway_link", "trunk", "trunk_link")) {
            return HARD_BLOCK_COST
        }
    }

    // 3. PONDERACIÓN SEGÚN JERARQUÍA Y PERFIL DE MOVILIDAD
    val d = edge.distanceMeters
    val t = edge.distanceMeters / speedMetersPerSecondFor(profile)
    val r = edge.riskWeight * RISK_SCALE_METERS_EQUIVALENT
    val a = edge.accessibilityPenalty * ACCESSIBILITY_SCALE_METERS_EQUIVALENT

    var highwayAdjustment = 0.0
    when (profile) {
        RouteMobilityProfile.VEHICLE -> {
            highwayAdjustment = when (hType) {
                "motorway", "trunk", "primary" -> -0.20 * d
                "secondary", "tertiary" -> -0.10 * d
                "footway", "pedestrian", "steps", "path" -> HARD_BLOCK_COST // Vehículos no ingresan a zonas peatonales
                "service", "living_street" -> +0.35 * d
                else -> 0.0
            }
        }
        RouteMobilityProfile.WALKING -> {
            highwayAdjustment = when (hType) {
                "footway", "pedestrian", "path" -> -0.30 * d // Priorizar paseos peatonales y senderos
                "residential", "living_street" -> -0.15 * d
                "steps" -> +0.10 * d // Permitido para peatones convencionales
                "primary", "secondary" -> +0.25 * d // Evitar avenidas concurridas de alto tráfico
                else -> 0.0
            }
        }
        RouteMobilityProfile.REDUCED_MOBILITY -> {
            highwayAdjustment = when (hType) {
                "footway", "pedestrian", "path" -> -0.35 * d // Priorizar senderos planos y rampas
                "residential", "living_street" -> -0.20 * d
                else -> 0.0
            }
        }
        RouteMobilityProfile.BICYCLE -> {
            highwayAdjustment = when (hType) {
                "cycleway", "path" -> -0.30 * d
                "residential", "tertiary" -> -0.10 * d
                "steps" -> HARD_BLOCK_COST
                else -> 0.0
            }
        }
    }

    val baseCost = weights.wDistance * d + weights.wTime * t + weights.wRisk * r + weights.wAccessibility * a + highwayAdjustment
    return (baseCost + extraPenalty).coerceAtLeast(0.1)
}
