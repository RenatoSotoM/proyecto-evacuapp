package com.example.proyecto_evacuapp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.osmdroid.util.GeoPoint

enum class TransportMode(val label: String) {
    VEHICLE("Vehículo"),
    WALKING("A pie"),
    BICYCLE("Bicicleta"),
    REDUCED_MOBILITY("Movilidad reducida")
}

data class UserProfile(
    val name: String = "José Cataldo / Renato Soto",
    val email: String = "contacto@evacuapp.cl",
    val locationZone: String = "San Bernardo, Santiago",
    val transportMode: TransportMode = TransportMode.WALKING,
    val companions: String = "Adultos · Niños",
    val isLoggedIn: Boolean = true
)

object UserSessionState {
    var currentUser by mutableStateOf(UserProfile())
}

data class IncidenceReport(
    val id: String,
    val title: String,
    val description: String,
    val severity: String, // "Alto", "Medio", "Bajo"
    val location: GeoPoint,
    val timestamp: Long = System.currentTimeMillis(),
    var verificationCount: Int = 1 // Comienza con 1 (creador)
)

object IncidentRepository {
    val activeReports = mutableStateListOf<IncidenceReport>()

    fun addReport(report: IncidenceReport) {
        activeReports.add(report)
    }

    fun verifyReport(reportId: String) {
        val index = activeReports.indexOfFirst { it.id == reportId }
        if (index != -1) {
            val current = activeReports[index]
            activeReports[index] = current.copy(verificationCount = current.verificationCount + 1)
        }
    }
}