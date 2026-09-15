package com.example.proyecto_evacuapp.data.repository

import com.example.proyecto_evacuapp.data.remote.EmergenciesApiService
import com.example.proyecto_evacuapp.domain.model.Emergency
import org.osmdroid.util.GeoPoint

class EmergencyRepository(
    private val apiService: EmergenciesApiService
) {
    suspend fun getActiveEmergency(): Emergency? {
        val response = apiService.getActiveEmergency()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.active != false && body.id != null) {
                // Convertir GeoJSON [lng, lat] a puntos de Osmdroid (GeoPoint)
                val points = body.affectedArea?.coordinates?.firstOrNull()?.map { coord ->
                    GeoPoint(coord[1], coord[0])
                } ?: emptyList()

                return Emergency(
                    id = body.id,
                    type = body.type ?: "OTHER",
                    status = body.status ?: "ACTIVE",
                    title = body.title ?: "Emergencia Activa",
                    description = body.description,
                    affectedAreaPoints = points
                )
            }
        }
        return null
    }
}