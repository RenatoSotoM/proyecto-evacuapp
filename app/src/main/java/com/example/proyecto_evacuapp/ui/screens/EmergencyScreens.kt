package com.example.proyecto_evacuapp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.proyecto_evacuapp.R
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.example.proyecto_evacuapp.ui.theme.AppBackground
import com.example.proyecto_evacuapp.ui.theme.DangerRed
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueDark
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import com.example.proyecto_evacuapp.ui.theme.WarningAmber
import com.example.proyecto_evacuapp.utils.CustomVoicePlayer
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class EmergencySafeZone(
    val name: String,
    val point: GeoPoint,
    val riskLevel: String = "Bajo"
)

val safeZoneCandidates = listOf(
    EmergencySafeZone("Parque García de la Huerta", GeoPoint(-33.5925, -70.7045)),
    EmergencySafeZone("Estadio Municipal de San Bernardo", GeoPoint(-33.5980, -70.7010)),
    EmergencySafeZone("Plaza de Armas de San Bernardo", GeoPoint(-33.5921, -70.7027)),
    EmergencySafeZone("Parque Víctor Jara", GeoPoint(-33.5180, -70.6520)),
    EmergencySafeZone("Parque O'Higgins", GeoPoint(-33.4638, -70.6610)),
    EmergencySafeZone("Parque Metropolitano", GeoPoint(-33.4250, -70.6330))
)

suspend fun fetchOSRMRoute(
    start: GeoPoint,
    destination: GeoPoint,
    mobilityMode: String = "Vehículo"
): List<GeoPoint> {
    val osrmProfile = when {
        mobilityMode.contains("Vehí", ignoreCase = true) ||
                mobilityMode.contains("Auto", ignoreCase = true) -> "driving"
        mobilityMode.contains("Bici", ignoreCase = true) -> "bike"
        else -> "foot"
    }

    return withContext(Dispatchers.IO) {
        try {
            val urlString = "https://router.project-osrm.org/route/v1/$osrmProfile/" +
                    "${start.longitude},${start.latitude};" +
                    "${destination.longitude},${destination.latitude}" +
                    "?overview=full&geometries=geojson"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "EvacuApp-UBO-StudentProject/1.0")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val stream = connection.inputStream
                val responseText = stream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val routes = json.optJSONArray("routes")

                if (routes != null && routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")

                    val points = mutableListOf<GeoPoint>()
                    for (i in 0 until coordinates.length()) {
                        val coord = coordinates.getJSONArray(i)
                        points.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                    }
                    return@withContext points
                }
            }
        } catch (e: Exception) {
            Log.e("OSRM_ROUTE", "Error: ${e.localizedMessage}")
        }
        listOf(start, destination)
    }
}

@Composable
fun EmergencyTypeSelectScreen(
    onEmergencySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val emergencyTypes = listOf(
        Triple("Terremoto", "🌎", "Sismo y réplicas urbanas"),
        Triple("Incendio", "🔥", "Incendio estructural o forestal"),
        Triple("Inundación", "🌊", "Anegamiento y crecida de canales"),
        Triple("Tsunami", "🌊", "Evacuación sobre cota 30"),
        Triple("Derrumbe", "🪨", "Corte de quebradas y laderas"),
        Triple("Otra emergencia", "⚠️", "Peligro general no clasificado")
    )

    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selecciona la emergencia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            emergencyTypes.forEach { (name, emoji, subtitle) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { onEmergencySelected(name) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = EvacuBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyActiveScreen(
    emergencyType: String,
    selectedDestinationName: String = "Zona Segura",
    selectedDestinationPoint: GeoPoint = GeoPoint(-33.5925, -70.7045),
    routeDistanceMeters: Double? = null,
    routeDurationSeconds: Double? = null,
    onStartNavigation: (destinationName: String, destinationPoint: GeoPoint) -> Unit,
    onBack: () -> Unit
) {
    val userPoint = UserLocationState.currentLocation ?: GeoPoint(-33.5925, -70.7045)
    val hasCustomDestination = selectedDestinationName.isNotBlank() && selectedDestinationName != "Zona Segura"

    val maxRadiusMeters = 20000.0
    val safeZonesInRange = safeZoneCandidates.filter { userPoint.distanceToAsDouble(it.point) <= maxRadiusMeters }
    val nearestSafeZone = safeZonesInRange.minByOrNull { userPoint.distanceToAsDouble(it.point) } ?: safeZoneCandidates.first()

    val finalDestinationName = if (hasCustomDestination) selectedDestinationName else nearestSafeZone.name
    val finalDestinationPoint = if (hasCustomDestination) selectedDestinationPoint else nearestSafeZone.point

    val realDistanceMeters = routeDistanceMeters ?: (userPoint.distanceToAsDouble(finalDestinationPoint) * 1.35)
    val distanceText = if (realDistanceMeters >= 1000) String.format(Locale.getDefault(), "%.1f km", realDistanceMeters / 1000.0) else "${realDistanceMeters.toInt()} m"
    val estimatedTimeMinutes = routeDurationSeconds?.let { (it / 60.0).toInt().coerceAtLeast(1) } ?: (realDistanceMeters / 666.0).toInt().coerceAtLeast(1)

    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text(text = "MODO EMERGENCIA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DangerRed)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Zona segura seleccionada", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Text(text = finalDestinationName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Distancia", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(text = distanceText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = EvacuBlue)
                            }
                            Column {
                                Text(text = "Tiempo", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(text = "$estimatedTimeMinutes min", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column {
                                Text(text = "Riesgo", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(text = "Bajo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SafeGreen)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onStartNavigation(finalDestinationName, finalDestinationPoint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White)
            ) {
                Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "INICIAR EVACUACIÓN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ActiveNavigationScreen(
    destinationName: String = "Zona Segura",
    destinationPoint: GeoPoint = GeoPoint(-33.5925, -70.7045),
    mobilityMode: String = "Vehículo",
    fullPolyline: List<GeoPoint> = emptyList(),
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentPoint by remember {
        mutableStateOf(UserLocationState.currentLocation ?: GeoPoint(-33.5925, -70.7045))
    }
    var currentBearing by remember { mutableDoubleStateOf(0.0) }
    var remainingDistanceMeters by remember { mutableDoubleStateOf(0.0) }
    var routePolylinePoints by remember { mutableStateOf(fullPolyline) }

    var turnInstruction by remember { mutableStateOf("Siga recto hacia la zona segura") }
    var turnIconType by remember { mutableStateOf("straight") }

    DisposableEffect(context) {
        onDispose {
            CustomVoicePlayer.stop()
        }
    }

    LaunchedEffect(currentPoint, destinationPoint, mobilityMode) {
        val fetchedPoints = fetchOSRMRoute(currentPoint, destinationPoint, mobilityMode)
        if (fetchedPoints.size > 2) {
            routePolylinePoints = fetchedPoints

            if (fetchedPoints.size > 1) {
                val p1 = fetchedPoints[0]
                val p2 = fetchedPoints[1]
                val bearingDiff = (p2.longitude - p1.longitude)
                val newInstruction: String
                val newType: String
                if (bearingDiff > 0.0001) {
                    newInstruction = "Gire levemente a la derecha"
                    newType = "right"
                } else if (bearingDiff < -0.0001) {
                    newInstruction = "Gire levemente a la izquierda"
                    newType = "left"
                } else {
                    newInstruction = "Continúe recto"
                    newType = "straight"
                }

                if (turnInstruction != newInstruction) {
                    turnInstruction = newInstruction
                    turnIconType = newType
                    val audioResId = CustomVoicePlayer.getAudioForStep(newType)
                    CustomVoicePlayer.playAudio(context, audioResId)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val newPoint = GeoPoint(loc.latitude, loc.longitude)
                    currentPoint = newPoint
                    UserLocationState.currentLocation = newPoint
                    currentBearing = loc.bearing.toDouble()
                    remainingDistanceMeters = newPoint.distanceToAsDouble(destinationPoint)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        onDispose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    val remainingDistanceText = if (remainingDistanceMeters >= 1000) {
        String.format(Locale.getDefault(), "%.1f km", remainingDistanceMeters / 1000.0)
    } else {
        "${remainingDistanceMeters.toInt()} m"
    }

    val speedMetersPerMinute = when {
        mobilityMode.contains("Vehí", ignoreCase = true) || mobilityMode.contains("Auto", ignoreCase = true) -> 500.0
        mobilityMode.contains("Bici", ignoreCase = true) -> 250.0
        else -> 80.0
    }
    val remainingMinutes = (remainingDistanceMeters / speedMetersPerMinute).toInt().coerceAtLeast(1)

    Surface(modifier = Modifier.fillMaxSize(), color = EvacuBlueDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = when (turnIconType) {
                            "left" -> WarningAmber
                            "right" -> EvacuBlue
                            else -> SafeGreen
                        },
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = when (turnIconType) {
                                "left" -> Icons.Default.ArrowBack
                                "right" -> Icons.Default.ArrowForward
                                else -> Icons.Default.Navigation
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = turnInstruction,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Hacia: $destinationName",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxSize(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    ActiveNavigationMapOSM(
                        currentPoint = currentPoint,
                        bearing = currentBearing,
                        destinationPoint = destinationPoint,
                        destinationName = destinationName,
                        fullPolyline = routePolylinePoints
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Distancia / Tiempo", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "$remainingDistanceText • $remainingMinutes min aprox.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EvacuBlue
                        )
                        Text(
                            text = "Riesgo: Bajo",
                            style = MaterialTheme.typography.bodySmall,
                            color = SafeGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text(text = "FINALIZAR", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

private class MapStateHolder(
    val mapView: MapView,
    val userMarker: Marker,
    val endMarker: Marker,
    val routePolyline: Polyline
)

@Composable
fun ActiveNavigationMapOSM(
    currentPoint: GeoPoint,
    bearing: Double,
    destinationPoint: GeoPoint,
    destinationName: String,
    fullPolyline: List<GeoPoint>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val holder = remember {
        Configuration.getInstance().userAgentValue = "EvacuApp-UBO-StudentProject/1.0"
        val map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(18.5)
        }

        val userM = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            title = "Tu ubicación"
            setInfoWindow(null)
        }

        val endM = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = destinationName
            setInfoWindow(null)
        }

        val line = Polyline(map).apply {
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.strokeWidth = 16f
            outlinePaint.color = Color(0xFF0066FF).toArgb()
        }

        map.overlays.add(line)
        map.overlays.add(userM)
        map.overlays.add(endM)

        MapStateHolder(map, userM, endM, line)
    }

    LaunchedEffect(currentPoint, bearing, fullPolyline) {
        holder.userMarker.position = currentPoint
        holder.userMarker.rotation = bearing.toFloat()
        holder.endMarker.position = destinationPoint

        val remainingPath = if (fullPolyline.isNotEmpty()) {
            val closestIndex = findClosestPointIndex(fullPolyline, currentPoint)
            val sliced = fullPolyline.drop(closestIndex).toMutableList()
            if (sliced.isEmpty()) sliced.add(currentPoint) else sliced.add(0, currentPoint)
            sliced
        } else {
            listOf(currentPoint, destinationPoint)
        }

        holder.routePolyline.setPoints(remainingPath)
        holder.mapView.controller.animateTo(currentPoint)
        holder.mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> holder.mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            holder.mapView.onDetach()
        }
    }

    AndroidView(factory = { holder.mapView }, modifier = Modifier.fillMaxSize())
}

private fun findClosestPointIndex(points: List<GeoPoint>, current: GeoPoint): Int {
    if (points.isEmpty()) return 0
    var minIndex = 0
    var minDistance = Double.MAX_VALUE
    for (i in points.indices) {
        val dist = points[i].distanceToAsDouble(current)
        if (dist < minDistance) {
            minDistance = dist
            minIndex = i
        }
    }
    return minIndex
}