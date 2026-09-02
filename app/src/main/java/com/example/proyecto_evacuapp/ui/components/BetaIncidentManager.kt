package com.example.proyecto_evacuapp.ui.components

object BetaIncidentManager {

    const val VERIFIED_THRESHOLD = 0.75

    fun confidence(alpha: Double, beta: Double): Double {
        val total = alpha + beta
        return if (total == 0.0) 0.0 else alpha / total
    }

    fun isVerified(alpha: Double, beta: Double): Boolean {
        return confidence(alpha, beta) >= VERIFIED_THRESHOLD
    }

    fun edgePenalty(incident: LocalIncident): Double {
        return when {
            incident.isVerified -> Double.POSITIVE_INFINITY
            incident.confidence >= 0.50 ->
                1.0 + incident.severity * incident.confidence * 3.0
            else -> 1.0
        }
    }
}