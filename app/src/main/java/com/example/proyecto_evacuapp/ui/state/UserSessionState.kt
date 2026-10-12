package com.example.proyecto_evacuapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.proyecto_evacuapp.data.remote.UserMeResponse

object UserSessionState {
    var currentUser: UserProfile by mutableStateOf(UserProfile())

    fun updateFromUserMeResponse(response: UserMeResponse?) {
        if (response == null) {
            clear()
            return
        }
        val mp = response.mobilityProfile
        currentUser = UserProfile(
            id = response.id ?: "",
            name = response.name ?: "Usuario EvacuApp",
            email = response.email ?: "",
            role = response.role ?: "USER",
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
