package com.example.proyecto_evacuapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.core.content.ContextCompat
import com.example.proyecto_evacuapp.R
import com.example.proyecto_evacuapp.data.remote.PointOfInterestResponse
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.data.remote.SafeZoneNearbyDto
import com.example.proyecto_evacuapp.data.repository.IncidentRepository
import com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine
import com.example.proyecto_evacuapp.ui.components.*
import com.example.proyecto_evacuapp.ui.theme.*
import com.example.proyecto_evacuapp.utils.CustomVoicePlayer
import com.example.proyecto_evacuapp.utils.MeshtaticSender
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class RecommendedSafeZone(
    val point: GeoPoint,
    val name: String
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

private fun findBlockingIncidentOnRoute(
    routePoints: List<GeoPoint>,
    incidents: List<SharedIncident>,
    maxDistanceMeters: Double = 40.0
): SharedIncident? {
    if (routePoints.isEmpty() || incidents.isEmpty()) return null

    val blockingIncidents = incidents.filter {
        (it.status == IncidentStatus.VERIFIED || it.status == IncidentStatus.PROBABLE) &&
                (it.type == IncidentType.BLOQUEO_VIAL ||
                        it.type == IncidentType.INCENDIO ||
                        it.type == IncidentType.INUNDACION ||
                        it.type == IncidentType.DERRUMBE ||
                        it.type == IncidentType.ACCIDENTE ||
                        it.type == IncidentType.RUTA_INACCESIBLE)
    }

    for (incident in blockingIncidents) {
        val incidentPoint = GeoPoint(incident.latitude, incident.longitude)
        for (point in routePoints) {
            if (point.distanceToAsDouble(incidentPoint) <= maxDistanceMeters) {
                return incident
            }
        }
    }
    return null
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
    val sharedIncidents = IncidentSharedState.incidents

    // ESTADOS DE GPS Y NAVEGACIÓN
    var isTrackingUser by remember { mutableStateOf(true) }
    var currentLatitude by remember { mutableStateOf<Double?>(null) }
    var currentLongitude by remember { mutableStateOf<Double?>(null) }
    var currentSpeedMps by remember { mutableStateOf(0.0) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var overviewTrigger by remember { mutableIntStateOf(0) }

    // Punto de origen donde se realizó el último cálculo de ruta (para re-routing dinámico > 25m)
    var lastRouteCalcPoint by remember { mutableStateOf<GeoPoint?>(null) }

    // ESTADO DE ZONAS SEGURAS Y PUNTOS DE INTERÉS
    var safeZones by remember { mutableStateOf<List<SafeZoneNearbyDto>>(emptyList()) }
    var pointsOfInterest by remember { mutableStateOf<List<PointOfInterestResponse>>(emptyList()) }
    var selectedPoiType by remember { mutableStateOf<String?>(null) }
    var isFetchingPois by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // ESTADOS PARA RUTA, VARIANTES Y NAVEGACIÓN ACTIVA
    var customDestination by remember { mutableStateOf<GeoPoint?>(null) }
    var customDestinationName by remember { mutableStateOf("") }
    var customRoutePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var routeAlternatives by remember { mutableStateOf<List<LocalRouteResult>>(emptyList()) }
    var selectedRouteVariant by remember { mutableStateOf<LocalRouteResult?>(null) }
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

    // CARGAR INCIDENTES DE LA API Y SINCRONIZAR CON EL ESTADO COMPARTIDO
    LaunchedEffect(currentLatitude, currentLongitude) {
        try {
            val response = IncidentRepository(RetrofitClient.incidentApiService).getIncidents(
                lat = currentLatitude,
                lng = currentLongitude
            )
            if (response.isSuccessful) {
                val remoteList = response.body().orEmpty()
                IncidentSharedState.syncRemoteIncidents(remoteList)
                Log.d("MAP_INCIDENTS", "Incidentes cargados y sincronizados: ${remoteList.size}")
            } else {
                Log.w("MAP_INCIDENTS", "Respuesta no exitosa al obtener incidentes remotos: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("MAP_INCIDENTS", "Error al cargar incidentes remotos: ${e.message}", e)
        }
    }

    // ZONAS SEGURAS EN SEGUNDO PLANO Y PERSISTENCIA LOCAL EN ROOM
    // 1. Carga inmediata desde la base de datos local Room
    LaunchedEffect(Unit) {
        try {
            val database = EvacuAppDatabase.getInstance(context)
            val localZones = database.safeZoneDao().getAllSafeZones()
            if (localZones.isNotEmpty()) {
                safeZones = localZones.map { entity ->
                    SafeZoneNearbyDto(
                        id = entity.id.toString(),
                        name = entity.name,
                        description = entity.description,
                        capacity = entity.capacity,
                        active = true,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        distance_meters = null
                    )
                }
                Log.d("SAFE_ZONES", "Zonas seguras cargadas de Room local: ${safeZones.size}")
            }
        } catch (e: Exception) {
            Log.e("SAFE_ZONES", "Error al leer zonas seguras desde Room: ${e.message}")
        }
    }

    // 2. Consulta a la API en segundo plano con persistencia a Room DB
    LaunchedEffect(currentLatitude, currentLongitude) {
        val lat = currentLatitude
        val lng = currentLongitude
        if (lat != null && lng != null) {
            try {
                val response = RetrofitClient.safeZonesApi.getNearbySafeZones(lat, lng, 30000.0)
                if (response.isSuccessful) {
                    val apiList = response.body().orEmpty()
                    if (apiList.isNotEmpty()) {
                        safeZones = apiList
                        Log.d("SAFE_ZONES", "Zonas seguras actualizadas de API: ${safeZones.size}")

                        coroutineScope.launch(Dispatchers.IO) {
                            val database = EvacuAppDatabase.getInstance(context)
                            val entities = apiList.map { dto ->
                                SafeZoneEntity(
                                    name = dto.name,
                                    description = dto.description ?: "Zona segura",
                                    capacity = dto.capacity ?: 0,
                                    latitude = dto.latitude,
                                    longitude = dto.longitude
                                )
                            }
                            database.safeZoneDao().insertAll(entities)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SAFE_ZONES", "Sin red para zonas seguras. Mantiene Room: ${e.message}")
            }
        }
    }

    fun findRecommendedSafeZone(userPoint: GeoPoint): RecommendedSafeZone? {
        if (safeZones.isEmpty()) return null
        val nearest = safeZones.minByOrNull { zone ->
            userPoint.distanceToAsDouble(GeoPoint(zone.latitude, zone.longitude))
        } ?: return null

        return RecommendedSafeZone(
            point = GeoPoint(nearest.latitude, nearest.longitude),
            name = nearest.name
        )
    }

    fun calculateRouteToPoint(
        targetPoint: GeoPoint,
        targetName: String = "",
        automatic: Boolean = false,
        isRerouting: Boolean = false
    ) {
        val startLat = currentLatitude
        val startLon = currentLongitude
        if (startLat == null || startLon == null) {
            if (!isRerouting) Toast.makeText(context, "Esperando señal GPS...", Toast.LENGTH_SHORT).show()
            return
        }

        isCalculatingRoute = true
        coroutineScope.launch {
            val startPoint = GeoPoint(startLat, startLon)
            lastRouteCalcPoint = startPoint

            var routePoints = emptyList<GeoPoint>()
            var steps = emptyList<StepInstruction>()
            var alternatives = emptyList<LocalRouteResult>()

            val originCoord = startPoint.toRouteCoordinate()
            val destCoord = targetPoint.toRouteCoordinate()

            // 1. INTENTO ONLINE: OSRM (Ruta real calle por calle en OpenStreetMap)
            try {
                val osrmResult = OsrmRoutingService.fetchRealStreetRoute(
                    start = startPoint,
                    end = targetPoint,
                    profile = "Vehículo"
                )
                if (osrmResult.points.isNotEmpty()) {
                    routePoints = osrmResult.points
                    steps = osrmResult.steps
                    Log.d("MAP_ROUTE_OSRM", "Ruta OSRM online generada calle por calle (${osrmResult.points.size} puntos)")
                }
            } catch (e: Exception) {
                Log.w("MAP_ROUTE", "Sin internet o fallo OSRM, usando motor de rutas local: ${e.message}")
            }

            // 2. CALCULO DE ALTERNATIVAS LOCALES (Grafo / CostModel / Opciones)
            try {
                LocalRouteEngine.initialize(context)
                val computedAlternatives = LocalRouteEngine.calculateRouteAlternatives(
                    origin = originCoord,
                    destination = destCoord,
                    profile = RouteMobilityProfile.VEHICLE
                )

                if (computedAlternatives.isNotEmpty()) {
                    // Si OSRM trajo el trazado real calle por calle, integra esos puntos en la opción Principal
                    alternatives = if (routePoints.isNotEmpty()) {
                        computedAlternatives.map { variant ->
                            if (variant.variant == RouteVariant.PRINCIPAL) {
                                variant.copy(points = routePoints.map { it.toRouteCoordinate() })
                            } else {
                                variant
                            }
                        }
                    } else {
                        computedAlternatives
                    }

                    val bestVariant = selectedRouteVariant
                        ?.let { sel -> alternatives.find { it.variant == sel.variant } }
                        ?: alternatives.firstOrNull { it.variant == RouteVariant.PRINCIPAL }
                        ?: alternatives.first()

                    selectedRouteVariant = bestVariant

                    if (routePoints.isEmpty()) {
                        routePoints = bestVariant.points.map { it.toGeoPoint() }
                    }
                }
            } catch (e: Exception) {
                Log.e("MAP_ROUTE_OFFLINE", "Error al calcular alternativas locales: ${e.message}")
            }

            // 3. RESPALDO EN LÍNEA INTERMEDIA SI AÚN ESTÁ VACÍA
            if (routePoints.isEmpty()) {
                val fallbackRoute = mutableListOf<GeoPoint>()
                fallbackRoute.add(startPoint)
                val midLat = (startPoint.latitude + targetPoint.latitude) / 2 + 0.0004
                val midLon = (startPoint.longitude + targetPoint.longitude) / 2 - 0.0004
                fallbackRoute.add(GeoPoint(midLat, midLon))
                fallbackRoute.add(targetPoint)
                routePoints = fallbackRoute
            }

            if (routePoints.isNotEmpty()) {
                customDestination = targetPoint
                customDestinationName = targetName.ifBlank { "Zona de Emergencia" }
                customRoutePoints = routePoints
                routeAlternatives = alternatives
                routeSteps = steps
                isAutomaticEvacuation = automatic
            } else {
                if (!isRerouting) Toast.makeText(context, "No se pudo generar la ruta", Toast.LENGTH_SHORT).show()
            }
            isCalculatingRoute = false
        }
    }

    // DETECCIÓN DE INCIDENTES BLOQUEANTES EN LA RUTA ACTUAL (Desvío automático)
    LaunchedEffect(sharedIncidents, customRoutePoints) {
        if (customDestination != null && customRoutePoints.isNotEmpty() && !isCalculatingRoute) {
            val blockingIncident = findBlockingIncidentOnRoute(customRoutePoints, sharedIncidents)
            if (blockingIncident != null) {
                Log.w("MAP_DESVIO", "¡Incidente bloqueante detectado en la ruta! (${blockingIncident.type.displayName}). Desviando...")
                Toast.makeText(
                    context,
                    "⚠️ Incidente reportado en tu ruta (${blockingIncident.type.displayName}). Recalculando desvío...",
                    Toast.LENGTH_LONG
                ).show()

                calculateRouteToPoint(
                    targetPoint = customDestination!!,
                    targetName = customDestinationName,
                    automatic = isAutomaticEvacuation,
                    isRerouting = true
                )
            }
        }
    }

    fun loadPointsOfInterest(type: String? = null) {
        val lat = currentLatitude
        val lng = currentLongitude
        if (lat != null && lng != null) {
            isFetchingPois = true
            coroutineScope.launch {
                try {
                    val response = RetrofitClient.pointsOfInterestApi.getNearbyPointsOfInterest(
                        lat = lat,
                        lng = lng,
                        radius = 30000.0,
                        type = type
                    )

                    if (response.isSuccessful) {
                        val list = response.body() ?: emptyList()
                        pointsOfInterest = list
                        if (list.isEmpty()) {
                            Toast.makeText(context, "No se encontraron elementos cercanos", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("POIS", "Error al obtener puntos: ${e.message}")
                } finally {
                    isFetchingPois = false
                }
            }
        }
    }

    LaunchedEffect(currentLatitude, currentLongitude) {
        if (currentLatitude != null && currentLongitude != null) {
            loadPointsOfInterest(selectedPoiType)
        }
    }

    fun stopNavigation() {
        isNavigating = false
        currentStepIndex = 0
        customDestination = null
        customDestinationName = ""
        customRoutePoints = emptyList()
        routeAlternatives = emptyList()
        selectedRouteVariant = null
        routeSteps = emptyList()
        customDistanceText = ""
        customDurationText = ""
        isAutomaticEvacuation = false
        lastRouteCalcPoint = null
        CustomVoicePlayer.stop()
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
            Toast.makeText(context, "⚠️ No hay zonas seguras registradas", Toast.LENGTH_LONG).show()
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
                    if (location.hasBearing()) {
                        UserLocationState.currentBearing = location.bearing
                    }

                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    UserLocationState.currentLocation = newGeoPoint

                    // 1. RECÁLCULO DINÁMICO EN MOVIMIENTO (> 25 METROS)
                    if (isNavigating && customDestination != null && !isCalculatingRoute) {
                        val lastCalc = lastRouteCalcPoint
                        val distMoved = if (lastCalc != null) newGeoPoint.distanceToAsDouble(lastCalc) else Double.MAX_VALUE

                        if (distMoved >= 25.0) {
                            Log.d("MAP_REROUTE", "Usuario se desplazó ${distMoved.toInt()}m (>25m). Recalculando ruta en movimiento...")
                            calculateRouteToPoint(
                                targetPoint = customDestination!!,
                                targetName = customDestinationName,
                                automatic = isAutomaticEvacuation,
                                isRerouting = true
                            )
                        }
                    }

                    // 2. GUÍA VOCAL DE PASOS DE NAVEGACIÓN
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
                        if (location.hasBearing()) UserLocationState.currentBearing = location.bearing
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
            routeAlternatives = routeAlternatives,
            selectedRouteVariant = selectedRouteVariant,
            incidents = sharedIncidents,
            safeZones = safeZones,
            pointsOfInterest = pointsOfInterest,
            onPoiSelected = { poi ->
                if (!isNavigating) {
                    isTrackingUser = false
                    calculateRouteToPoint(
                        targetPoint = GeoPoint(poi.latitude, poi.longitude),
                        targetName = poi.name
                    )
                }
            },
            onSafeZoneSelected = { point: GeoPoint, name: String ->
                if (!isNavigating) {
                    isTrackingUser = false
                    calculateRouteToPoint(targetPoint = point, targetName = name)
                }
            },
            onRouteVariantSelected = { variant ->
                selectedRouteVariant = variant
                customRoutePoints = variant.points.map { it.toGeoPoint() }
            },
            onMapTouched = { isTrackingUser = false },
            onMapLongClick = { point: GeoPoint ->
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

        // BOTÓN FLOTANTE SOS / MESHTATIC
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

        // BANNER DE INSTRUCCIÓN AL NAVEGAR O FILTROS SUPERIORES
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
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

                // BARRA HORIZONTAL DE FILTROS DE PUNTOS DE INTERÉS
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedPoiType == null,
                            onClick = {
                                selectedPoiType = null
                                loadPointsOfInterest(null)
                            },
                            label = { Text("Todos") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceWhite,
                                selectedContainerColor = EvacuBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPoiType == "FIRE_STATION",
                            onClick = {
                                selectedPoiType = "FIRE_STATION"
                                loadPointsOfInterest("FIRE_STATION")
                            },
                            label = { Text("🚒 Bomberos") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceWhite,
                                selectedContainerColor = EvacuBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPoiType == "HEALTH_CENTER",
                            onClick = {
                                selectedPoiType = "HEALTH_CENTER"
                                loadPointsOfInterest("HEALTH_CENTER")
                            },
                            label = { Text("🏥 CESFAM") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceWhite,
                                selectedContainerColor = EvacuBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPoiType == "POLICE",
                            onClick = {
                                selectedPoiType = "POLICE"
                                loadPointsOfInterest("POLICE")
                            },
                            label = { Text("👮 Policía") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceWhite,
                                selectedContainerColor = EvacuBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (isCalculatingRoute) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = EvacuBlue
            )
        }

        // PANEL INFERIOR CON SELECCIÓN DE RUTAS Y ACCIÓN DE EVACUACIÓN
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Panel de selección de alternativas de ruta (Ruta Rápida, Segura, Accesible)
            if (routeAlternatives.isNotEmpty() && !isNavigating) {
                RouteOptionsPanel(
                    routes = routeAlternatives,
                    selectedRoute = selectedRouteVariant,
                    onRouteSelected = { variant ->
                        selectedRouteVariant = variant
                        customRoutePoints = variant.points.map { it.toGeoPoint() }
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = DangerRed)
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
}
