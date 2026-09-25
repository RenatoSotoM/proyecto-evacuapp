package com.example.proyecto_evacuapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.proyecto_evacuapp.R
import com.example.proyecto_evacuapp.data.remote.PointOfInterestResponse
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.data.remote.SafeZoneNearbyDto
import com.example.proyecto_evacuapp.data.repository.IncidentRepository
import com.example.proyecto_evacuapp.domain.RouteManager
import com.example.proyecto_evacuapp.domain.engine.DownloadState
import com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine
import com.example.proyecto_evacuapp.domain.engine.MapDownloadManager
import com.example.proyecto_evacuapp.domain.engine.RoutingEngineManager
import com.example.proyecto_evacuapp.ui.components.*
import com.example.proyecto_evacuapp.ui.theme.*
import com.example.proyecto_evacuapp.ui.viewmodel.RoutingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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

    var lastRouteCalcPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var lastNetworkQueryPoint by remember { mutableStateOf<GeoPoint?>(null) }

    // ESTADO DE BÚSQUEDA Y PRUEBAS DEV ONLY
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<com.example.proyecto_evacuapp.data.remote.GeocodingResult>>(emptyList()) }
    var isDevBlockModeEnabled by remember { mutableStateOf(false) }

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
    val routeManager = remember { RouteManager(coroutineScope) }
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

    // VIEWMODEL DE DESCARGA Y PRECARGA DE CARTOGRAFÍA Y RUTEOS
    val routingViewModel: RoutingViewModel = viewModel()
    val mapDownloadState by routingViewModel.downloadState.collectAsState()

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val isDownloaded = MapDownloadManager.isMapDownloaded(context)
            if (isDownloaded) {
                RoutingEngineManager.initializeEngine(context)
            }
        }
    }

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

    // DEBOUNCE PARA BÚSQUEDA DE DIRECCIONES (400ms)
    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length >= 3) {
            kotlinx.coroutines.delay(400L)
            searchResults = com.example.proyecto_evacuapp.data.remote.GeocodingService.searchAddress(
                query = query,
                userLat = currentLatitude,
                userLon = currentLongitude
            )
        } else {
            searchResults = emptyList()
        }
    }

    // GESTIÓN DEL SERVICIO EN PRIMER PLANO DE NAVEGACIÓN GPS (NavigationForegroundService)
    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            val serviceIntent = android.content.Intent(context, com.example.proyecto_evacuapp.services.NavigationForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(android.content.Intent(context, com.example.proyecto_evacuapp.services.NavigationForegroundService::class.java))
        }
    }

    // CONSULTA A API OPTIMIZADA (Sólo al iniciar o al desplazarse > 500m)
    LaunchedEffect(currentLatitude, currentLongitude) {
        val lat = currentLatitude ?: return@LaunchedEffect
        val lon = currentLongitude ?: return@LaunchedEffect
        val userPt = GeoPoint(lat, lon)
        val lastLoc = lastNetworkQueryPoint

        if (lastLoc == null || userPt.distanceToAsDouble(lastLoc) >= 500.0) {
            lastNetworkQueryPoint = userPt

            try {
                val response = IncidentRepository(RetrofitClient.incidentApiService).getIncidents(lat = lat, lng = lon)
                if (response.isSuccessful) {
                    val remoteList = response.body().orEmpty()
                    IncidentSharedState.syncRemoteIncidents(remoteList)
                }
            } catch (e: Exception) {
                Log.w("MAP_INCIDENTS", "Error al cargar incidentes remotos: ${e.message}")
            }

            try {
                val szResponse = RetrofitClient.safeZonesApi.getNearbySafeZones(lat, lon, 30000.0)
                if (szResponse.isSuccessful) {
                    val apiList = szResponse.body().orEmpty()
                    if (apiList.isNotEmpty()) {
                        safeZones = apiList
                    }
                }
            } catch (e: Exception) {
                Log.w("SAFE_ZONES", "Sin red para zonas seguras: ${e.message}")
            }
        }
    }

    // ZONAS SEGURAS EN SEGUNDO PLANO DESDE ROOM LOCAL
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
            }
        } catch (e: Exception) {
            Log.e("SAFE_ZONES", "Error al leer zonas seguras desde Room: ${e.message}")
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

            // 0. ASEGURAR DESCARGA BBOX QUE CUBRA ORIGEN Y DESTINO (30 KM)
            try {
                MapDownloadManager.checkAndDownloadOnStartup(
                    context = context,
                    userLat = startLat,
                    userLng = startLon,
                    destLat = targetPoint.latitude,
                    destLng = targetPoint.longitude
                )
            } catch (e: Exception) {
                Log.w("MAP_ROUTE", "No fue posible verificar/actualizar BBOX dinámico: ${e.message}")
            }

            // 1. INTENTO ONLINE: OSRM (Rutas alternativas calle por calle)
            var osrmAlternatives = emptyList<OsrmRouteResponse>()
            try {
                val currentBearing = UserLocationState.currentBearing
                osrmAlternatives = OsrmRoutingService.fetchRealStreetRouteAlternatives(
                    start = startPoint,
                    end = targetPoint,
                    profile = "Vehículo",
                    bearing = currentBearing,
                    speedMps = currentSpeedMps
                )
                if (osrmAlternatives.isNotEmpty()) {
                    val primaryOsrm = osrmAlternatives.first()
                    val osrmBlockingIncident = findBlockingIncidentOnRoute(primaryOsrm.points, sharedIncidents)
                    if (osrmBlockingIncident == null) {
                        routePoints = primaryOsrm.points
                        steps = primaryOsrm.steps
                    }
                }
            } catch (e: Exception) {
                Log.w("MAP_ROUTE", "Sin internet o fallo OSRM, usando motor de rutas local: ${e.message}")
            }

            // 2. CÁLCULO DE ALTERNATIVAS LOCALES Y ACOPLE DE TRAZADOS DE CALLE REAL
            try {
                LocalRouteEngine.initialize(context)
                val computedAlternatives = LocalRouteEngine.calculateRouteAlternatives(
                    origin = originCoord,
                    destination = destCoord,
                    profile = RouteMobilityProfile.VEHICLE,
                    startBearing = UserLocationState.currentBearing
                )

                if (computedAlternatives.isNotEmpty()) {
                    alternatives = computedAlternatives.mapIndexed { idx, variant ->
                        val matchingOsrm = osrmAlternatives.getOrNull(idx) ?: osrmAlternatives.firstOrNull()
                        if (matchingOsrm != null && matchingOsrm.points.isNotEmpty() && findBlockingIncidentOnRoute(matchingOsrm.points, sharedIncidents) == null) {
                            variant.copy(points = matchingOsrm.points.map { it.toRouteCoordinate() })
                        } else {
                            variant
                        }
                    }

                    val bestVariant = selectedRouteVariant
                        ?.let { sel -> alternatives.find { it.variant == sel.variant } }
                        ?: alternatives.firstOrNull { it.variant == RouteVariant.SEGURA }
                        ?: alternatives.first()

                    selectedRouteVariant = bestVariant
                    routePoints = bestVariant.points.map { it.toGeoPoint() }
                }
            } catch (e: Exception) {
                Log.e("MAP_ROUTE_OFFLINE", "Error al calcular alternativas locales: ${e.message}")
            }

            // 3. PROHIBICIÓN TOTAL DE LÍNEA RECTA: Si no hay ruta en la red vial, se descarta y notifica
            if (routePoints.isEmpty()) {
                if (!isRerouting) Toast.makeText(context, "⚠️ Ruta no encontrada sobre la red vial", Toast.LENGTH_LONG).show()
                routePoints = emptyList()
            }

            if (routePoints.isNotEmpty()) {
                customDestination = targetPoint
                customDestinationName = targetName.ifBlank { "Zona de Emergencia" }
                customRoutePoints = routePoints
                routeAlternatives = alternatives
                routeSteps = steps
                currentStepIndex = 0
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
                    }
                } catch (e: Exception) {
                    Log.e("POIS", "Error al obtener puntos: ${e.message}")
                } finally {
                    isFetchingPois = false
                }
            }
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
                            calculateRouteToPoint(
                                targetPoint = customDestination!!,
                                targetName = customDestinationName,
                                automatic = isAutomaticEvacuation,
                                isRerouting = true
                            )
                        }
                    }

                    // 2. DETECCIÓN DE CAMBIO DE SENTIDO / VIRAGE (> 90° DURANTE > 3 SEGUNDOS)
                    if (isNavigating && customDestination != null && customRoutePoints.isNotEmpty() && !isCalculatingRoute) {
                        val userCoord = newGeoPoint.toRouteCoordinate()
                        val routeCoords = customRoutePoints.map { it.toRouteCoordinate() }

                        routeManager.checkHeadingDeviationAndRecalculate(
                            userPoint = userCoord,
                            userBearing = UserLocationState.currentBearing,
                            activeRoutePoints = routeCoords,
                            onTriggerRecalculate = {
                                Toast.makeText(context, "🔄 Cambio de rumbo detectado. Recalculando ruta...", Toast.LENGTH_SHORT).show()
                                calculateRouteToPoint(
                                    targetPoint = customDestination!!,
                                    targetName = customDestinationName,
                                    automatic = isAutomaticEvacuation,
                                    isRerouting = true
                                )
                            }
                        )
                    }

                    // 3. GUÍA VOCAL, NOTIFICACIÓN Y ASISTENCIA HÁPTICA DE PASOS DE NAVEGACIÓN
                    if (isNavigating && routeSteps.isNotEmpty() && currentStepIndex < routeSteps.size) {
                        val currentStep = routeSteps[currentStepIndex]
                        val distanceToStep = newGeoPoint.distanceToAsDouble(currentStep.location)
                        distanceToNextStepMeters = distanceToStep.toInt()

                        val speedKmH = (currentSpeedMps * 3.6).toInt()

                        com.example.proyecto_evacuapp.services.NavigationForegroundService.updateNavigationProgress(
                            context = context,
                            speedKmH = speedKmH,
                            nextInstruction = currentStep.text,
                            distanceMeters = distanceToNextStepMeters
                        )

                        val triggerDistanceMeters = when {
                            speedKmH >= 60 -> currentSpeedMps * 15.0
                            speedKmH >= 30 -> currentSpeedMps * 12.0
                            else -> 30.0
                        }

                        if (distanceToStep <= triggerDistanceMeters) {
                            val isArriveStep = currentStep.modifier.lowercase().contains("arrive") || currentStepIndex == routeSteps.size - 1

                            if (isArriveStep) {
                                val distToDest = customDestination?.let { newGeoPoint.distanceToAsDouble(it) } ?: distanceToStep
                                if (distToDest <= 15.0) {
                                    val audioRes = CustomVoicePlayer.getAudioForStep(currentStep.modifier)
                                    CustomVoicePlayer.playAudio(context, audioRes)
                                    if (currentStepIndex < routeSteps.size - 1) currentStepIndex++
                                }
                            } else {
                                val modifier = currentStep.modifier.lowercase()
                                when {
                                    modifier.contains("right") -> com.example.proyecto_evacuapp.utils.AdaptiveAssistant.notifyTurnRight(context)
                                    modifier.contains("left") -> com.example.proyecto_evacuapp.utils.AdaptiveAssistant.notifyTurnLeft(context)
                                    else -> {
                                        val audioRes = CustomVoicePlayer.getAudioForStep(currentStep.modifier)
                                        CustomVoicePlayer.playAudio(context, audioRes)
                                    }
                                }
                                if (currentStepIndex < routeSteps.size - 1) currentStepIndex++
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
                if (isDevBlockModeEnabled) {
                    // // DEV ONLY: Insertar un bloqueo de calle en tiempo real
                    val devBlock = SharedIncident(
                        localId = java.util.UUID.randomUUID().toString(),
                        type = IncidentType.BLOQUEO_VIAL,
                        severity = IncidentSeverity.CRITICA,
                        description = "🚧 Bloqueo de vía de prueba (DEV ONLY)",
                        latitude = point.latitude,
                        longitude = point.longitude,
                        alpha = 10.0,
                        beta = 1.0,
                        status = IncidentStatus.VERIFIED,
                        isOwnReport = true
                    )
                    IncidentSharedState.addLocalIncident(devBlock)
                    Toast.makeText(context, "🚧 Bloqueo de vía creado en (${"%.4f".format(point.latitude)}, ${"%.4f".format(point.longitude)})", Toast.LENGTH_SHORT).show()
                } else if (!isNavigating) {
                    isTrackingUser = false
                    calculateRouteToPoint(point)
                }
            }
        )

        // BOTONES FLOTANTES DE CÁMARA Y DEV TOOL
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 240.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // // DEV ONLY: BOTÓN DE HERRAMIENTAS DE PRUEBA DE BLOQUEO EN VIVO
            FloatingActionButton(
                onClick = {
                    isDevBlockModeEnabled = !isDevBlockModeEnabled
                    val msg = if (isDevBlockModeEnabled) "DEV MODE: Toca el mapa para crear un bloqueo en vivo" else "DEV MODE desactivado"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                containerColor = if (isDevBlockModeEnabled) DangerRed else SurfaceWhite,
                contentColor = if (isDevBlockModeEnabled) Color.White else EvacuBlue,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "DEV ONLY: Crear bloqueo en vivo"
                )
            }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConnectivityBadge(
                        text = if (isCalculatingRoute) "Calculando calle..." else "GPS Activo",
                        color = SafeGreen,
                        backgroundColor = SafeGreenLight,
                        icon = Icons.Default.CloudDone
                    )

                    // BANDA DE ESTADO DE MAPA Y BOTÓN DE PRECARGA MANUAL
                    when (val state = mapDownloadState) {
                        is DownloadState.Downloading -> {
                            Surface(
                                color = WarningAmberLight,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = WarningAmber
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Descargando: ${state.progress}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        is DownloadState.Downloaded -> {
                            ConnectivityBadge(
                                text = "Mapa Offline Listo",
                                color = SafeGreen,
                                backgroundColor = SafeGreenLight,
                                icon = Icons.Default.DownloadDone
                            )
                        }
                        else -> {
                            Button(
                                onClick = {
                                    routingViewModel.startFullPreload(
                                        context = context,
                                        lat = currentLatitude ?: -33.4489,
                                        lon = currentLongitude ?: -70.6693
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EvacuBlue),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Precargar Mapa",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // BARRA DE BÚSQUEDA DE DIRECCIONES ("¿Adónde vas?")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("¿Adónde vas? Buscar dirección...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    coroutineScope.launch {
                                        val query = searchQuery.trim()
                                        if (query.isNotEmpty()) {
                                            val results = com.example.proyecto_evacuapp.data.remote.GeocodingService.searchAddress(
                                                query = query,
                                                userLat = currentLatitude,
                                                userLon = currentLongitude
                                            )
                                            if (results.isNotEmpty()) {
                                                val first = results.first()
                                                searchQuery = ""
                                                searchResults = emptyList()
                                                isTrackingUser = false
                                                val destPoint = GeoPoint(first.latitude, first.longitude)
                                                val shortName = first.displayName.split(",").firstOrNull() ?: "Destino"
                                                calculateRouteToPoint(destPoint, shortName)
                                            } else {
                                                Toast.makeText(context, "Ruta no encontrada sobre la red vial o dirección no válida.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EvacuBlue,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        if (searchResults.isNotEmpty()) {
                            Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                                searchResults.forEach { result ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchQuery = ""
                                                searchResults = emptyList()
                                                isTrackingUser = false
                                                val destPoint = GeoPoint(result.latitude, result.longitude)
                                                val shortName = result.displayName.split(",").firstOrNull() ?: "Destino"
                                                calculateRouteToPoint(destPoint, shortName)
                                            }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = EvacuBlue)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = result.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                            maxLines = 2
                                        )
                                    }
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                }
                            }
                        }
                    }
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
