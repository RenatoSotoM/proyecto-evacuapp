package com.example.proyecto_evacuapp.ui.components

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object IncidentSharedState {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val incidentList = mutableStateListOf<SharedIncident>()

    val incidents: List<SharedIncident>
        get() = incidentList

    private var initialized = false
    private lateinit var incidentDao: IncidentDao

    fun initialize(database: EvacuAppDatabase) {
        if (initialized) return

        initialized = true
        incidentDao = database.incidentDao()

        scope.launch {
            incidentDao.observeAll().collectLatest { entities ->
                val restoredIncidents = entities.map { entity ->
                    entity.toSharedIncident()
                }

                incidentList.clear()
                incidentList.addAll(restoredIncidents)
            }
        }
    }

    fun addLocalIncident(incident: SharedIncident) {
        val localIncident = incident.copy(
            status = IncidentStatus.LOCAL_PENDING,
            updatedAtMillis = System.currentTimeMillis()
        )

        replaceInMemory(localIncident)
        persist(localIncident)
    }

    fun confirmIncident(localId: String) {
        updateVote(
            localId = localId,
            isConfirmation = true
        )
    }

    fun rejectIncident(localId: String) {
        updateVote(
            localId = localId,
            isConfirmation = false
        )
    }

    fun verifiedBlockedSegmentIds(): Set<String> {
        return incidentList
            .filter { it.isVerified }
            .flatMap { it.affectedSegmentIds }
            .toSet()
    }

    private fun updateVote(
        localId: String,
        isConfirmation: Boolean
    ) {
        val index = incidentList.indexOfFirst {
            it.localId == localId
        }

        if (index < 0) return

        val current = incidentList[index]

        val newAlpha = if (isConfirmation) {
            current.alpha + 1.0
        } else {
            current.alpha
        }

        val newBeta = if (isConfirmation) {
            current.beta
        } else {
            current.beta + 1.0
        }

        val confidence = newAlpha / (newAlpha + newBeta)

        val newStatus = when {
            confidence >= 0.75 -> IncidentStatus.VERIFIED
            confidence >= 0.50 -> IncidentStatus.PROBABLE
            else -> IncidentStatus.PENDING
        }

        val updatedIncident = current.copy(
            alpha = newAlpha,
            beta = newBeta,
            status = newStatus,
            updatedAtMillis = System.currentTimeMillis()
        )

        replaceInMemory(updatedIncident)
        persist(updatedIncident)
    }

    private fun replaceInMemory(incident: SharedIncident) {
        val index = incidentList.indexOfFirst {
            it.localId == incident.localId
        }

        if (index >= 0) {
            incidentList[index] = incident
        } else {
            incidentList.add(0, incident)
        }
    }

    private fun persist(incident: SharedIncident) {
        if (!::incidentDao.isInitialized) return

        scope.launch {
            incidentDao.upsert(
                incident.toEntity()
            )
        }
    }
}

private fun SharedIncident.toEntity(): IncidentEntity {
    return IncidentEntity(
        localId = localId,
        remoteId = remoteId,
        type = type.name,
        severity = severity.name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        alpha = alpha,
        beta = beta,
        status = status.name,
        affectedSegmentIds = affectedSegmentIds.joinToString(","),
        isOwnReport = isOwnReport
    )
}

private fun IncidentEntity.toSharedIncident(): SharedIncident {
    return SharedIncident(
        localId = localId,
        remoteId = remoteId,
        type = IncidentType.valueOf(type),
        severity = IncidentSeverity.valueOf(severity),
        description = description,
        latitude = latitude,
        longitude = longitude,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        alpha = alpha,
        beta = beta,
        status = IncidentStatus.valueOf(status),
        affectedSegmentIds = affectedSegmentIds
            .split(",")
            .filter { it.isNotBlank() }
            .toSet(),
        isOwnReport = isOwnReport
    )
}