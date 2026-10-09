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
            smsManager.sendTextMessage(destinationNumber, null, frameText, null, null)
            Log.d(TAG, "Trama SMS enviada con éxito a $destinationNumber: $frameText")
            Toast.makeText(context, "💬 Trama de emergencia enviada por SMS", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando SMS de emergencia: ${e.message}", e)
            Toast.makeText(context, "Error al enviar SMS de emergencia: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
