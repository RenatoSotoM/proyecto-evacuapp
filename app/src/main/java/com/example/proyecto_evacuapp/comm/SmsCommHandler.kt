package com.example.proyecto_evacuapp.comm

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast

object SmsCommHandler {
    private const val TAG = "SmsCommHandler"

    fun sendEmergencySms(context: Context, destinationNumber: String, payload: EmergencyPayload): Boolean {
        return try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val frameText = payload.toSmsFrame()
            val tramaBytes = frameText.toByteArray(Charsets.UTF_8).size
            smsManager.sendTextMessage(destinationNumber, null, frameText, null, null)
            Log.d(TAG, "Trama SMS enviada con éxito a $destinationNumber: $frameText ($tramaBytes bytes)")
            Toast.makeText(context, "💬 Trama de emergencia enviada por SMS", Toast.LENGTH_SHORT).show()

            val smsTypeStr = payload.type.replace(",", " ")
            val smsCoordsStr = String.format(java.util.Locale.US, "%.5f;%.5f", payload.latitude, payload.longitude)

            com.example.proyecto_evacuapp.utils.PowerBiCsvLogger.logMetricRow(
                context = context,
                ejecucionId = payload.localId.take(8),
                modo = "SMS_OffGrid",
                perfil = payload.channel,
                tipoRuta = "Evento_SMS",
                destinoNombre = smsTypeStr,
                destinoCoords = smsCoordsStr,
                nodosProcesados = 0,
                aristasProcesadas = 0,
                cargaBBoxMs = 0L,
                calculoRutaMs = 0L,
                tiempoTotalMs = 0L,
                distanciaMeters = 0.0,
                tiempoEstimadoMin = 0,
                giros = 0,
                inferenciaOnnxMs = 0.0,
                tramaBytes = tramaBytes,
                exito = true
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SMS de emergencia: ${e.message}", e)
            Toast.makeText(context, "Error al enviar SMS de emergencia: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
