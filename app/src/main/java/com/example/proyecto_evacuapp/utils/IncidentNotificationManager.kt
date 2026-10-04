package com.example.proyecto_evacuapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import org.osmdroid.util.GeoPoint

object IncidentNotificationManager {
    private const val TAG = "NotificationManager"
    private const val CHANNEL_ID = "evacuapp_incidents_channel"
    private const val CHANNEL_NAME = "Alertas de Evacuación Táctica"
    private const val PROXIMITY_RADIUS_METERS = 3000.0 // Radio de 3 km

    private val notifiedIncidentIds = HashSet<String>()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones prioritarias de incidentes en radio de 3 km"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Evalúa la lista de incidentes contra la ubicación GPS actual.
     * Dispara notificación PUSH local SOLO si el incidente está dentro del radio de 3 km.
     */
    fun checkAndNotifyProximityIncidents(
        context: Context,
        userLocation: GeoPoint?,
        incidents: List<SharedIncident>
    ) {
        if (userLocation == null || userLocation.latitude == 0.0) return

        incidents.forEach { incident ->
            val isActive = incident.status != IncidentStatus.REJECTED && incident.status != IncidentStatus.SYNC_FAILED
            if (!isActive) return@forEach

            val incidentPoint = GeoPoint(incident.latitude, incident.longitude)
            val distanceMeters = userLocation.distanceToAsDouble(incidentPoint)

            val isWithin = distanceMeters <= PROXIMITY_RADIUS_METERS
            val idKey = incident.remoteId ?: incident.localId
            val notified = isWithin && !notifiedIncidentIds.contains(idKey)

            Log.d("EVAC_METRIC", "[GEOFENCE] Distance: ${distanceMeters.toInt()}m | WithinRadius3km: $isWithin | NotificationTriggered: $notified")

            if (notified) {
                notifiedIncidentIds.add(idKey)
                showLocalPushNotification(context, incident, distanceMeters)
            }
        }
    }

    private fun showLocalPushNotification(context: Context, incident: SharedIncident, distanceMeters: Double) {
        try {
            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val distText = if (distanceMeters < 1000) "${distanceMeters.toInt()} m" else "${"%.1f".format(distanceMeters / 1000.0)} km"
            val title = "⚠️ Peligro a $distText: ${incident.type.emoji} ${incident.type.displayName}"
            val text = "${incident.description.ifBlank { "Precaución en la vía" }} (Severidad: ${incident.severity.name})"

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationId = (incident.remoteId ?: incident.localId).hashCode()
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Notificación Push local enviada para incidente a $distText")
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando notificación push: ${e.message}", e)
        }
    }
}
