package com.example.proyecto_evacuapp.data.repository

import com.example.proyecto_evacuapp.data.remote.IncidentApiService
import com.example.proyecto_evacuapp.data.remote.IncidentNetworkDto
import com.example.proyecto_evacuapp.data.remote.IncidentResponseDto
import com.example.proyecto_evacuapp.data.remote.VoteIncidentNetworkDto
import com.example.proyecto_evacuapp.data.remote.IncidentVoteResponseDto
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import retrofit2.Response

class IncidentRepository(
    private val apiService: IncidentApiService
) {
    suspend fun createIncident(
        incident: SharedIncident
    ): Response<IncidentResponseDto> {
        val dto = IncidentNetworkDto(
            type = incident.type.name,
            severity = incident.severity.toNetworkValue(),
            description = incident.description
                .trim()
                .takeIf { it.isNotBlank() },
            latitude = incident.latitude,
            longitude = incident.longitude
        )

        return apiService.createIncident(dto)
    }

    suspend fun getIncidents(): Response<List<IncidentResponseDto>> {
        return apiService.getIncidents()
    }

    suspend fun voteIncident(
        remoteId: String,
        vote: String
    ): Response<IncidentVoteResponseDto> {
        return apiService.voteIncident(
            incidentId = remoteId,
            dto = VoteIncidentNetworkDto(vote = vote)
        )
    }
}

private fun IncidentSeverity.toNetworkValue(): String {
    return when (this) {
        IncidentSeverity.BAJA -> "LOW"
        IncidentSeverity.MEDIA -> "MEDIUM"
        IncidentSeverity.ALTA -> "HIGH"
        IncidentSeverity.CRITICA -> "CRITICAL"
    }
}