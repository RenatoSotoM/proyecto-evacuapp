package com.example.proyecto_evacuapp.data.repository

import com.example.proyecto_evacuapp.ui.components.IncidentDao
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import kotlinx.coroutines.flow.Flow

class IncidentRepository(private val incidentDao: IncidentDao) {

    val allIncidents: Flow<List<IncidentEntity>> = incidentDao.observeAll()

    suspend fun upsertIncident(incident: IncidentEntity) {
        incidentDao.upsert(incident)
    }

    suspend fun deleteIncident(localId: String) {
        incidentDao.deleteByLocalId(localId)
    }
}