package com.example.proyecto_evacuapp.ui.screens

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.proyecto_evacuapp.R
import com.example.proyecto_evacuapp.ui.components.ConnectivityBadge
import com.example.proyecto_evacuapp.ui.components.OsrmRoutingService
import com.example.proyecto_evacuapp.ui.components.SosMeshtaticMenu
import com.example.proyecto_evacuapp.ui.components.StepInstruction
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.example.proyecto_evacuapp.ui.theme.*
import com.example.proyecto_evacuapp.utils.CustomVoicePlayer
import com.example.proyecto_evacuapp.utils.MeshtaticSender
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Clase de soporte superior para evitar errores de destructuring
data class MapBundle(
    val mapView: MapView,
    val userMarker: Marker,
    val destinationMarker: Marker,
    val routePolyline: Polyline
)

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) {
        "${meters.toInt()} m"
    } else {
        String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
    }
}

private fun formatDuration(meters: Double, speedMetersPerSecond: Double): String {
    val effectiveSpeed = if (speedMetersPerSecond > 1.5) speedMetersPerSecond else 6.94
    val seconds = (meters / effectiveSpeed).toInt()
    val minutes = (seconds + 29) / 60

    return when {
        minutes < 1 -> "Menos de 1 min"
        minutes == 1 -> "1 min"
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    var showSosSheet by remember { mutableStateOf(false) }
    var distanceToNextStepMeters by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    // ESTADOS DE GPS Y NAVEGACIÓN
    var isTrackingUser by remember { mutableStateOf(true) }
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var currentSpeedMps by remember { mutableStateOf(0.0) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var overviewTrigger by remember { mutableIntStateOf(0) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // ESTADOS PARA RUTA Y NAVEGACIÓN ACTIVA
    var customDestination by remember { mutableStateOf<GeoPoint?>(null) }
    var customDestinationName by remember { mutableStateOf("") }
    var customRoutePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var customDistanceText by remember { mutableStateOf("") }
    var customDurationText by remember { mutableStateOf("") }
    var routeSteps by remember { mutableStateOf<List<StepInstruction>>(emptyList()) }
    var isCalculatingRoute by remember { mutableStateOf(false) }
    var isAutomaticEvacuation by remember { mutableStateOf(false) }

    val dynamicRemainingDistanceMeters by remember(currentLatitude, currentLongitude, customRoutePoints) {
        derivedStateOf {
            if (currentLatitude == null || currentLongitude == null || customRoutePoints.isEmpty()) {
                0.0
            } else {
                val userPoint = GeoPoint(currentLatitude!!, currentLongitude!!)
                userPoint.distanceToAsDouble(customRoutePoints.last())
            }
        }
    }
    val dynamicDistanceText = formatDistance(dynamicRemainingDistanceMeters)
    val dynamicDurationText = formatDuration(dynamicRemainingDistanceMeters, currentSpeedMps)

    var isNavigating by remember { mutableStateOf(false) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    fun stopNavigation() {
        isNavigating = false
        currentStepIndex = 0
        customDestination = null
        customDestinationName = ""
        customRoutePoints = emptyList()
        routeSteps = emptyList()
        customDistanceText = ""
        customDurationText = ""
        isAutomaticEvacuation = false
        CustomVoicePlayer.stop()
    }

    fun calculateRouteToPoint(
        targetPoint: GeoPoint,
        targetName: String = "",
        automatic: Boolean = false
    ) {
        val startLat = currentLatitude
        val startLon = currentLongitude
        if (startLat == null || startLon == null) {
            Toast.makeText(context, "Esperando señal GPS...", Toast.LENGTH_SHORT).show()
            return
        }

        isCalculatingRoute = true
        coroutineScope.launch {
            val startPoint = GeoPoint(startLat, startLon)
            val result = OsrmRoutingService.fetchRealStreetRoute(
                start = startPoint,
                end = targetPoint,
                profile = "Vehiculo"
            )

            if (result.points.isNotEmpty()) {
                customDestination = targetPoint
                customDestinationName = targetName
                customRoutePoints = result.points
                customDistanceText = result.distanceText
                customDurationText = result.durationText
                routeSteps = result.steps
                isAutomaticEvacuation = automatic
            } else {
                Toast.makeText(context, "No se pudo calcular la ruta por calle", Toast.LENGTH_SHORT).show()
            }
            isCalculatingRoute = false
        }
    }

    fun startAutomaticEvacuation() {
        val startLat = currentLatitude
        val startLon = currentLongitude
        if (startLat == null || startLon == null) {
            Toast.makeText(context, "Esperando señal GPS...", Toast.LENGTH_SHORT).show()
            return
        }

        val userPoint = GeoPoint(startLat, startLon)
        val nearestSafeZone = findRecommendedSafeZone(userPoint)

        if (nearestSafeZone == null) {
            Toast.makeText(context, "⚠️ No hay zonas seguras registradas dentro de 15 km", Toast.LENGTH_LONG).show()
            return
        }

        isTrackingUser = false
        calculateRouteToPoint(
            targetPoint = nearestSafeZone.point,
            targetName = nearestSafeZone.name,
            automatic = true
        )
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude

                    if (location.hasSpeed()) {
                        currentSpeedMps = location.speed.toDouble()
                    }

                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    UserLocationState.currentLocation = newGeoPoint

                    if (isNavigating && routeSteps.isNotEmpty() && currentStepIndex < routeSteps.size) {
                        val currentStep = routeSteps[currentStepIndex]
                        val distanceToStep = newGeoPoint.distanceToAsDouble(currentStep.location)
                        distanceToNextStepMeters = distanceToStep.toInt()

                        val speedKmH = currentSpeedMps * 3.6
                        val triggerDistanceMeters = when {
                            speedKmH >= 60.0 -> currentSpeedMps * 15.0
                            speedKmH >= 30.0 -> currentSpeedMps * 12.0
                            else -> 30.0
                        }

                        if (distanceToStep <= triggerDistanceMeters) {
                            val audioRes = CustomVoicePlayer.getAudioForStep(currentStep.modifier)
                            CustomVoicePlayer.playAudio(context, audioRes)

                            if (currentStepIndex < routeSteps.size - 1) {
                                currentStepIndex++
                            }
                        }
                    }
                }
            }
        }
    }

    fun requestFreshLocation() {
        if (hasLocationPermission) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        if (location.hasSpeed()) currentSpeedMps = location.speed.toDouble()
                        UserLocationState.currentLocation = GeoPoint(location.latitude, location.longitude)
                    }
                    recenterTrigger++
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) requestFreshLocation()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            requestFreshLocation()
        }
    }

    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            CustomVoicePlayer.stop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapViewOSM(
            latitude = currentLatitude,
            longitude = currentLongitude,
            recenterTrigger = recenterTrigger,
            overviewTrigger = overviewTrigger,
            isTrackingUser = isTrackingUser,
            destinationPoint = customDestination,
            routePoints = customRoutePoints,
            onMapTouched = { isTrackingUser = false },
            onMapLongClick = { point ->
                if (!isNavigating) {
                    isTrackingUser = false
                    calculateRouteToPoint(point)
                }
            }
        )

        // BOTONES FLOTANTES DE CÁMARA (VISTA GENERAL Y RECENTRAR)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 240.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (customRoutePoints.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        isTrackingUser = false
                        overviewTrigger++
                    },
                    containerColor = SurfaceWhite,
                    contentColor = EvacuBlue,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Vista general de ruta"
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    isTrackingUser = true
                    requestFreshLocation()
                },
                containerColor = SurfaceWhite,
                contentColor = if (isTrackingUser) EvacuBlue else TextSecondary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recentrar mapa"
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { showSosSheet = true },
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Radio, contentDescription = "Meshtatic SOS")
                    Text("SOS / MESH", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showSosSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSosSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                SosMeshtaticMenu(
                    currentLocation = GeoPoint(currentLatitude ?: 0.0, currentLongitude ?: 0.0),
                    onSendMessage = { text ->
                        MeshtaticSender.sendBroadcastSms(context, text)
                        showSosSheet = false
                    }
                )
            }
        }

        if (isNavigating && routeSteps.isNotEmpty() && currentStepIndex < routeSteps.size) {
            val activeStep = routeSteps[currentStepIndex]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EvacuBlue),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val stepIcon = when (activeStep.modifier) {
                        "left", "slight left", "sharp left" -> Icons.Default.ArrowBack
                        "right", "slight right", "sharp right" -> Icons.Default.ArrowForward
                        "uturn" -> Icons.Default.UTurnLeft
                        "arrive" -> Icons.Default.Flag
                        else -> Icons.Default.ArrowUpward
                    }
                    Icon(
                        imageVector = stepIcon,
                        contentDescription = "Maniobra",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (distanceToNextStepMeters > 0) "EN $distanceToNextStepMeters METROS" else "PRÓXIMA MANIOBRA",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeStep.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ConnectivityBadge(
                        text = if (isCalculatingRoute) "Calculando calle..." else "GPS Activo",
                        color = SafeGreen,
                        backgroundColor = SafeGreenLight,
                        icon = Icons.Default.CloudDone
                    )
                    ConnectivityBadge(
                        text = if (hasLocationPermission) "Señal GPS Ok" else "Sin GPS",
                        color = EvacuBlue,
                        backgroundColor = EvacuBlueLight,
                        icon = Icons.Default.LocationOn
                    )
                }
            }
        }

        if (isCalculatingRoute) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = EvacuBlue
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isNavigating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Navegación en Curso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Distancia: $dynamicDistanceText | Tiempo: $dynamicDurationText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                        }
                    }
                    Button(
                        onClick = { stopNavigation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "DETENER NAVEGACIÓN", fontWeight = FontWeight.Bold)
                    }
                } else if (customDestination != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isAutomaticEvacuation) "Ruta de evacuación" else "Ruta lista",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (customDestinationName.isNotBlank()) {
                                Text(
                                    text = "Destino: $customDestinationName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "$dynamicDistanceText ($dynamicDurationText)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EvacuBlue
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { stopNavigation() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) {
                            Text(text = "CANCELAR", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                isNavigating = true
                                isTrackingUser = true
                                currentStepIndex = if (routeSteps.size > 1) 1 else 0
                                CustomVoicePlayer.playAudio(context, R.raw.inicio_evacuacion)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen, contentColor = Color.White)
                        ) {
                            Text(text = "IR", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text(
                        text = "¿Necesitas evacuar?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Button(
                        onClick = { startAutomaticEvacuation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "EVACUAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MapViewOSM(
    latitude: Double?,
    longitude: Double?,
    recenterTrigger: Int,
    overviewTrigger: Int,
    isTrackingUser: Boolean,
    destinationPoint: GeoPoint?,
    routePoints: List<GeoPoint>,
    onMapTouched: () -> Unit,
    onMapLongClick: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val mapBundle = remember {
        val map = object : MapView(context) {
            override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
                if (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE) {
                    onMapTouched()
                }
                return super.dispatchTouchEvent(ev)
            }
        }.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(17.0)
        }

        val uMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Mi Ubicación"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)
        }
        val dMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destino Seleccionado"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass)
        }
        val polyline = Polyline(map).apply {
            outlinePaint.strokeWidth = 14f
            outlinePaint.color = AndroidColor.parseColor("#108981")
        }

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onMapTouched()
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                onMapLongClick(p)
                return true
            }
        }

        val eventsOverlay = MapEventsOverlay(eventsReceiver)
        map.overlays.add(eventsOverlay)
        map.overlays.add(polyline)
        map.overlays.add(uMarker)
        map.overlays.add(dMarker)

        MapBundle(map, uMarker, dMarker, polyline)
    }

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            val userPoint = GeoPoint(latitude, longitude)
            mapBundle.userMarker.apply {
                position = userPoint
                setVisible(true)
            }
            if (isTrackingUser) {
                mapBundle.mapView.controller.animateTo(userPoint)
                mapBundle.mapView.invalidate()
            }
        }
    }

    LaunchedEffect(recenterTrigger) {
        if (latitude != null && longitude != null && recenterTrigger > 0) {
            mapBundle.mapView.controller.animateTo(GeoPoint(latitude, longitude))
        }
    }

    LaunchedEffect(destinationPoint, routePoints) {
        if (destinationPoint != null) {
            mapBundle.destinationMarker.position = destinationPoint
            mapBundle.destinationMarker.setVisible(true)
            mapBundle.routePolyline.setPoints(routePoints)
        } else {
            mapBundle.destinationMarker.setVisible(false)
            mapBundle.routePolyline.setPoints(emptyList())
        }
        mapBundle.mapView.invalidate()
    }

    LaunchedEffect(overviewTrigger) {
        if (overviewTrigger > 0 && routePoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(routePoints)
            mapBundle.mapView.zoomToBoundingBox(boundingBox, true, 120)
        }
    }

    AndroidView(
        factory = { mapBundle.mapView },
        modifier = Modifier.fillMaxSize()
    )
}