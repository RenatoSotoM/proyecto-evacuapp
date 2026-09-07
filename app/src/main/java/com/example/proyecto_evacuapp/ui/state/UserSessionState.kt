package com.example.proyecto_evacuapp.ui.state

import com.example.proyecto_evacuapp.domain.model.TransportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSessionState {
    private val _selectedTransportMode = MutableStateFlow(TransportMode.VEHICULO)
    val selectedTransportMode: StateFlow<TransportMode> = _selectedTransportMode.asStateFlow()

    private val _hasReducedMobility = MutableStateFlow(false)
    val hasReducedMobility: StateFlow<Boolean> = _hasReducedMobility.asStateFlow()

    fun updateTransportMode(mode: TransportMode) {
        _selectedTransportMode.value = mode
    }

    fun updateReducedMobility(hasReduced: Boolean) {
        _hasReducedMobility.value = hasReduced
    }
}