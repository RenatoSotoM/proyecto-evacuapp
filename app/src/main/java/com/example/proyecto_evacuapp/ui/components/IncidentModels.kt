package com.example.proyecto_evacuapp.ui.components

import java.util.UUID

enum class IncidentType(val emoji: String, val displayName: String) {
    BLOQUEO_VIAL("🚧", "Calle bloqueada"),
    INCENDIO("🔥", "Incendio"),
    INUNDACION("🌊", "Inundación"),
    DERRUMBE("⚠️", "Derrumbe"),
    ACCIDENTE("🚗", "Accidente vehicular"),
    RUTA_INACCESIBLE("🚫", "Ruta inaccesible"),
    OTRO("⚠️", "Otro peligro")
}

enum class IncidentSeverity {
    BAJA,
    MEDIA,
    ALTA,
    CRITICA
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