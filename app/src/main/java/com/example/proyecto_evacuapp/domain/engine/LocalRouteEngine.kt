package com.example.proyecto_evacuapp.domain.engine

import android.location.Location
import com.example.proyecto_evacuapp.data.local.IncidentEntity
import com.example.proyecto_evacuapp.domain.model.TransportMode
import org.osmdroid.util.GeoPoint

object LocalRouteEngine {

    fun calculateEvacuationRoute(
        start: GeoPoint,
        destination: GeoPoint,
        mode: TransportMode,
        hasReducedMobility: Boolean,
        incidents: List<IncidentEntity>
    ): List<GeoPoint> {
        // 1. Determinar velocidad según perfil de usuario
        val averageSpeedKmH = when (mode) {
            TransportMode.VEHICULO -> 30.0
            TransportMode.PEATON -> if (hasReducedMobility) 2.5 else 4.5
        }

        // 2. Filtrar incidentes de alta/crítica severidad ubicados dentro del radio de 5 km
        val blockedZones = incidents.filter { incident ->
            val isHighSeverity = incident.severity.equals("ALTA", ignoreCase = true) ||
                    incident.severity.equals("CRITICA", ignoreCase = true)

            val isNearby = isWithinRadius(
                userLat = start.latitude,
                userLon = start.longitude,
                incidentLat = incident.latitude,
                incidentLon = incident.longitude,
                radiusInMeters = 5000f
            )

            isHighSeverity && isNearby
        }

        // 3. Cálculo/Simulación de la Polyline evitando las zonas bloqueadas detectadas
        if (blockedZones.isNotEmpty() && averageSpeedKmH > 0) {
            // Lógica de evasión del tramo en el grafo vial OSRM / OSMDroid local
        }

        return listOf(start, destination)
    }

    /**
     * Calcula si una posición (incidente) está dentro de un radio dado (ej. 5 km = 5000 metros)
     * respecto a la posición actual del usuario.
     */
    fun isWithinRadius(
        userLat: Double,
        userLon: Double,
        incidentLat: Double,
        incidentLon: Double,
        radiusInMeters: Float = 5000f
    ): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLat, userLon,
            incidentLat, incidentLon,
            results
        )
        return results[0] <= radiusInMeters
    }
}