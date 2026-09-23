package com.example.proyecto_evacuapp.ui.components

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.example.proyecto_evacuapp.data.remote.IncidentResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

                withContext(Dispatchers.Main.immediate) {
                    incidentList.clear()
                    incidentList.addAll(restoredIncidents)
                }
            }
        }
    }

    fun syncRemoteIncidents(remoteList: List<IncidentResponseDto>) {
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                remoteList.forEach { dto ->
                    val type = IncidentType.fromApiValue(dto.type)
                    val severity = IncidentSeverity.fromApiValue(dto.severity)
                    val status = try { IncidentStatus.valueOf(dto.status) } catch (_: Exception) { IncidentStatus.PENDING }

                    val existingIndex = incidentList.indexOfFirst {
                        it.remoteId == dto.id || (it.localId.isNotBlank() && it.latitude == dto.latitude && it.longitude == dto.longitude)
                    }
                    if (existingIndex >= 0 && existingIndex < incidentList.size) {
                        val existing = incidentList[existingIndex]
                        val updated = existing.copy(
                            remoteId = dto.id,
                            type = type,
                            severity = severity,
                            description = dto.description ?: "",
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            alpha = dto.alpha ?: existing.alpha,
                            beta = dto.beta ?: existing.beta,
                            status = status,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                        replaceInMemory(updated)
                        persist(updated)
                    } else {
                        val newIncident = SharedIncident(
                            remoteId = dto.id,
                            type = type,
                            severity = severity,
                            description = dto.description ?: "",
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            alpha = dto.alpha ?: 1.0,
                            beta = dto.beta ?: 1.0,
                            status = status,
                            isOwnReport = false
                        )
                        replaceInMemory(newIncident)
                        persist(newIncident)
                    }
                }
            }
        }
    }

    fun markAsSynced(localId: String, remoteId: String?, status: String) {
        scope.launch(Dispatchers.Main.immediate) {
            val index = incidentList.indexOfFirst { it.localId == localId }
            if (index >= 0 && index < incidentList.size) {
                val current = incidentList[index]
                val parsedStatus = try { IncidentStatus.valueOf(status) } catch (_: Exception) { IncidentStatus.PENDING }
                val updated = current.copy(
                    remoteId = remoteId ?: current.remoteId,
                    status = parsedStatus,
                    updatedAtMillis = System.currentTimeMillis()
                )
                replaceInMemory(updated)
                persist(updated)
            }
        }
    }

    fun updateVoteFromRemote(localId: String, alpha: Double, beta: Double, status: String) {
        scope.launch(Dispatchers.Main.immediate) {
            val index = incidentList.indexOfFirst { it.localId == localId || it.remoteId == localId }
            if (index >= 0 && index < incidentList.size) {
                val current = incidentList[index]
                val parsedStatus = try { IncidentStatus.valueOf(status) } catch (_: Exception) { IncidentStatus.PENDING }
                val updated = current.copy(
                    alpha = alpha,
                    beta = beta,
                    status = parsedStatus,
                    updatedAtMillis = System.currentTimeMillis()
                )
                replaceInMemory(updated)
                persist(updated)
            }
        }
    }

    fun addLocalIncident(incident: SharedIncident) {
        val localIncident = incident.copy(
            status = IncidentStatus.LOCAL_PENDING,
            updatedAtMillis = System.currentTimeMillis()
        )

        scope.launch(Dispatchers.Main.immediate) {
            replaceInMemory(localIncident)
            persist(localIncident)
        }
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
        scope.launch(Dispatchers.Main.immediate) {
            val index = incidentList.indexOfFirst {
                it.localId == localId
            }

            if (index < 0 || index >= incidentList.size) return@launch

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
    }

    private fun replaceInMemory(incident: SharedIncident) {
        val index = incidentList.indexOfFirst {
            it.localId == incident.localId || (incident.remoteId != null && it.remoteId == incident.remoteId)
        }

        if (index >= 0 && index < incidentList.size) {
            incidentList[index] = incident
        } else {
            incidentList.add(0, incident)
        }
    }

    private fun persist(incident: SharedIncident) {
        if (!::incidentDao.isInitialized) return

        scope.launch(Dispatchers.IO) {
            incidentDao.upsert(
                incident.toEntity()
            )
        }
    }

    fun triggerSync(context: Context) {
        IncidentSyncService.scheduleSync(context)
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
        type = IncidentType.fromApiValue(type),
        severity = IncidentSeverity.fromApiValue(severity),
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
