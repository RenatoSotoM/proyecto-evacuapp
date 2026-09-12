package com.example.proyecto_evacuapp.data

data class UserProfile(
    val id: String = "",
    val name: String = "Usuario Invitado",
    val email: String = "",
    val role: String = "",
    val isLoggedIn: Boolean = false,
    // Movilidad (backend: MobilityProfile)
    val mobilityType: String = "VEHICULO",           // PEATON, VEHICULO, BICICLETA, PERSONA_MOVILIDAD_REDUCIDA
    val requiresAccessibleRoute: Boolean = false,
    val travelsWithMinors: Boolean = false,
    val companionCount: Int = 0,
    // Legacy (para compatibilidad UI antigua)
    val transportMode: TransportMode = TransportMode.VEHICLE,
    val companions: String = "Solo",
    val locationZone: String = "San Bernardo, Santiago"
) {
    val hasCompanions: Boolean = companionCount > 0 || travelsWithMinors
    val companionDescription: String = when {
        companionCount > 0 && travelsWithMinors -> "$companionCount acompañantes + menores"
        companionCount > 0 -> "$companionCount acompañantes"
        travelsWithMinors -> "Menores a cargo"
        else -> "Solo"
    }
}

enum class TransportMode(val label: String, val backendValue: String) {
    VEHICLE("Vehículo", "VEHICULO"),
    WALKING("A pie", "PEATON"),
    BICYCLE("Bicicleta", "BICICLETA"),
    REDUCED_MOBILITY("Movilidad reducida", "PERSONA_MOVILIDAD_REDUCIDA")
}