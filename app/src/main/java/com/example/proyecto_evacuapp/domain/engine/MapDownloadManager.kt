package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.example.proyecto_evacuapp.data.remote.GraphEdgeDto
import com.example.proyecto_evacuapp.data.remote.GraphNodeDto
import com.example.proyecto_evacuapp.data.remote.MapGraphResponseDto
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

private const val TAG = "MapDownloadManager"
private val Context.dataStore by preferencesDataStore(name = "map_download_prefs")

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Downloaded : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * Gestor de descarga asíncrona optimizado por anillos de cobertura.
 * Por defecto abarca hasta el Anillo 2 (5-15 km) en inicio para evitar consumo excesivo de RAM y ANRs.
 * El Anillo 3 (15-30 km) queda disponible de forma diferida/bajo demanda.
 */
object MapDownloadManager {

    private val KEY_MAP_DOWNLOADED = booleanPreferencesKey("MAP_DOWNLOADED")
    private val KEY_RING3_DOWNLOADED = booleanPreferencesKey("RING3_DOWNLOADED")
    private val KEY_CENTER_LAT = doublePreferencesKey("CENTER_LAT")
    private val KEY_CENTER_LON = doublePreferencesKey("CENTER_LON")

    private const val MAP_FILE_NAME_15KM = "local_graph_15km.json"
    private const val MAP_FILE_NAME_30KM = "local_graph_30km.json"

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun getLocalMapFile(context: Context): File {
        val file30 = File(context.filesDir, MAP_FILE_NAME_30KM)
        val file15 = File(context.filesDir, MAP_FILE_NAME_15KM)
        return if (file30.exists() && file30.length() > 0) file30 else file15
    }

    suspend fun isMapDownloaded(context: Context): Boolean = withContext(Dispatchers.IO) {
        val file = getLocalMapFile(context)
        val datastoreFlag = context.dataStore.data.map { prefs ->
            prefs[KEY_MAP_DOWNLOADED] ?: false
        }.first()
        file.exists() && file.length() > 0 && datastoreFlag
    }

    suspend fun isRing3Downloaded(context: Context): Boolean = withContext(Dispatchers.IO) {
        val file30 = File(context.filesDir, MAP_FILE_NAME_30KM)
        val ring3Flag = context.dataStore.data.map { prefs ->
            prefs[KEY_RING3_DOWNLOADED] ?: false
        }.first()
        file30.exists() && file30.length() > 0 && ring3Flag
    }

    suspend fun checkAndDownloadOnStartup(
        context: Context,
        userLat: Double,
        userLng: Double,
        destLat: Double? = null,
        destLng: Double? = null
    ) {
        withContext(Dispatchers.IO) {
            withContext(NonCancellable) {
                if (isMapDownloaded(context)) {
                    Log.d(TAG, "Grafo local (Anillo 2 / 15 km) ya precargado en disco.")
                    _downloadState.value = DownloadState.Downloaded
                    return@withContext
                }
                // Carga liviana por defecto hasta Anillo 2 (0-15 km)
                startDefaultPreload(context, userLat, userLng)
            }
        }
    }

    suspend fun triggerManualDownload(
        context: Context,
        userLat: Double,
        userLng: Double,
        destLat: Double? = null,
        destLng: Double? = null
    ): Boolean = withContext(Dispatchers.IO) {
        withContext(NonCancellable) {
            if (isMapDownloaded(context)) {
                _downloadState.value = DownloadState.Downloaded
                return@withContext true
            }
            startDefaultPreload(context, userLat, userLng)
            return@withContext false
        }
    }

    private fun parseGraphResponseBody(body: okhttp3.ResponseBody, gson: Gson): MapGraphResponseDto {
        val rawStream = body.byteStream()
        val bufferedStream = java.io.BufferedInputStream(rawStream)

        bufferedStream.mark(2)
        val header = ByteArray(2)
        val read = bufferedStream.read(header, 0, 2)
        bufferedStream.reset()

        val inputStream: java.io.InputStream = if (read == 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) {
            java.util.zip.GZIPInputStream(bufferedStream)
        } else {
            bufferedStream
        }

        java.io.InputStreamReader(inputStream, Charsets.UTF_8).use { streamReader ->
            com.google.gson.stream.JsonReader(streamReader).use { jsonReader ->
                jsonReader.isLenient = true
                return gson.fromJson(jsonReader, MapGraphResponseDto::class.java)
            }
        }
    }

    /**
     * Carga por defecto liviana hasta Anillo 2 (0 - 15 km) para arranque optimizado sin ANRs.
     */
    suspend fun startDefaultPreload(
        context: Context,
        lat: Double,
        lon: Double,
        travelMode: String = "vehicle",
        isReducedMobility: Boolean = false,
        avoidIncidents: Boolean = true
    ) = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(10)
        val api = RetrofitClient.mapGraphApiService
        val gson = Gson()

        try {
            Log.d("EVAC_DEBUG", "Iniciando descarga por defecto Anillo 1 (0-5 km)...")
            val resp1 = api.getGraphRing(centerLat = lat, centerLon = lon, ringMin = 0, ringMax = 5000, travelMode = travelMode, isReducedMobility = isReducedMobility, avoidIncidents = avoidIncidents)
            val r1 = parseGraphResponseBody(resp1, gson)
            _downloadState.value = DownloadState.Downloading(50)

            Log.d("EVAC_DEBUG", "Anillo 1 OK. Iniciando descarga Anillo 2 (5-15 km)...")
            val resp2 = api.getGraphRing(centerLat = lat, centerLon = lon, ringMin = 5000, ringMax = 15000, travelMode = travelMode, isReducedMobility = isReducedMobility, avoidIncidents = avoidIncidents)
            val r2 = parseGraphResponseBody(resp2, gson)
            _downloadState.value = DownloadState.Downloading(90)

            val unifiedNodes = (r1.nodes + r2.nodes).distinctBy { it.id }
            val unifiedEdges = (r1.edges + r2.edges).distinctBy { it.id }
            val finalGraph = MapGraphResponseDto(nodes = unifiedNodes, edges = unifiedEdges)

            val file15 = File(context.filesDir, MAP_FILE_NAME_15KM)
            val jsonString = gson.toJson(finalGraph)
            FileWriter(file15).use { it.write(jsonString) }

            context.dataStore.edit { prefs ->
                prefs[KEY_MAP_DOWNLOADED] = true
                prefs[KEY_CENTER_LAT] = lat
                prefs[KEY_CENTER_LON] = lon
            }

            _downloadState.value = DownloadState.Downloaded
            Log.d("EVAC_DEBUG", "GRAFO HASTA ANILLO 2 (15 KM) GUARDADO EN DISCO OK!")
        } catch (e: Exception) {
            Log.e("EVAC_DEBUG", "Error en startDefaultPreload: ${e.message}", e)
            _downloadState.value = DownloadState.Error("Error al descargar grafo: ${e.message}")
        }
    }

    /**
     * Carga bajo demanda / diferida del Anillo 3 (15 - 30 km) para escenarios extendidos.
     */
    suspend fun downloadExtendedRing3(
        context: Context,
        lat: Double,
        lon: Double,
        travelMode: String = "vehicle",
        isReducedMobility: Boolean = false,
        avoidIncidents: Boolean = true
    ) = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(10)
        val api = RetrofitClient.mapGraphApiService
        val gson = Gson()

        try {
            // Asegurar Anillos 1 y 2
            startDefaultPreload(context, lat, lon, travelMode, isReducedMobility, avoidIncidents)
            _downloadState.value = DownloadState.Downloading(70)

            Log.d("EVAC_DEBUG", "Iniciando carga extendida bajo demanda Anillo 3 (15-30 km)...")
            val resp3 = api.getGraphRing(centerLat = lat, centerLon = lon, ringMin = 15000, ringMax = 30000, travelMode = travelMode, isReducedMobility = isReducedMobility, avoidIncidents = avoidIncidents)
            val r3 = parseGraphResponseBody(resp3, gson)

            val existingFile = getLocalMapFile(context)
            var baseNodes = emptyList<GraphNodeDto>()
            var baseEdges = emptyList<GraphEdgeDto>()

            if (existingFile.exists() && existingFile.length() > 0) {
                try {
                    val baseGraph = java.io.FileReader(existingFile).use { reader ->
                        gson.fromJson(reader, MapGraphResponseDto::class.java)
                    }
                    baseNodes = baseGraph?.nodes ?: emptyList()
                    baseEdges = baseGraph?.edges ?: emptyList()
                } catch (e: Exception) {
                    Log.w("EVAC_DEBUG", "No se pudo leer grafo base previo: ${e.message}")
                }
            }

            val unifiedNodes = (baseNodes + r3.nodes).distinctBy { it.id }
            val unifiedEdges = (baseEdges + r3.edges).distinctBy { it.id }
            val fullGraph = MapGraphResponseDto(nodes = unifiedNodes, edges = unifiedEdges)

            val file30 = File(context.filesDir, MAP_FILE_NAME_30KM)
            val jsonString = gson.toJson(fullGraph)
            FileWriter(file30).use { it.write(jsonString) }

            context.dataStore.edit { prefs ->
                prefs[KEY_MAP_DOWNLOADED] = true
                prefs[KEY_RING3_DOWNLOADED] = true
                prefs[KEY_CENTER_LAT] = lat
                prefs[KEY_CENTER_LON] = lon
            }

            _downloadState.value = DownloadState.Downloaded
            Log.d("EVAC_DEBUG", "ANILLO 3 EXTENDIDO (30 KM) GUARDADO EN DISCO OK!")
        } catch (e: Exception) {
            Log.e("EVAC_DEBUG", "Error en downloadExtendedRing3: ${e.message}", e)
            _downloadState.value = DownloadState.Error("Error descargando Anillo 3: ${e.message}")
        }
    }

    suspend fun startFullPreload(
        context: Context,
        lat: Double,
        lon: Double
    ) = downloadExtendedRing3(context, lat, lon)
}
