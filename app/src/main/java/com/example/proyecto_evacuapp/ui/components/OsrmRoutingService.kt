package com.example.proyecto_evacuapp.ui.components
// Trazado con perfil vehicular
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
     * Proveedor online de geometrías de rutas OSM.
     * Perfil temporalmente fijado a Vehículo (driving/car).
     */
    suspend fun fetchRealStreetRoute(
        start: GeoPoint,
        end: GeoPoint,
        profile: String = "Vehículo"
    ): OsrmRouteResponse = withContext(Dispatchers.IO) {

        // 🚗 Acepta variaciones con/sin tilde y deja "driving" como valor por defecto (else)
        val osrmProfile = when (profile.lowercase().trim()) {
            "vehiculo", "vehículo", "auto", "car", "driving" -> "driving"
            "bicicleta", "bike", "cycling" -> "cycling"
            "movilidad reducida", "a pie", "peaton", "peatón", "foot" -> "foot"
            else -> "driving" // 🔴 Fijado a vehículo ante cualquier caso no contemplado
        }

        /*
         * routing.openstreetmap.de publica servidores separados:
         * - routed-car  → perfil driving
         * - routed-bike → perfil cycling
         * - routed-foot → perfil foot
         */
        val serviceName = when (osrmProfile) {
            "driving" -> "car"
            "cycling" -> "bike"
            else -> "car" // 🔴 Servidor vehicular por defecto
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

            val formattedDuration = if (totalMinutes >= 60) {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                "${hours}h ${mins} min"
            } else {
                "$totalMinutes min"
            }

            OsrmRouteResponse(
                points = points,
                distanceText = distanceText,
                durationText = formattedDuration
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