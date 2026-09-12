package com.example.proyecto_evacuapp.data

object UserSessionState {
    var currentUser: UserProfile = UserProfile()
        private set

    fun updateFromUserMeResponse(response: com.example.proyecto_evacuapp.data.remote.UserMeResponse) {
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
            locationZone = "San Bernardo, Santiago" // TODO: guardar en prefs si el usuario lo cambia
        )
    }

    fun clear() {
        currentUser = UserProfile()
    }
}