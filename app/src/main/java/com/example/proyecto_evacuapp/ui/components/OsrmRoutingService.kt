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
    val modifier: String,
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
            String.format(java.util.Locale.US, "%.1f km", distanceInMeters / 1000.0)
        } else {
            "${distanceInMeters.toInt()} m"
        }
    }

    suspend fun fetchRealStreetRoute(
        start: GeoPoint,
        end: GeoPoint,
        profile: String = "Vehículo",
        bearing: Float? = null,
        speedMps: Double? = null,
        avoidPoints: List<GeoPoint> = emptyList()
    ): OsrmRouteResponse {
        val alternatives = fetchRealStreetRouteAlternatives(start, end, profile, bearing, speedMps)
        return alternatives.firstOrNull() ?: OsrmRouteResponse(emptyList(), "--", "--")
    }

    suspend fun fetchRealStreetRouteAlternatives(
        start: GeoPoint,
        end: GeoPoint,
        profile: String = "Vehículo",
        bearing: Float? = null,
        speedMps: Double? = null
    ): List<OsrmRouteResponse> = withContext(Dispatchers.IO) {

        val osrmProfile = when (profile.lowercase().trim()) {
            "vehiculo", "vehículo", "auto", "car", "driving" -> "driving"
            "bicicleta", "bike", "cycling" -> "cycling"
            "movilidad reducida", "a pie", "peaton", "peatón", "foot" -> "foot"
            else -> "driving"
        }

        val serviceName = when (osrmProfile) {
            "driving" -> "car"
            "cycling" -> "bike"
            "foot" -> "foot"
            else -> "car"
        }

        val applyBearings = bearing != null && bearing >= 0 && osrmProfile == "driving" && (speedMps == null || speedMps > 1.0)

        fun buildUrl(includeBearings: Boolean): String = buildString {
            append("https://routing.openstreetmap.de/routed-")
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
            append("?overview=full&geometries=geojson&steps=true&alternatives=true")

            if (includeBearings && bearing != null) {
                val b = bearing.toInt().coerceIn(0, 359)
                append("&bearings=").append(b).append(",45;&radiuses=15;")
            }
        }

        var results = executeOsrmQueryMulti(buildUrl(applyBearings))
        if (results.isEmpty() && applyBearings) {
            results = executeOsrmQueryMulti(buildUrl(false))
        }

        results
    }

    private fun executeOsrmQueryMulti(urlString: String): List<OsrmRouteResponse> {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "EvacuApp-UBO-StudentProject/1.0")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode !in 200..299) return emptyList()

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val routesArr = json.optJSONArray("routes") ?: return emptyList()

            val parsedRoutes = mutableListOf<OsrmRouteResponse>()
            for (r in 0 until routesArr.length()) {
                val routeObj = routesArr.getJSONObject(r)
                val distanceMeters = routeObj.getDouble("distance")
                val durationSeconds = routeObj.getDouble("duration")

                val coordinates = routeObj.getJSONObject("geometry").getJSONArray("coordinates")
                val points = buildList {
                    for (index in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(index)
                        add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                    }
                }

                val stepsList = mutableListOf<StepInstruction>()
                val legs = routeObj.optJSONArray("legs")
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

                val distanceText = formatDistance(distanceMeters)
                val totalMinutes = ceil(durationSeconds / 60.0).toInt().coerceAtLeast(1)
                val durationText = if (totalMinutes >= 60) {
                    "${totalMinutes / 60}h ${totalMinutes % 60} min"
                } else {
                    "$totalMinutes min"
                }

                parsedRoutes.add(
                    OsrmRouteResponse(
                        points = points,
                        distanceText = distanceText,
                        durationText = durationText,
                        steps = stepsList
                    )
                )
            }
            parsedRoutes
        } catch (e: Exception) {
            emptyList()
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

    suspend fun fetchRealStreetRouteWithAvoidance(
        start: GeoPoint,
        end: GeoPoint,
        avoidPoints: List<GeoPoint>,
        profile: String,
        bearing: Float? = null,
        speedMps: Double? = null
    ) = fetchRealStreetRoute(start, end, profile, bearing, speedMps, avoidPoints)
}
