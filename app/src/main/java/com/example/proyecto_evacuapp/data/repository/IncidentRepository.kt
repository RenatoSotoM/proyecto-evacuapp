package com.example.proyecto_evacuapp.data.repository

import com.example.proyecto_evacuapp.data.remote.IncidentApiService
import com.example.proyecto_evacuapp.data.remote.IncidentNetworkDto
import com.example.proyecto_evacuapp.data.remote.IncidentResponseDto
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.data.remote.VoteDto
import com.example.proyecto_evacuapp.ui.components.IncidentDao
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

class IncidentRepository(
    private val apiService: IncidentApiService? = null,
    private val incidentDao: IncidentDao? = null
) {
    constructor(apiService: IncidentApiService) : this(apiService, null)
    constructor(incidentDao: IncidentDao) : this(null, incidentDao)

    val allIncidents: Flow<List<IncidentEntity>>? = incidentDao?.observeAll()

    suspend fun upsertIncident(incident: IncidentEntity) {
        incidentDao?.upsert(incident)
    }

    suspend fun deleteIncident(localId: String) {
        incidentDao?.deleteByLocalId(localId)
    }

    suspend fun getIncidents(lat: Double? = null, lng: Double? = null): Response<List<IncidentResponseDto>> {
        val service = apiService ?: RetrofitClient.incidentApiService
        
        val primaryResponse = service.getIncidents()
        if (primaryResponse.isSuccessful) return primaryResponse

        if (primaryResponse.code() == 404) {
            val activeResponse = service.getActiveIncidents()
            if (activeResponse.isSuccessful) return activeResponse

            if (lat != null && lng != null) {
                val nearbyResponse = service.getNearbyIncidents(lat, lng)
                if (nearbyResponse.isSuccessful) return nearbyResponse
            }
        }

        return primaryResponse
    }

    suspend fun createIncident(incident: SharedIncident): Response<IncidentResponseDto> {
        val service = apiService ?: RetrofitClient.incidentApiService
        val dto = IncidentNetworkDto(
            type = incident.type.apiValue,
            severity = incident.severity.apiValue,
            description = incident.description,
            latitude = incident.latitude,
            longitude = incident.longitude
        )
        return service.createIncident(dto)
    }

    suspend fun voteIncident(remoteId: String, vote: String): Response<IncidentResponseDto> {
        val service = apiService ?: RetrofitClient.incidentApiService
        return service.voteIncident(remoteId, VoteDto(vote))
    }
}
