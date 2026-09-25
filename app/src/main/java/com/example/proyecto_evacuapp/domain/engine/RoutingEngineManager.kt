package com.example.proyecto_evacuapp.domain.engine

import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.File

private const val TAG = "RoutingEngineManager"

/**
 * Gestor del motor de ruteo offline vectorial por calles reales.
 * ELIMINACIÓN TOTAL DEL FALLBACK DE LÍNEA RECTA: si no hay un camino conectado entre origen y destino,
 * devuelve emptyList() y muestra el aviso de "Ruta no encontrada sobre la red vial".
 */
object RoutingEngineManager {

    private val mutex = Mutex()
    private var isEngineLoaded = false

    suspend fun initializeEngine(context: Context, mapFile: File? = null): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        mutex.withLock {
            if (isEngineLoaded) return@withContext true

            try {
                val cacheDir = File(context.filesDir, "gh-cache")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                LocalRouteEngine.initialize(context)

                val origin = RouteCoordinate(-33.4489, -70.6693)
                val destination = RouteCoordinate(-33.4500, -70.6700)

                val repository = RoadNetworkRepository()
                repository.ensureLoadedForRoute(context, origin, destination)

                isEngineLoaded = true
                Log.d(TAG, "Motor de ruteo offline vectorial inicializado correctamente.")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar el motor de ruteo: ${e.message}", e)
                return@withContext false
            }
        }
    }

    /**
     * Consulta ruta vectorial sobre calles reales.
     * Si el grafo no encuentra un camino conectado entre las calles de origen y destino,
     * retorna emptyList() y muestra el aviso de "Ruta no encontrada sobre la red vial".
     */
    suspend fun calculateRoute(
        start: GeoPoint,
        end: GeoPoint,
        avoidPoints: List<GeoPoint> = emptyList()
    ): List<GeoPoint> = withContext(Dispatchers.IO) {
        if (!isEngineLoaded) {
            Log.w(TAG, "El motor no ha sido inicializado. Ruta no encontrada sobre la red vial.")
            return@withContext emptyList()
        }

        try {
            val startCoord = RouteCoordinate(start.latitude, start.longitude)
            val endCoord = RouteCoordinate(end.latitude, end.longitude)

            val alternatives = LocalRouteEngine.calculateRouteAlternatives(
                origin = startCoord,
                destination = endCoord,
                profile = com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile.WALKING
            )

            val primaryPath = alternatives.firstOrNull()?.points ?: emptyList()

            // ELIMINACIÓN TOTAL DEL FALLBACK DE LÍNEA RECTA: si está vacío o tiene <= 2 puntos (línea recta directa), retorna emptyList().
            if (primaryPath.isEmpty() || primaryPath.size <= 2) {
                Log.w(TAG, "⚠️ Ruta no encontrada sobre la red vial (grafo desconectado o sin ruta válida).")
                return@withContext emptyList()
            }

            primaryPath.map { GeoPoint(it.latitude, it.longitude) }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante el cálculo de ruta vectorial: ${e.message}. Ruta no encontrada sobre la red vial.")
            emptyList()
        }
    }

    fun isReady(): Boolean = isEngineLoaded
}
