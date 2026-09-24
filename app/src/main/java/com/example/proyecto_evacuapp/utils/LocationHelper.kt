package com.example.proyecto_evacuapp.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.util.GeoPoint
import kotlin.math.sqrt

private const val TAG = "LocationHelper"
private const val HIGH_FREQ_INTERVAL_MS = 300L
private const val HIGH_FREQ_MIN_INTERVAL_MS = 150L
private const val LOW_FREQ_INTERVAL_MS = 3000L

class LocationHelper(private val context: Context) : SensorEventListener {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastLocationPoint: Location? = null
    private var lastStationaryCheckTime: Long = System.currentTimeMillis()
    private var stationaryStartLocation: Location? = null
    private var isLowPowerMode = false
    private var activeLocationCallback: LocationCallback? = null
    private var currentCallback: ((GeoPoint) -> Unit)? = null

    // Batería Adaptativa - Sensor
    private var lastAccelMagnitude = 9.81
    private var lowAccelStartTime: Long? = null

    init {
        registerAccelerometerIfAvailable()
    }

    private fun registerAccelerometerIfAvailable() {
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(onLocationReceived: (GeoPoint) -> Unit) {
        if (!hasLocationPermission()) return

        currentCallback = onLocationReceived
        stopLocationUpdates()

        val interval = if (isLowPowerMode) LOW_FREQ_INTERVAL_MS else HIGH_FREQ_INTERVAL_MS
        val minInterval = if (isLowPowerMode) 1500L else HIGH_FREQ_MIN_INTERVAL_MS

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, interval
        ).setMinUpdateIntervalMillis(minInterval).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    processLocationUpdate(location, onLocationReceived)
                }
            }
        }
        activeLocationCallback = callback

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )
    }

    private fun processLocationUpdate(location: Location, onLocationReceived: (GeoPoint) -> Unit) {
        val lastLoc = lastLocationPoint
        val speed = if (location.hasSpeed()) location.speed else 0.0f

        // 1. FILTRO ANTI-TIEMBLA (Jitter Filter):
        // Si se movió menos de 0.5 metros y la velocidad es nula, omitir la actualización visual
        if (lastLoc != null) {
            val dist = lastLoc.distanceTo(location)
            if (dist < 0.5f && speed <= 0.1f) {
                return
            }
        }

        var bearing = if (location.hasBearing()) location.bearing else null
        if (bearing == null || bearing == 0.0f) {
            if (lastLoc != null) {
                val calculated = lastLoc.bearingTo(location)
                if (calculated != 0.0f) {
                    bearing = (calculated + 360.0f) % 360.0f
                }
            }
        }

        lastLocationPoint = location

        UserLocationState.currentLocation = GeoPoint(location.latitude, location.longitude)
        UserLocationState.currentSpeedMps = speed.toDouble()
        UserLocationState.currentBearing = bearing

        // 2. BATERÍA ADAPTATIVA - FALLBACK POR GPS (Gama baja / Sin acelerómetro)
        checkGpsBatteryFallback(location, speed)

        onLocationReceived(GeoPoint(location.latitude, location.longitude))
    }

    private fun checkGpsBatteryFallback(location: Location, speed: Float) {
        val now = System.currentTimeMillis()
        val statLoc = stationaryStartLocation ?: location.also { stationaryStartLocation = it }

        val distFromStationary = statLoc.distanceTo(location)

        if (speed > 1.0f || distFromStationary > 1.5f) {
            // Vehículo en movimiento: restablecer frecuencia alta inmediatamente (300ms)
            stationaryStartLocation = location
            lastStationaryCheckTime = now
            if (isLowPowerMode) {
                Log.d(TAG, "Movimiento detectado (speed=${speed}m/s, dist=${distFromStationary}m). Restableciendo alta frecuencia (300ms)")
                isLowPowerMode = false
                currentCallback?.let { startLocationUpdates(it) }
            }
        } else {
            // Estacionado o sin desplazamiento (>1.5m) durante 10 segundos
            if (now - lastStationaryCheckTime >= 10000L && !isLowPowerMode) {
                Log.d(TAG, "Vehículo sin desplazamiento durante 10s. Cambiando a batería adaptativa (3000ms)")
                isLowPowerMode = true
                currentCallback?.let { startLocationUpdates(it) }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
            val delta = kotlin.math.abs(magnitude - lastAccelMagnitude)
            lastAccelMagnitude = magnitude

            val now = System.currentTimeMillis()
            if (delta < 0.3) {
                val start = lowAccelStartTime ?: now.also { lowAccelStartTime = it }
                if (now - start >= 10000L && !isLowPowerMode) {
                    Log.d(TAG, "Aceleración casi nula (<0.3m/s²) durante 10s. Reduciendo tasa de muestreo GPS.")
                    isLowPowerMode = true
                    currentCallback?.let { startLocationUpdates(it) }
                }
            } else {
                lowAccelStartTime = null
                if (isLowPowerMode) {
                    Log.d(TAG, "Aceleración detectada. Restableciendo GPS de alta frecuencia.")
                    isLowPowerMode = false
                    currentCallback?.let { startLocationUpdates(it) }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun stopLocationUpdates() {
        activeLocationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            activeLocationCallback = null
        }
    }
}
