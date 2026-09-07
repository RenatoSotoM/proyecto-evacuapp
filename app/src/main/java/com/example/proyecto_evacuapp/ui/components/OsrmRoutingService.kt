package com.example.proyecto_evacuapp.ui.components

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.ceil

data class StepInstruction(
    val text: String,
    val modifier: String, // "left", "right", "straight", "uturn", "arrive", etc.
    val location: GeoPoint,
    val distanceMeters: Double
)

data class OsrmRouteResponse(
    val points: List<GeoPoint>,
    val distanceText: String,
    val durationText: String,
    val steps: List<StepInstruction> = emptyList(),
    val errorMessage: String? = null
)

object OsrmRoutingService {

    fun formatDistance(distanceInMeters: Double): String {
        return if (distanceInMeters >= 1000) {
            // Si es mayor a 1 km, mostrar en km (ejemplo: 2.5 km)
            String.format(java.util.Locale.US, "%.1f km", distanceInMeters / 1000.0)
        } else {
            // Si es menor a 1 km, mostrar en metros (ejemplo: 267 m)
            "${distanceInMeters.toInt()} m"
        }
    }

    suspend fun fetchRealStreetRoute(
        start: GeoPoint,
        end: GeoPoint,
        profile: String = "Vehículo"
    ): OsrmRouteResponse = withContext(Dispatchers.IO) {

        val osrmProfile = when (profile.lowercase().trim()) {
            "vehiculo", "vehículo", "auto", "car", "driving" -> "driving"
            "bicicleta", "bike", "cycling" -> "cycling"
            "movilidad reducida", "a pie", "peaton", "peatón", "foot" -> "foot"
            else -> "driving"
        }

        // CORRECCIÓN 1: Soporte correcto para el servidor de peatón ("foot")
        val serviceName = when (osrmProfile) {
            "driving" -> "car"
            "cycling" -> "bike"
            "foot" -> "foot"
            else -> "car"
        }

        val urlString = buildString {
            append("https://routing.openstreetmap.de/")
            append("routed-")
            append(serviceName)
            append("/route/v1/")
            append(osrmProfile)
            append("/")
            append(start.longitude)
            append(",")
            append(start.latitude)
            append(";")
            append(end.longitude)
            append(",")
            append(end.latitude)
            append("?overview=full")
            append("&geometries=geojson")
            append("&steps=true")
            append("&alternatives=false")
        }

        var connection: HttpURLConnection? = null

        try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "EvacuApp-UBO-StudentProject/1.0")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                return@withContext OsrmRouteResponse(
                    points = emptyList(),
                    distanceText = "--",
                    durationText = "--",
                    errorMessage = "OSRM respondió HTTP $responseCode"
                )
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val routes = json.optJSONArray("routes")

            if (routes == null || routes.length() == 0) {
                return@withContext OsrmRouteResponse(
                    points = emptyList(),
                    distanceText = "--",
                    durationText = "--",
                    errorMessage = "OSRM no devolvió rutas."
                )
            }

            val primaryRoute = routes.getJSONObject(0)
            val distanceMeters = primaryRoute.getDouble("distance")
            val durationSeconds = primaryRoute.getDouble("duration")

            // Geometría completa
            val coordinates = primaryRoute.getJSONObject("geometry").getJSONArray("coordinates")
            val points = buildList {
                for (index in 0 until coordinates.length()) {
                    val coord = coordinates.getJSONArray(index)
                    add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                }
            }

            // Extracción de giros / pasos (Steps)
            val stepsList = mutableListOf<StepInstruction>()
            val legs = primaryRoute.optJSONArray("legs")
            if (legs != null && legs.length() > 0) {
                val stepsArr = legs.getJSONObject(0).optJSONArray("steps")
                if (stepsArr != null) {
                    for (i in 0 until stepsArr.length()) {
                        val stepObj = stepsArr.getJSONObject(i)
                        val stepDistance = stepObj.optDouble("distance", 0.0)
                        val streetName = stepObj.optString("name", "")
                        val maneuver = stepObj.optJSONObject("maneuver")

                        val type = maneuver?.optString("type", "") ?: ""
                        var modifier = maneuver?.optString("modifier", "") ?: ""
                        if (modifier.isEmpty()) modifier = type

                        val locArr = maneuver?.optJSONArray("location")
                        val stepPoint = if (locArr != null && locArr.length() >= 2) {
                            GeoPoint(locArr.getDouble(1), locArr.getDouble(0))
                        } else {
                            GeoPoint(0.0, 0.0)
                        }

                        val instructionText = buildInstruction(type, modifier, streetName)

                        stepsList.add(
                            StepInstruction(
                                text = instructionText,
                                modifier = modifier,
                                location = stepPoint,
                                distanceMeters = stepDistance
                            )
                        )
                    }
                }
            }

            // CORRECCIÓN 2: Reutilizar formatDistance para mantener uniformidad (metros vs km)
            val distanceText = formatDistance(distanceMeters)

            val totalMinutes = ceil(durationSeconds / 60.0).toInt().coerceAtLeast(1)
            val durationText = if (totalMinutes >= 60) {
                "${totalMinutes / 60}h ${totalMinutes % 60} min"
            } else {
                "$totalMinutes min"
            }

            OsrmRouteResponse(
                points = points,
                distanceText = distanceText,
                durationText = durationText,
                steps = stepsList
            )
        } catch (exception: Exception) {
            OsrmRouteResponse(
                points = emptyList(),
                distanceText = "--",
                durationText = "--",
                errorMessage = exception.message
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildInstruction(type: String, modifier: String, streetName: String): String {
        val street = if (streetName.isNotBlank()) "por $streetName" else ""
        return when (type) {
            "depart" -> "Inicia el recorrido $street".trim()
            "arrive" -> "Has llegado a tu destino"
            "turn" -> when (modifier) {
                "left", "slight left", "sharp left" -> "Gira a la izquierda $street".trim()
                "right", "slight right", "sharp right" -> "Gira a la derecha $street".trim()
                "straight" -> "Continúa recto $street".trim()
                "uturn" -> "Gira en U $street".trim()
                else -> "Gira $street".trim()
            }

            "new name", "continue" -> "Sigue recto $street".trim()
            "roundabout", "rotary" -> "En la rotonda toma la salida $street".trim()
            else -> "Continúa $street".trim()
        }
    }


// ... al final de OsrmRoutingService.kt ...

    suspend fun fetchRealStreetRouteWithAvoidance(
        start: GeoPoint,
        end: GeoPoint,
        avoidPoints: List<GeoPoint>,
        profile: String
    ) = fetchRealStreetRoute(start, end, profile)
}