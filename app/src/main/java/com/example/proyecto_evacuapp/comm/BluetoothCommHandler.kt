package com.example.proyecto_evacuapp.comm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.IncidentType
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

object BluetoothCommHandler {
    private const val TAG = "BluetoothCommHandler"
    private val EVAC_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private const val SERVICE_NAME = "EvacuAppBluetoothComm"

    private var serverJob: Job? = null
    private var isListening = false

    @SuppressLint("MissingPermission")
    fun startBluetoothServer(context: Context, scope: CoroutineScope) {
        if (isListening) return
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        if (!adapter.isEnabled) return

        isListening = true
        serverJob = scope.launch(Dispatchers.IO) {
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, EVAC_UUID)
                Log.d(TAG, "Servidor Bluetooth RFCOMM activo y escuchando conexiones offline...")

                while (isListening) {
                    var socket: BluetoothSocket? = null
                    try {
                        socket = serverSocket?.accept()
                        if (socket != null) {
                            handleIncomingConnection(context, socket)
                            socket.close()
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Socket aceptado cerrado o reiniciado: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en servidor Bluetooth: ${e.message}", e)
            } finally {
                serverSocket?.close()
                isListening = false
            }
        }
    }

    private suspend fun handleIncomingConnection(context: Context, socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = socket.inputStream
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            if (bytesRead > 0) {
                val jsonText = String(buffer, 0, bytesRead)
                Log.d(TAG, "🚨 Paquete Bluetooth recibido: $jsonText")

                val payload = Gson().fromJson(jsonText, EmergencyPayload::class.java)
                if (payload != null && payload.latitude != 0.0) {
                    val newIncident = SharedIncident(
                        localId = payload.localId,
                        type = IncidentType.fromApiValue(payload.type),
                        severity = IncidentSeverity.ALTA,
                        description = "[Bluetooth Mesh] ${payload.description}",
                        latitude = payload.latitude,
                        longitude = payload.longitude,
                        createdAtMillis = payload.createdAtMillis,
                        status = IncidentStatus.VERIFIED,
                        isOwnReport = false
                    )

                    IncidentSharedState.addLocalIncident(newIncident)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo paquete Bluetooth: ${e.message}", e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun broadcastPayloadToBondedDevices(context: Context, payload: EmergencyPayload): Int = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext 0
        if (!adapter.isEnabled) return@withContext 0

        val jsonText = Gson().toJson(payload)
        val bondedDevices = adapter.bondedDevices ?: emptySet()
        var sentCount = 0

        for (device in bondedDevices) {
            try {
                val socket = device.createInsecureRfcommSocketToServiceRecord(EVAC_UUID)
                socket.connect()
                val outputStream: OutputStream = socket.outputStream
                outputStream.write(jsonText.toByteArray())
                outputStream.flush()
                socket.close()
                sentCount++
                Log.d(TAG, "Paquete Bluetooth enviado exitosamente a ${device.name}")
            } catch (e: Exception) {
                Log.d(TAG, "No se pudo conectar vía Bluetooth con ${device.name}: ${e.message}")
            }
        }

        sentCount
    }

    fun stopServer() {
        isListening = false
        serverJob?.cancel()
        serverJob = null
    }
}
