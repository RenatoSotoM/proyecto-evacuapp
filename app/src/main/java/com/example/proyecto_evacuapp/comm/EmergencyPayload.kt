package com.example.proyecto_evacuapp.comm

import java.util.UUID

/**
 * Modelo de datos compacto para la transmisión offline de alertas
 * entre dispositivos mediante SMS, Bluetooth LE / RFCOMM o Red Mesh.
 */
data class EmergencyPayload(
    val localId: String = UUID.randomUUID().toString(),
    val type: String,
    val severity: String = "ALTA",
    val description: String = "Alerta de emergencia en terreno",
    val latitude: Double,
    val longitude: Double,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val channel: String = "MESH" // "SMS", "BLUETOOTH", "MESH"
) {
    /**
     * Serializa a la trama compacta estandarizada para SMS de baja latencia.
     */
    fun toSmsFrame(): String {
        return "EVAC_ALERT:$type|$latitude|$longitude|$createdAtMillis|${description.take(40)}"
    }

    companion object {
        /**
         * Deserializa una trama compacta SMS recibida.
         */
        fun parseFromSmsFrame(smsText: String): EmergencyPayload? {
            if (!smsText.startsWith("EVAC_ALERT:")) return null
            return try {
                val body = smsText.removePrefix("EVAC_ALERT:")
                val parts = body.split("|")
                if (parts.size < 4) return null

                val type = parts[0]
                val lat = parts[1].toDoubleOrNull() ?: return null
                val lon = parts[2].toDoubleOrNull() ?: return null
                val timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis()
                val desc = if (parts.size >= 5) parts[4] else "Alerta recibida por SMS"

                EmergencyPayload(
                    type = type,
                    severity = "ALTA",
                    description = desc,
                    latitude = lat,
                    longitude = lon,
                    createdAtMillis = timestamp,
                    channel = "SMS"
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
