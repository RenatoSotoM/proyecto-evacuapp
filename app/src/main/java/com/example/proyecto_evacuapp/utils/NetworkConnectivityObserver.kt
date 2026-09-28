package com.example.proyecto_evacuapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.proyecto_evacuapp.data.remote.IncidentSyncWorker
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.ui.viewmodel.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sensor de conectividad inteligente y orquestador de Store-and-Forward (Sincronización Diferida).
 * Observa cambios de red mediante ConnectivityManager y ejecuta pings de salud (Health Check).
 * Ante la reconexión, vacía la cola local enviando reportes pendientes mediante IncidentSyncWorker.
 */
class NetworkConnectivityObserver(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startObserving(onStateChanged: (SyncState) -> Unit) {
        if (networkCallback != null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("EVAC_DEBUG", "NetworkObserver: Red detectada (Wi-Fi/Celular). Verificando ping de salud...")
                _isNetworkAvailable.value = true
                scope.launch {
                    val healthy = performHealthCheck()
                    if (healthy) {
                        Log.d("EVAC_DEBUG", "NetworkObserver: Health check OK -> ONLINE_SYNCED. Vaciando cola de reportes offline (Store-and-Forward)...")
                        onStateChanged(SyncState.OnlineSynced)
                        triggerDeferredStoreAndForwardSync()
                    } else {
                        Log.w("EVAC_DEBUG", "NetworkObserver: Health check FAILED -> OFFLINE_LOCAL.")
                        onStateChanged(SyncState.OfflineLocal("Falló la comprobación de salud con el servidor"))
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.w("EVAC_DEBUG", "NetworkObserver: Red perdida -> OFFLINE_LOCAL.")
                _isNetworkAvailable.value = false
                onStateChanged(SyncState.OfflineLocal("Sin conexión a la red"))
            }
        }

        networkCallback = callback
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e("EVAC_DEBUG", "Error registrando callback de red: ${e.message}", e)
        }
    }

    fun stopObserving() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.w("EVAC_DEBUG", "Error desregistrando callback de red: ${e.message}")
            }
            networkCallback = null
        }
    }

    suspend fun performHealthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.userApiService.getMe()
            response.isSuccessful || response.code() != 404
        } catch (e: Exception) {
            Log.w("EVAC_DEBUG", "Excepción en ping de salud: ${e.message}")
            false
        }
    }

    fun triggerDeferredStoreAndForwardSync() {
        try {
            val syncWorkRequest = OneTimeWorkRequestBuilder<IncidentSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncWorkRequest)
            Log.d("EVAC_DEBUG", "Sincronización diferida (Store-and-Forward) encolada con WorkManager.")
        } catch (e: Exception) {
            Log.e("EVAC_DEBUG", "Error encolando IncidentSyncWorker: ${e.message}", e)
        }
    }
}
