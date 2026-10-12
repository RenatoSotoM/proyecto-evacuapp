package com.example.proyecto_evacuapp.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentType
import kotlinx.coroutines.flow.first

class IncidentSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = EvacuAppDatabase.getInstance(applicationContext)
            val dao = database.incidentDao()

            Log.d("SyncWorker", "Iniciando sincronización de incidentes local -> NestJS...")

            val syncStartMs = System.currentTimeMillis()
            val localIncidents = dao.observeAll().first()
            val now = System.currentTimeMillis()
            val maxOfflineReportAgeMs = 24 * 60 * 60 * 1000L // 24 horas máximo de caducidad para reportes offline

            var syncedCount = 0

            for (incident in localIncidents) {
                if (incident.remoteId == null) {
                    val ageMs = now - incident.createdAtMillis
                    if (ageMs > maxOfflineReportAgeMs) {
                        Log.w("SyncWorker", "Reporte local caducado (creado hace ${ageMs / 60000} min). Eliminando de Room DB para evitar re-subir datos obsoletos...")
                        dao.deleteByLocalId(incident.localId)
                        continue
                    }

                    val typeEnum = IncidentType.fromApiValue(incident.type)
                    val severityEnum = IncidentSeverity.fromApiValue(incident.severity)

                    val dto = IncidentNetworkDto(
                        type = typeEnum.apiValue,
                        severity = severityEnum.apiValue,
                        description = incident.description,
                        latitude = incident.latitude,
                        longitude = incident.longitude
                    )

                    val response = RetrofitClient.incidentApiService.createIncident(dto)
                    Log.d("DEBUG_ID", "Respuesta cruda del servidor (SyncWorker): $response")

                    if (response.isSuccessful) {
                        val serverData: IncidentResponseDto? = response.body()
                        val officialId = serverData?.id
                        val officialStatus = serverData?.status ?: "PENDING"
                        if (!officialId.isNullOrBlank()) {
                            Log.d("INCIDENT_MAPPING", "ID LOCAL: ${incident.localId} -> ID OFICIAL RECIBIDO DEL SERVIDOR: $officialId")
                            
                            // Reemplazo explícito de remoteId en Room DB
                            dao.updateRemoteId(incident.localId, officialId, officialStatus)

                            IncidentSharedState.markAsSynced(
                                localId = incident.localId,
                                remoteId = officialId,
                                status = officialStatus
                            )
                            syncedCount++
                            Log.d("SyncWorker", "Incidente sincronizado con éxito. ID Remoto NestJS: $officialId")
                        } else {
                            Log.w("SyncWorker", "Respuesta HTTP exitosa pero body nulo o ID oficial ausente.")
                        }
                    } else {
                        Log.e("SyncWorker", "Error del servidor (${response.code()}): ${response.errorBody()?.string()}")
                    }
                }
            }

            val syncMs = System.currentTimeMillis() - syncStartMs
            Log.d("EVAC_METRIC", "[STORE_FORWARD] Status: ONLINE_SYNCED | SyncedCount: $syncedCount | SyncDuration: ${syncMs}ms")

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Fallo de red al sincronizar: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}
