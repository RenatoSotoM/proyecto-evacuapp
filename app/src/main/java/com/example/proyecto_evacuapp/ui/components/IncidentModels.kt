package com.example.proyecto_evacuapp.ui.components

import java.util.UUID

enum class IncidentType(val emoji: String, val displayName: String, val apiValue: String) {
    BLOQUEO_VIAL("🚧", "Calle bloqueada", "BLOQUEO_VIAL"),
    INCENDIO("🔥", "Incendio", "INCENDIO"),
    INUNDACION("🌊", "Inundación", "INUNDACION"),
    DERRUMBE("⚠️", "Derrumbe", "ESCOMBROS"),
    ACCIDENTE("🚗", "Accidente vehicular", "ACCIDENTE"),
    RUTA_INACCESIBLE("🚫", "Ruta inaccesible", "RUTA_INACCESIBLE"),
    OTRO("⚠️", "Otro peligro", "PELIGRO_GENERAL");

    companion object {
        fun fromApiValue(value: String?): IncidentType {
            if (value.isNullOrBlank()) return OTRO
            return entries.find {
                it.apiValue.equals(value, ignoreCase = true) ||
                it.name.equals(value, ignoreCase = true)
            } ?: when (value.uppercase()) {
                "ESCOMBROS" -> DERRUMBE
                "PELIGRO_GENERAL" -> OTRO
                else -> OTRO
            }
        }
    }
}

enum class IncidentSeverity(val apiValue: String) {
    BAJA("LOW"),
    MEDIA("MEDIUM"),
    ALTA("HIGH"),
    CRITICA("CRITICAL");

    companion object {
        fun fromApiValue(value: String?): IncidentSeverity {
            if (value.isNullOrBlank()) return MEDIA
            return entries.find {
                it.apiValue.equals(value, ignoreCase = true) ||
                it.name.equals(value, ignoreCase = true)
            } ?: when (value.uppercase()) {
                "LOW" -> BAJA
                "MEDIUM" -> MEDIA
                "HIGH" -> ALTA
                "CRITICAL" -> CRITICA
                else -> MEDIA
            }
        }
    }
}

enum class IncidentStatus {
    LOCAL_PENDING,
    PENDING,
    PROBABLE,
    VERIFIED,
    REJECTED,
    SYNC_FAILED
}

enum class IncidentSyncActionType {
    CREATE,
    CONFIRM,
    REJECT
}

data class SharedIncident(
    val localId: String = UUID.randomUUID().toString(),
    val remoteId: String? = null,
    val type: IncidentType,
    val severity: IncidentSeverity,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val alpha: Double = 1.0,
    val beta: Double = 1.0,
    val status: IncidentStatus = IncidentStatus.LOCAL_PENDING,
    val affectedSegmentIds: Set<String> = emptySet(),
    val isOwnReport: Boolean = true
) {
    val confidence: Double
        get() {
            val total = alpha + beta
            return if (total <= 0.0) 0.0 else alpha / total
        }

    val confidencePercentage: Int
        get() = (confidence * 100).toInt()

    val isVerified: Boolean
        get() = status == IncidentStatus.VERIFIED || confidence >= 0.75
}

data class PendingIncidentSyncAction(
    val id: String = UUID.randomUUID().toString(),
    val incidentLocalId: String,
    val actionType: IncidentSyncActionType,
    val createdAtMillis: Long = System.currentTimeMillis()
)
