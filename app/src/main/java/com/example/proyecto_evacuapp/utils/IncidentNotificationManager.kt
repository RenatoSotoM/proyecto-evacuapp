package com.example.proyecto_evacuapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.proyecto_evacuapp.data.local.IncidentEntity

object IncidentNotificationManager {
    private const val CHANNEL_ID = "evacuapp_incidents_channel"
    private const val CHANNEL_NAME = "Alertas de Evacuación"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones prioritarias de incidentes a menos de 5 km"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNearbyIncidentNotification(context: Context, incident: IncidentEntity) {
        createNotificationChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("⚠️ Peligro a menos de 5 km: ${incident.title}")
            .setContentText("${incident.description} (Severidad: ${incident.severityLevel})")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(incident.id.hashCode(), builder.build())
    }
}