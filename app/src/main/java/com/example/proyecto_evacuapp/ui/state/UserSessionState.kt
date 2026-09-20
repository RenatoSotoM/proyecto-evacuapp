package com.example.proyecto_evacuapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.proyecto_evacuapp.data.remote.UserMeResponse

object UserSessionState {
    // 💡 'by mutableStateOf' permite que Compose reaccione a los cambios de sesión
    var currentUser: UserProfile by mutableStateOf(UserProfile())

    fun updateFromUserMeResponse(response: UserMeResponse) {
        val mp = response.mobilityProfile
        currentUser = UserProfile(
            id = response.id,
            name = response.name,
            email = response.email,
            role = response.role,
            isLoggedIn = true,
            mobilityType = mp?.mobilityType ?: "VEHICULO",
            requiresAccessibleRoute = mp?.requiresAccessibleRoute ?: false,
            travelsWithMinors = mp?.travelsWithMinors ?: false,
            companionCount = mp?.companionCount ?: 0,
            transportMode = TransportMode.entries.find { it.backendValue == (mp?.mobilityType ?: "VEHICULO") } ?: TransportMode.VEHICLE,
            companions = mp?.companionDescription ?: "Solo",
            locationZone = "San Bernardo, Santiago"
        )
    }

    fun clear() {
        currentUser = UserProfile()
    }
}