package com.example.proyecto_evacuapp.ui.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.proyecto_evacuapp.domain.engine.DownloadState
import com.example.proyecto_evacuapp.domain.engine.MapDownloadManager
import com.example.proyecto_evacuapp.domain.engine.RoutingEngineManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

/**
 * Modelo de estado de sincronización explícito para la arquitectura offline-first de EvacuApp.
 */
sealed class SyncState {
    object OnlineSynced : SyncState()
    object Syncing : SyncState()
    data class OfflineLocal(val reason: String = "Operating autonomously on local graph") : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * ViewModel que conecta la inicialización asíncrona de cartografía dinámica basada en GPS,
 * el estado explícito de sincronización (SyncState) y la precarga del motor de ruteo offline con la UI.
 */
class RoutingViewModel : ViewModel() {

    val downloadState: StateFlow<DownloadState> = MapDownloadManager.downloadState

    private val _syncState = MutableStateFlow<SyncState>(SyncState.OnlineSynced)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isEngineReady = MutableStateFlow(false)
    val isEngineReady: StateFlow<Boolean> = _isEngineReady.asStateFlow()

    private val _calculatedRoute = MutableStateFlow<List<GeoPoint>>(emptyList())
    val calculatedRoute: StateFlow<List<GeoPoint>> = _calculatedRoute.asStateFlow()

    fun updateSyncState(newState: SyncState) {
        _syncState.value = newState
    }

    /**
     * Verificación inicial al abrir la aplicación utilizando la ubicación GPS actual y destino.
     */
    fun checkMapAndInitializeOnStartup(
        context: Context,
        userLat: Double = -33.4489,
        userLng: Double = -70.6693,
        destLat: Double? = null,
        destLng: Double? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                _syncState.value = SyncState.Syncing
                try {
                    MapDownloadManager.checkAndDownloadOnStartup(context, userLat, userLng, destLat, destLng)
                    val ready = RoutingEngineManager.initializeEngine(context)
                    _isEngineReady.value = ready
                    _syncState.value = SyncState.OnlineSynced
                } catch (e: Exception) {
                    _syncState.value = SyncState.OfflineLocal("Backend sync timeout or error: ${e.message}")
                }
            }
        }
    }

    /**
     * Acción exclusiva del botón "Precargar Mapa":
     * Ejecuta la secuencia completa de descarga de los 3 anillos (30km), unifica y persiste el JSON y centro en DataStore,
     * e inmediatamente carga el grafo en la memoria del RoadGraph para tenerlo listo en el motor local.
     */
    fun startFullPreload(context: Context, lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                _syncState.value = SyncState.Syncing
                try {
                    MapDownloadManager.startFullPreload(context, lat, lon)

                    // Carga inmediata del archivo JSON en la memoria del RoadGraph para el motor local
                    val ready = RoutingEngineManager.initializeEngine(context)
                    _isEngineReady.value = ready
                    _syncState.value = SyncState.OnlineSynced

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "✅ Precarga completa de 30 km finalizada y cargada en memoria.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    _syncState.value = SyncState.OfflineLocal("Preload failed, operating on local graph: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "⚠️ Modo autónomo activado por fallo de red.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Invocado al presionar el botón de precarga manual de la cartografía dinámica.
     */
    fun onManualDownloadClick(
        context: Context,
        userLat: Double = -33.4489,
        userLng: Double = -70.6693,
        destLat: Double? = null,
        destLng: Double? = null
    ) {
        val lat = destLat ?: userLat
        val lon = destLng ?: userLng
        startFullPreload(context, lat, lon)
    }

    /**
     * Solicita el cálculo de ruta offline sobre calles reales (cero líneas rectas).
     */
    fun calculateOfflineRoute(start: GeoPoint, end: GeoPoint) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                val points = RoutingEngineManager.calculateRoute(start, end)
                _calculatedRoute.value = points
            }
        }
    }
}
