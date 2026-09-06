package com.example.proyecto_evacuapp.ui.state

import com.example.proyecto_evacuapp.data.local.IncidentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object IncidentSharedState {
    private val _incidents = MutableStateFlow<List<IncidentEntity>>(emptyList())
    val incidents: StateFlow<List<IncidentEntity>> = _incidents.asStateFlow()

    fun addIncident(incident: IncidentEntity) {
        _incidents.value = listOf(incident) + _incidents.value
    }

    fun setIncidents(list: List<IncidentEntity>) {
        _incidents.value = list
    }
}