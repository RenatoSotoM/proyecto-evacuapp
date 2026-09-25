package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
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
 * Gestor de descarga asíncrona para grafos viales optimizados por anillos (30 km)
 * mediante ejecución secuencial estricta en startFullPreload, utilizando streaming directo
 * desde ResponseBody para prevenir errores OutOfMemory (OOM) en payloads grandes.
 */
object MapDownloadManager {

    private val KEY_MAP_DOWNLOADED = booleanPreferencesKey("MAP_DOWNLOADED")
    private val KEY_CENTER_LAT = doublePreferencesKey("CENTER_LAT")
    private val KEY_CENTER_LON = doublePreferencesKey("CENTER_LON")

    private const val MAP_FILE_NAME = "local_graph_30km.json"

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun getLocalMapFile(context: Context): File = File(context.filesDir, MAP_FILE_NAME)

    suspend fun isMapDownloaded(context: Context): Boolean = withContext(Dispatchers.IO) {
        val file = getLocalMapFile(context)
        val datastoreFlag = context.dataStore.data.map { prefs ->
            prefs[KEY_MAP_DOWNLOADED] ?: false
        }.first()
        file.exists() && file.length() > 0 && datastoreFlag
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
                    Log.d(TAG, "Grafo local de 30 km ya precargado en disco.")
                    _downloadState.value = DownloadState.Downloaded
                    return@withContext
                }
                startFullPreload(context, userLat, userLng)
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
            startFullPreload(context, userLat, userLng)
            return@withContext false
        }
    }

    private fun parseGraphResponseBody(body: okhttp3.ResponseBody, gson: Gson): MapGraphResponseDto {
        val rawStream = body.byteStream()
        val bufferedStream = java.io.BufferedInputStream(rawStream)

        // Detectar si los primeros 2 bytes corresponden a la cabecera GZIP
        bufferedStream.mark(2)
        val header = ByteArray(2)
        val read = bufferedStream.read(header, 0, 2)
        bufferedStream.reset()

        val inputStream: java.io.InputStream = if (read == 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()) {
            java.util.zip.GZIPInputStream(bufferedStream)
        } else {
            bufferedStream
        }

        // Streaming directo de lectura sin cargar el texto completo en RAM
        java.io.InputStreamReader(inputStream, Charsets.UTF_8).use { streamReader ->
            com.google.gson.stream.JsonReader(streamReader).use { jsonReader ->
                jsonReader.isLenient = true
                return gson.fromJson(jsonReader, MapGraphResponseDto::class.java)
            }
        }
    }

    /**
     * Bucle secuencial estricto en Dispatchers.IO para descargar Anillo 1, 2 y 3,
     * emitiendo progreso de la UI (5%, 35%, 70%, 100%), unificando y guardando en disco.
     */
    suspend fun startFullPreload(
        context: Context,
        lat: Double,
        lon: Double,
        travelMode: String = "vehicle",
        isReducedMobility: Boolean = false,
        avoidIncidents: Boolean = true
    ) = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(5)
        val api = RetrofitClient.mapGraphApiService
        val gson = Gson()

        try {
            Log.d("EVAC_DEBUG", "Iniciando Anillo 1...")
            val resp1 = api.getGraphRing(centerLat = lat, centerLon = lon, ringMin = 0, ringMax = 5000, travelMode = travelMode, isReducedMobility = isReducedMobility, avoidIncidents = avoidIncidents)
            val r1 = parseGraphResponseBody(resp1, gson)
            _downloadState.value = DownloadState.Downloading(35)

            Log.d("EVAC_DEBUG", "Anillo 1 OK. Iniciando Anillo 2...")
            val resp2 = api.getGraphRing(centerLat = lat, centerLon = lon, ringMin = 5000, ringMax = 15000, travelMode = travelMode, isReducedMobility = isReducedMobility, avoidIncidents = avoidIncidents)
            val r2 = parseGraphResponseBody(resp2, gson)
            _downloadState.value = DownloadState.Downloading(70)

            Log.d("EVAC_DEBUG", "Anillo 2 OK. Iniciando Anillo 3...")
            val resp3 = api.getGraphRing(centerLat = lat, centerLon = lon, ringMin = 15000, ringMax = 30000, travelMode = travelMode, isReducedMobility = isReducedMobility, avoidIncidents = avoidIncidents)
            val r3 = parseGraphResponseBody(resp3, gson)

            val unifiedNodes = (r1.nodes + r2.nodes + r3.nodes).distinctBy { it.id }
            val unifiedEdges = (r1.edges + r2.edges + r3.edges).distinctBy { it.id }
            val finalGraph = MapGraphResponseDto(nodes = unifiedNodes, edges = unifiedEdges)

            val file = getLocalMapFile(context)
            val jsonString = gson.toJson(finalGraph)
            FileWriter(file).use { it.write(jsonString) }

            context.dataStore.edit { prefs ->
                prefs[KEY_MAP_DOWNLOADED] = true
                prefs[KEY_CENTER_LAT] = lat
                prefs[KEY_CENTER_LON] = lon
            }

            _downloadState.value = DownloadState.Downloaded
            Log.d("EVAC_DEBUG", "GRAFO UNIFICADO GUARDADO EN DISCO OK!")
        } catch (e: Exception) {
            Log.e("EVAC_DEBUG", "Error en startFullPreload: ${e.message}", e)
            _downloadState.value = DownloadState.Error("Error al descargar grafo: ${e.message}")
        }
    }
}
