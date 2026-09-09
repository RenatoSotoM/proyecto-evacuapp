package com.example.proyecto_evacuapp.data.remote

import android.content.Context
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

            android.util.Log.d("SyncWorker", "Iniciando sincronización local con el backend...")

            val localIncidents = dao.observeAll().first()

            for (incident in localIncidents) {
                // Sincronizar solo los que aún no tienen ID remoto
                if (incident.remoteId == null) {
                    val dto = IncidentNetworkDto(
                        type = incident.type,
                        severity = incident.severity,
                        description = incident.description,
                        latitude = incident.latitude,
                        longitude = incident.longitude
                    )

                    val response = RetrofitClient.apiService.createIncident(dto)
                    if (response.isSuccessful && response.body() != null) {
                        val serverData = response.body()!!
                        // Actualizar el registro local con el ID remoto devuelto
                        val updated = incident.copy(remoteId = serverData.id)
                        dao.upsert(updated)
                        android.util.Log.d("SyncWorker", "Incidente sincronizado con éxito. ID Remoto: ${serverData.id}")
                    } else {
                        android.util.Log.e("SyncWorker", "Error del servidor: ${response.errorBody()?.string()}")
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Fallo de red en sincronización: ${e.localizedMessage}", e)
            Result.retry()
        }
    }
}