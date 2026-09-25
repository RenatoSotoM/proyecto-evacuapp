package com.example.proyecto_evacuapp.data.remote

import com.example.proyecto_evacuapp.domain.engine.calculateOfflineBoundingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GeocodingResult(
    val displayName: String,
    val latitude: Double,
    val longitude: Double
)

object GeocodingService {

    /**
     * Búsqueda de direcciones restringida estrictamente al Viewbox de 25 km (`bounded=1`),
     * impidiendo sugerencias que requieran cartografía no descargada fuera del radio local.
     */
    suspend fun searchAddress(query: String, userLat: Double? = null, userLon: Double? = null): List<GeocodingResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 3) return@withContext emptyList()
        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = buildString {
                append("https://nominatim.openstreetmap.org/search?q=")
                append(encodedQuery)
                append("&format=json&limit=5&addressdetails=1")
                if (userLat != null && userLon != null) {
                    val box = calculateOfflineBoundingBox(userLat, userLon, radiusKm = 25.0)
                    append("&viewbox=")
                    append(box.west)
                    append(",")
                    append(box.north)
                    append(",")
                    append(box.east)
                    append(",")
                    append(box.south)
                    append("&bounded=1") // Restringe búsquedas únicamente dentro de los 25 km
                }
            }

            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6_000
                readTimeout = 6_000
                setRequestProperty("User-Agent", "EvacuApp-UBO-StudentProject/1.0")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode !in 200..299) return@withContext emptyList()

            val text = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(text)
            val results = mutableListOf<GeocodingResult>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val name = item.optString("display_name", "")
                val lat = item.optDouble("lat", 0.0)
                val lon = item.optDouble("lon", 0.0)
                if (lat != 0.0 && lon != 0.0) {
                    results.add(GeocodingResult(name, lat, lon))
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }
}
