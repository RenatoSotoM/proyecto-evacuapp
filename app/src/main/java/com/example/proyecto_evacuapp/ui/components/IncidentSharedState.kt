package com.example.proyecto_evacuapp.ui.components

import androidx.compose.runtime.mutableStateListOf

object IncidentSharedState {

    private val incidentList = mutableStateListOf<SharedIncident>()

    val incidents: List<SharedIncident>
        get() = incidentList

    fun addLocalIncident(incident: SharedIncident) {
        incidentList.removeAll { it.localId == incident.localId }
        incidentList.add(0, incident)
    }

    fun replaceIncident(updatedIncident: SharedIncident) {
        val index = incidentList.indexOfFirst {
            it.localId == updatedIncident.localId
        }

        if (index >= 0) {
            incidentList[index] = updatedIncident
        } else {
            incidentList.add(0, updatedIncident)
        }
    }

    fun confirmIncident(localId: String) {
        updateVote(localId = localId, isConfirmation = true)
    }

    fun rejectIncident(localId: String) {
        updateVote(localId = localId, isConfirmation = false)
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

        val incident = incidentList[index]

        val alpha = if (isConfirmation) {
            incident.alpha + 1.0
        } else {
            incident.alpha
        }

        val beta = if (isConfirmation) {
            incident.beta
        } else {
            incident.beta + 1.0
        }

        val confidence = alpha / (alpha + beta)

        val nextStatus = when {
            confidence >= 0.75 -> IncidentStatus.VERIFIED
            confidence >= 0.50 -> IncidentStatus.PROBABLE
            else -> IncidentStatus.PENDING
        }

        incidentList[index] = incident.copy(
            alpha = alpha,
            beta = beta,
            status = nextStatus,
            updatedAtMillis = System.currentTimeMillis()
        )
    }
}