package com.example.proyecto_evacuapp.comm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gestor central de comunicaciones offline de producción de EvacuApp.
 * Orquesta la difusión de tramas SMS compactas y paquetes JSON vía Bluetooth RFCOMM/BLE
 * y persiste automáticamente los reportes recibidos en Room DB.
 */
object EmergencyCommManager {
    private const val TAG = "EmergencyCommManager"

    fun initialize(context: Context, scope: CoroutineScope) {
        try {
            BluetoothCommHandler.startBluetoothServer(context, scope)
            Log.d(TAG, "Módulo de Comunicación Offline (SMS & Bluetooth RFCOMM) inicializado.")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando EmergencyCommManager: ${e.message}", e)
        }
    }

    fun broadcastAlertOffline(
        context: Context,
        scope: CoroutineScope,
        type: String,
        description: String,
        latitude: Double,
        longitude: Double,
        destinationSmsNumber: String? = null,
        onComplete: (smsSent: Boolean, bluetoothSentCount: Int) -> Unit = { _, _ -> }
    ) {
        scope.launch(Dispatchers.IO) {
            val payload = EmergencyPayload(
                type = type,
                description = description,
                latitude = latitude,
                longitude = longitude,
                createdAtMillis = System.currentTimeMillis()
            )

            var smsSent = false
            if (!destinationSmsNumber.isNullOrBlank()) {
                smsSent = SmsCommHandler.sendEmergencySms(context, destinationSmsNumber, payload)
            }

            val bluetoothSentCount = BluetoothCommHandler.broadcastPayloadToBondedDevices(context, payload)

            Log.d(TAG, "Difusión offline completada. SMS: $smsSent | Nodos Bluetooth alcanzados: $bluetoothSentCount")

            launch(Dispatchers.Main) {
                onComplete(smsSent, bluetoothSentCount)
            }
        }
    }
}
