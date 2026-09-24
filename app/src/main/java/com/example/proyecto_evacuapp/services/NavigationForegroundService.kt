package com.example.proyecto_evacuapp.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.proyecto_evacuapp.MainActivity
import com.example.proyecto_evacuapp.R
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.util.GeoPoint

private const val TAG = "NavigationService"
private const val CHANNEL_ID = "evacuapp_navigation_channel"
private const val NOTIFICATION_ID = 1001

/**
 * Servicio en Primer Plano (Foreground Service) persistente para la navegación GPS.
 * Mantiene la ubicación activa y notificaciones en tiempo real aun con la app minimizada.
 */
class NavigationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Navegación EvacuApp Activa", "Iniciando guía GPS...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startBackgroundLocationUpdates()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startBackgroundLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 500L
        ).setMinUpdateIntervalMillis(250L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val speedMps = if (location.hasSpeed()) location.speed.toDouble() else 0.0
                    val bearing = if (location.hasBearing()) location.bearing else null

                    val currentPoint = GeoPoint(location.latitude, location.longitude)
                    UserLocationState.currentLocation = currentPoint
                    UserLocationState.currentSpeedMps = speedMps
                    UserLocationState.currentBearing = bearing

                    evaluateTurnAlertsAndNotification(speedMps)
                }
            }
        }
        locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Error al solicitar ubicaciones en servicio: ${e.message}")
        }
    }

    private fun evaluateTurnAlertsAndNotification(speedMps: Double) {
        val speedKmH = speedMps * 3.6
        val warningDistanceMeters = when {
            speedKmH > 60.0 -> 220
            speedKmH >= 30.0 -> 90
            else -> 45
        }

        val notifTitle = "Navegando (${speedKmH.toInt()} km/h)"
        val notifText = "Aviso de giro anticipado a $warningDistanceMeters m | GPS Activo"
        updateNotification(notifTitle, notifText)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "EvacuApp Navegación GPS"
            val descriptionText = "Notificaciones de navegación persistente en segundo plano"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
