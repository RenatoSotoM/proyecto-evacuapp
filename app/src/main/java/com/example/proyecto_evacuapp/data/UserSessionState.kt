package com.example.proyecto_evacuapp.data

import com.example.proyecto_evacuapp.data.remote.UserMeResponse

object UserSessionState {
    // SETTER PÚBLICO para que las Screens puedan hacer: UserSessionState.currentUser = ...
    var currentUser: UserProfile = UserProfile()

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