package com.example.proyecto_evacuapp.ui.screens

import android.Manifest
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
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(onFindRoute: () -> Unit) {
    var showSosSheet by remember { mutableStateOf(false) }
    var distanceToNextStepMeters by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    // ESTADOS DE GPS Y NAVEGACIÓN
    var isTrackingUser by remember { mutableStateOf(true) }
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // ESTADOS PARA RUTA Y NAVEGACIÓN ACTIVA
    var customDestination by remember { mutableStateOf<GeoPoint?>(null) }
    var customRoutePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var customDistanceText by remember { mutableStateOf("") }
    var customDurationText by remember { mutableStateOf("") }
    var routeSteps by remember { mutableStateOf<List<StepInstruction>>(emptyList()) }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    // ESTADO DE MODO NAVEGACIÓN "IR"
    var isNavigating by remember { mutableStateOf(false) }
    var currentStepIndex by remember { mutableIntStateOf(0) }

    fun stopNavigation() {
        isNavigating = false
        currentStepIndex = 0
        customDestination = null
        customRoutePoints = emptyList()
        routeSteps = emptyList()
        customDistanceText = ""
        customDurationText = ""
        CustomVoicePlayer.stop()
    }

    // Calcular ruta vehicular
    // Calcular ruta vehicular (con límite estricto de 15 km para zonas seguras)
    fun calculateRouteToPoint(targetPoint: GeoPoint) {
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
                // OSRM devuelve la distancia en texto (ej. "12.4 km" o "850 m")
                // Validamos el límite operativo de 15 km (15000 metros)
                val distanceInMeters = parseDistanceToMeters(result.distanceText)

                if (distanceInMeters > 15000.0) {
                    Toast.makeText(context, "⚠️ La zona segura supera el radio máximo de 15 km", Toast.LENGTH_LONG).show()
                    isCalculatingRoute = false
                    return@launch
                }

                customDestination = targetPoint
                customRoutePoints = result.points
                customDistanceText = result.distanceText
                customDurationText = result.durationText
                routeSteps = result.steps
            } else {
                Toast.makeText(context, "No se pudo calcular la ruta por calle", Toast.LENGTH_SHORT).show()
            }
            isCalculatingRoute = false
        }
    }

    fun calculateAlternativeRouteAvoidingHazards(
        start: GeoPoint,
        end: GeoPoint,
        blockedPoints: List<GeoPoint>
    ) {
        isCalculatingRoute = true
        coroutineScope.launch {
            val result = OsrmRoutingService.fetchRealStreetRouteWithAvoidance(
                start = start,
                end = end,
                avoidPoints = blockedPoints,
                profile = "Vehiculo"
            )
            if (result.points.isNotEmpty()) {
                customRoutePoints = result.points
                customDistanceText = result.distanceText
                customDurationText = result.durationText
                routeSteps = result.steps
            } else {
                Toast.makeText(context, "No se encontró ruta alternativa", Toast.LENGTH_SHORT).show()
            }
            isCalculatingRoute = false
        }
    }

    fun onHazardReportReceived(hazardPoint: GeoPoint, activeHazards: MutableState<List<GeoPoint>>) {
        activeHazards.value = activeHazards.value + hazardPoint

        if (customDestination != null && currentLatitude != null && currentLongitude != null) {
            Toast.makeText(context, "⚠️ Incidente en ruta. Buscando vía alternativa...", Toast.LENGTH_LONG).show()

            // Puedes usar un audio existente o eliminar esta línea si no tienes el archivo raw creado
            CustomVoicePlayer.playAudio(context, R.raw.inicio_evacuacion)

            calculateAlternativeRouteAvoidingHazards(
                start = GeoPoint(currentLatitude!!, currentLongitude!!),
                end = customDestination!!,
                blockedPoints = activeHazards.value
            )
        }
    }

    // Función auxiliar para convertir el texto de OSRM a metros para la validación
    fun parseDistanceToMeters(distanceText: String): Double {
        return try {
            val cleanText = distanceText.lowercase().replace(",", ".")
            when {
                cleanText.contains("km") -> cleanText.replace("km", "").trim().toDouble() * 1000.0
                cleanText.contains("m") -> cleanText.replace("m", "").trim().toDouble()
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    // SEGUIMIENTO GPS Y AVANCE AUTOMÁTICO
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    UserLocationState.currentLocation = GeoPoint(location.latitude, location.longitude)

                    if (isNavigating && routeSteps.isNotEmpty() && currentStepIndex < routeSteps.size) {
                        val currentStep = routeSteps[currentStepIndex]
                        val userGeo = GeoPoint(location.latitude, location.longitude)
                        val distanceToStep = userGeo.distanceToAsDouble(currentStep.location)
                        distanceToNextStepMeters = distanceToStep.toInt()

                        val speedMetersPerSecond = if (location.hasSpeed()) location.speed.toDouble() else 0.0
                        val speedKmH = speedMetersPerSecond * 3.6

                        val triggerDistanceMeters = when {
                            speedKmH >= 60.0 -> speedMetersPerSecond * 15.0
                            speedKmH >= 30.0 -> speedMetersPerSecond * 12.0
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
        // MAPA
        MapViewOSM(
            latitude = currentLatitude,
            longitude = currentLongitude,
            recenterTrigger = recenterTrigger,
            isTrackingUser = isTrackingUser,
            destinationPoint = customDestination,
            routePoints = customRoutePoints,
            onMapTouched = { if (!isNavigating) isTrackingUser = false },
            onMapLongClick = { point ->
                if (!isNavigating) {
                    isTrackingUser = false
                    calculateRouteToPoint(point)
                }
            }
        )

        // COLUMNA DE BOTONES FLOTANTES
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // BOTÓN SOS MESHTATIC
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

        // TARJETA FLOTANTE SUPERIOR (NAVEGACIÓN)
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
                    IconButton(onClick = {
                        val audioRes = CustomVoicePlayer.getAudioForStep(activeStep.modifier)
                        CustomVoicePlayer.playAudio(context, audioRes)
                    }) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Repetir voz",
                            tint = Color.White
                        )
                    }
                }
            }
        } else {
            // BARRA SUPERIOR NORMAL CUANDO NO SE ESTÁ NAVEGANDO
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

        // INDICADOR DE CARGA
        if (isCalculatingRoute) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = EvacuBlue
            )
        }

        // BOTÓN RECENTRAR MAPA
        FloatingActionButton(
            onClick = {
                isTrackingUser = true
                requestFreshLocation()
            },
            containerColor = SurfaceWhite,
            contentColor = if (isTrackingUser) EvacuBlue else TextSecondary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 240.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recentrar mapa"
            )
        }

        // PANEL INFERIOR DINÁMICO
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
                    // PANEL DURANTE MODO NAVEGACIÓN "IR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Navegación Vehicular en Curso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Distancia: $customDistanceText | Tiempo: $customDurationText",
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "DETENER NAVEGACIÓN", fontWeight = FontWeight.Bold)
                    }
                } else if (customDestination != null) {
                    // PANEL DE PREVIA DE RUTA TRAZADA (IR / CANCELAR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ruta vehicular lista",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = EvacuBlue
                                )
                                Text(
                                    text = if (customDistanceText.isNotEmpty()) "$customDistanceText ($customDurationText)" else "Midiendo...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EvacuBlue
                                )
                            }
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
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SafeGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "IR", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // PANEL POR DEFECTO
                    Text(
                        text = "¿Necesitas evacuar?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Mantén presionado cualquier punto del mapa para medir rutas y navegar por calles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onFindRoute,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Route, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ENCONTRAR RUTA SEGURA", fontWeight = FontWeight.Bold)
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
    isTrackingUser: Boolean,
    destinationPoint: GeoPoint?,
    routePoints: List<GeoPoint>,
    onMapTouched: () -> Unit,
    onMapLongClick: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val (mapView, userMarker, destinationMarker, routePolyline) = remember {
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

        Quadruple(map, uMarker, dMarker, polyline)
    }

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            val userPoint = GeoPoint(latitude, longitude)
            (userMarker as Marker).apply {
                position = userPoint
                setVisible(true)
            }
            if (isTrackingUser) {
                (mapView as MapView).controller.animateTo(userPoint)
                (mapView as MapView).invalidate()
            }
        }
    }

    LaunchedEffect(recenterTrigger) {
        if (latitude != null && longitude != null && recenterTrigger > 0) {
            (mapView as MapView).controller.animateTo(GeoPoint(latitude, longitude))
        }
    }

    LaunchedEffect(destinationPoint, routePoints) {
        val map = mapView as MapView
        val destMarker = destinationMarker as Marker
        val line = routePolyline as Polyline

        if (destinationPoint != null) {
            destMarker.position = destinationPoint
            destMarker.setVisible(true)
            line.setPoints(routePoints)
        } else {
            destMarker.setVisible(false)
            line.setPoints(emptyList())
        }
        map.invalidate()
    }

    AndroidView(
        factory = { mapView as MapView },
        modifier = Modifier.fillMaxSize()
    )
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

fun setupRouteMarkers(
    mapView: MapView,
    startPoint: GeoPoint,
    endPoint: GeoPoint
) {
    val context = mapView.context
    mapView.overlays.removeAll { it is Marker }

    val startMarker = Marker(mapView).apply {
        position = startPoint
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = "Punto de Origen (Tú)"
        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)
    }

    val endMarker = Marker(mapView).apply {
        position = endPoint
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = "Zona Segura de Evacuación"
        icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass)
    }

    mapView.overlays.add(startMarker)
    mapView.overlays.add(endMarker)
    mapView.invalidate()
}

// Función auxiliar para convertir el texto de OSRM a metros para la validación de los 15 km
private fun parseDistanceToMeters(distanceText: String): Double {
    return try {
        val cleanText = distanceText.lowercase().replace(",", ".")
        when {
            cleanText.contains("km") -> cleanText.replace("km", "").trim().toDouble() * 1000.0
            cleanText.contains("m") -> cleanText.replace("m", "").trim().toDouble()
            else -> 0.0
        }
    } catch (e: Exception) {
        0.0
    }
}