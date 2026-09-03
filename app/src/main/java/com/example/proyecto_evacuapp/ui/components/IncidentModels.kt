package com.example.proyecto_evacuapp.ui.components

import java.util.UUID

enum class IncidentType {
    BLOQUEO_VIAL,
    INCENDIO,
    INUNDACION,
    DERRUMBE,
    ACCIDENTE,
    RUTA_INACCESIBLE,
    OTRO
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

    val isVerified: Boolean
        get() = status == IncidentStatus.VERIFIED || confidence >= 0.75
}

data class PendingIncidentSyncAction(
    val id: String = UUID.randomUUID().toString(),
    val incidentLocalId: String,
    val actionType: IncidentSyncActionType,
    val createdAtMillis: Long = System.currentTimeMillis()
)