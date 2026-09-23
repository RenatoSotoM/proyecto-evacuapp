package com.example.proyecto_evacuapp.ui.state

import com.example.proyecto_evacuapp.domain.model.Emergency

sealed interface EmergencyUiState {
    object Idle : EmergencyUiState
    object Loading : EmergencyUiState
    data class Active(val emergency: Emergency) : EmergencyUiState
    object NoActive : EmergencyUiState
    data class Error(val message: String) : EmergencyUiState
}