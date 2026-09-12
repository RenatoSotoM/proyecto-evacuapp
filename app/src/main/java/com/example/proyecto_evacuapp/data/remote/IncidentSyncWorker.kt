package com.example.proyecto_evacuapp.data.remote

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
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

            val localIncidents = dao.observeAll().first()

            for (incident in localIncidents) {
                if (incident.remoteId == null) {
                    val dto = IncidentNetworkDto(
                        type = incident.type,
                        severity = incident.severity,
                        description = incident.description,
                        latitude = incident.latitude,
                        longitude = incident.longitude
                    )

                    val response = RetrofitClient.incidentApiService.createIncident(dto)

                    if (response.isSuccessful) {
                        val serverData: IncidentResponseDto? = response.body()
                        if (serverData != null) {
                            val updated = incident.copy(remoteId = serverData.id)
                            dao.upsert(updated)
                            Log.d("SyncWorker", "Incidente sincronizado con éxito. ID Remoto NestJS: ${serverData.id}")
                        } else {
                            Log.w("SyncWorker", "Respuesta HTTP exitosa pero body nulo.")
                        }
                    } else {
                        Log.e("SyncWorker", "Error del servidor (${response.code()}): ${response.errorBody()?.string()}")
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Fallo de red al sincronizar: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}