package com.example.proyecto_evacuapp.ui.components

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.ceil

data class OsrmRouteResponse(
    val points: List<GeoPoint>,
    val distanceText: String,
    val durationText: String,
    val errorMessage: String? = null
)

object OsrmRoutingService {

    /**
     * Proveedor temporal online de geometrías reales de rutas OSM.
     *
     * La respuesta contiene puntos de geometry.coordinates en formato GeoJSON:
     * [longitud, latitud]. Estos puntos siguen la geometría de calles/caminos.
     *
     * No genera rutas artificiales de respaldo. Si no existe red o falla el
     * proveedor, devuelve una lista vacía para evitar diagonales engañosas.
     *
     * En la versión offline-first final, este componente debe reemplazarse
     * internamente por BRouter/GraphHopper local o por el grafo precargado.
     */
    suspend fun fetchRealStreetRoute(
        start: GeoPoint,
        end: GeoPoint,
        profile: String
    ): OsrmRouteResponse = withContext(Dispatchers.IO) {
        val osrmProfile = when (profile) {
            "Vehículo" -> "driving"
            "Bicicleta" -> "cycling"
            "Movilidad reducida" -> "foot"
            else -> "foot"
        }

        /*
         * routing.openstreetmap.de publica servidores separados:
         *
         * - routed-car     → perfil driving
         * - routed-bike    → perfil cycling
         * - routed-foot    → perfil foot
         *
         * No basta con cambiar el servidor: el perfil tras /v1 también
         * debe ser el perfil que ese servidor reconoce.
         */
        val serviceName = when (osrmProfile) {
            "driving" -> "car"
            "cycling" -> "bike"
            else -> "foot"
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
                setRequestProperty(
                    "User-Agent",
                    "EvacuApp-UBO-StudentProject/1.0"
                )
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                return@withContext OsrmRouteResponse(
                    points = emptyList(),
                    distanceText = "--",
                    durationText = "--",
                    errorMessage = "OSRM respondió HTTP $responseCode. $errorBody"
                )
            }

            val responseText = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val json = JSONObject(responseText)
            val routes = json.optJSONArray("routes")

            if (routes == null || routes.length() == 0) {
                return@withContext OsrmRouteResponse(
                    points = emptyList(),
                    distanceText = "--",
                    durationText = "--",
                    errorMessage = "OSRM no devolvió rutas para este origen y destino."
                )
            }

            val primaryRoute = routes.getJSONObject(0)
            val distanceMeters = primaryRoute.getDouble("distance")
            val durationSeconds = primaryRoute.getDouble("duration")

            val coordinates = primaryRoute
                .getJSONObject("geometry")
                .getJSONArray("coordinates")

            val points = buildList {
                for (index in 0 until coordinates.length()) {
                    val coordinate = coordinates.getJSONArray(index)

                    val longitude = coordinate.getDouble(0)
                    val latitude = coordinate.getDouble(1)

                    add(GeoPoint(latitude, longitude))
                }
            }

            if (points.size < 2) {
                return@withContext OsrmRouteResponse(
                    points = emptyList(),
                    distanceText = "--",
                    durationText = "--",
                    errorMessage = "OSRM devolvió una geometría incompleta."
                )
            }

            val distanceText = if (distanceMeters >= 1_000.0) {
                String.format("%.1f km", distanceMeters / 1_000.0)
            } else {
                "${distanceMeters.toInt()} m"
            }

            val totalMinutes = ceil(durationSeconds / 60.0)
                .toInt()
                .coerceAtLeast(1)

            OsrmRouteResponse(
                points = points,
                distanceText = distanceText,
                durationText = "$totalMinutes min"
            )
        } catch (exception: Exception) {
            OsrmRouteResponse(
                points = emptyList(),
                distanceText = "--",
                durationText = "--",
                errorMessage = exception.message
                    ?: "No fue posible consultar la ruta por calles reales."
            )
        } finally {
            connection?.disconnect()
        }
    }
}