package com.example.proyecto_evacuapp.data.repository

import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.data.remote.IncidentApiService
import com.example.proyecto_evacuapp.data.remote.IncidentNetworkDto
import com.example.proyecto_evacuapp.data.remote.IncidentResponseDto
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.data.remote.VoteDto
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.IncidentDao
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
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

    /**
     * Consulta incidentes activos mediante GET /api/v1/incidents/nearby, el endpoint oficial documentado del backend.
     */
    suspend fun getIncidents(lat: Double? = null, lng: Double? = null): Response<List<IncidentResponseDto>> {
        val service = apiService ?: RetrofitClient.incidentApiService
        val targetLat = lat ?: -33.5919
        val targetLng = lng ?: -70.7060
        return service.getNearbyIncidents(lat = targetLat, lng = targetLng, radius = 30000.0)
    }

    /**
     * Sincronización de creación remota: cuando Retrofit recibe 201 Created con el ID oficial de PostgreSQL,
     * imprime logs de diagnóstico de seguridad, extrae el officialRemoteId y reemplaza en Room DB y memoria.
     */
    suspend fun createIncident(incident: SharedIncident, context: Context? = null): Response<IncidentResponseDto> {
        val service = apiService ?: RetrofitClient.incidentApiService
        val dto = IncidentNetworkDto(
            type = incident.type.apiValue,
            severity = incident.severity.apiValue,
            description = incident.description,
            latitude = incident.latitude,
            longitude = incident.longitude
        )

        Log.d("EVAC_DIAG", "[CREATE_INCIDENT] Enviando POST /api/v1/incidents | LocalID: ${incident.localId} | Tipo: ${dto.type} | Lat/Lng: (${dto.latitude}, ${dto.longitude})")

        val response = service.createIncident(dto)

        Log.d("EVAC_DIAG", "[CREATE_INCIDENT] Respuesta HTTP ${response.code()} | Raw: $response")

        if (response.isSuccessful && response.body() != null) {
            val serverData = response.body()!!
            val officialRemoteId = serverData.id

            if (!officialRemoteId.isNullOrBlank()) {
                Log.d("EVAC_DIAG", "[CREATE_INCIDENT] EXITO HTTP 201 | LocalID: ${incident.localId} -> OfficialServerID: $officialRemoteId | Status: ${serverData.status}")

                val dao = incidentDao ?: (context?.let { EvacuAppDatabase.getInstance(it).incidentDao() })
                if (dao != null) {
                    dao.updateRemoteId(incident.localId, officialRemoteId, serverData.status ?: "PENDING")
                }

                IncidentSharedState.markAsSynced(
                    localId = incident.localId,
                    remoteId = officialRemoteId,
                    status = serverData.status ?: "PENDING"
                )
            } else {
                Log.e("EVAC_DIAG", "[CREATE_INCIDENT] ERROR: HTTP 201 recibido pero el cuerpo JSON no contiene un ID oficial válido.")
            }
        } else {
            Log.e("EVAC_DIAG", "[CREATE_INCIDENT] ERROR HTTP ${response.code()} | Body: ${response.errorBody()?.string()}")
        }

        return response
    }

    /**
     * Resuelve estrictamente el ID oficial de PostgreSQL (remoteId) antes de invocar /api/v1/incidents/{id}/vote.
     * Si targetId es un UUID local, consulta memoria y Room DB para obtener el remoteId oficial.
     */
    suspend fun voteIncident(targetId: String, vote: String, context: Context? = null): Response<IncidentResponseDto> {
        val service = apiService ?: RetrofitClient.incidentApiService

        val sharedMatch = IncidentSharedState.incidents.find { it.localId == targetId || it.remoteId == targetId }
        val sharedRemoteId = sharedMatch?.remoteId

        val dao = incidentDao ?: (context?.let { EvacuAppDatabase.getInstance(it).incidentDao() })
        val dbEntity = if (dao != null) dao.getByLocalId(targetId) else null
        val dbRemoteId = dbEntity?.remoteId

        val officialRemoteId = when {
            !sharedRemoteId.isNullOrBlank() -> sharedRemoteId
            !dbRemoteId.isNullOrBlank() -> dbRemoteId
            else -> targetId
        }

        Log.d("EVAC_DIAG", "[VOTE_INCIDENT] Solicitado targetId=$targetId -> Resuelto OfficialServerID=$officialRemoteId | VoteType=$vote")
        return service.voteIncident(officialRemoteId, VoteDto(vote))
    }
}
