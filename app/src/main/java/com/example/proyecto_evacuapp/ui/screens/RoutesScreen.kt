package com.example.proyecto_evacuapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentType
import com.example.proyecto_evacuapp.ui.components.OsrmRoutingService
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.example.proyecto_evacuapp.ui.theme.AppBackground
import com.example.proyecto_evacuapp.ui.theme.DangerRed
import com.example.proyecto_evacuapp.ui.theme.DangerRedLight
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import com.example.proyecto_evacuapp.ui.theme.WarningAmber
import com.example.proyecto_evacuapp.ui.theme.WarningAmberLight
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Modelo para Zonas Seguras reportadas (marcas reales)
data class SafeZone(
    val id: String,
    val name: String,
    val location: GeoPoint,
    val isVerifiedByAdmin: Boolean = true,
    val description: String = ""
)

data class RealStreetRoute(
    val id: Int,
    val title: String,
    val destination: String,
    val distance: String,
    val estimatedTime: String,
    val riskLevel: String,
    val riskColor: Color,
    val riskBg: Color,
    val routeLineColor: Color,
    val criteriaReason: String,
    val startPoint: GeoPoint,
    val endPoint: GeoPoint,
    val polylinePoints: List<GeoPoint> = emptyList(),
    val isLoading: Boolean = true,
    val routingError: String? = null
)

@Composable
fun RoutesScreen(
    mobilityMode: String,
    availableSafeZones: List<SafeZone> = emptyList(),
    onSelectRoute: () -> Unit,
    onChangeMobility: (String) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 1. Lectura del GPS en tiempo real para esta pantalla
    var liveUserLocation by remember { mutableStateOf<GeoPoint?>(UserLocationState.currentLocation) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it }
    }

    // Monitoreo en tiempo real del GPS
    DisposableEffect(hasPermission) {
        if (hasPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        val point = GeoPoint(loc.latitude, loc.longitude)
                        liveUserLocation = point
                        UserLocationState.currentLocation = point
                    }
                }

                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                    .setMinUpdateIntervalMillis(1500L)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { loc ->
                            val point = GeoPoint(loc.latitude, loc.longitude)
                            liveUserLocation = point
                            UserLocationState.currentLocation = point
                        }
                    }
                }

                fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

                onDispose {
                    fusedLocationClient.removeLocationUpdates(callback)
                }
            } catch (e: SecurityException) {
                onDispose { }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            onDispose { }
        }
    }

    // 2. Si el GPS aún no devuelve una coordenada física, muestra pantalla de carga viva
    val currentStartPoint = liveUserLocation ?: UserLocationState.currentLocation

    if (currentStartPoint == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = EvacuBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Obteniendo tu ubicación en tiempo real por GPS...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
        return
    }

    // 3. A partir de aquí, currentStartPoint contiene la posición exacta actual del usuario
    var selectedRouteId by remember { mutableIntStateOf(0) }
    var selectedLayerName by remember { mutableStateOf("Estándar") }

    val mobilityModes = listOf("Vehículo", "A pie", "Bicicleta", "Movilidad reducida")

    val sharedIncidents = IncidentSharedState.incidents
    val verifiedRoadIncidents = sharedIncidents.filter { incident ->
        incident.isVerified && incident.type in setOf(
            IncidentType.BLOQUEO_VIAL,
            IncidentType.INUNDACION,
            IncidentType.DERRUMBE,
            IncidentType.ACCIDENTE,
            IncidentType.RUTA_INACCESIBLE
        )
    }

    val verifiedBlockedSegments = IncidentSharedState.verifiedBlockedSegmentIds()
    val hasVerifiedRoadBlock = verifiedBlockedSegments.isNotEmpty()

    // 4. Zonas Seguras reportadas (marcas reales)
    val safeZonesList = if (availableSafeZones.isNotEmpty()) {
        availableSafeZones
    } else {
        remember {
            listOf(
                SafeZone("sz1", "Zona Segura: Parque O'Higgins", GeoPoint(-33.4638, -70.6610)),
                SafeZone("sz2", "Zona Segura: Explanada Sur", GeoPoint(-33.4730, -70.6580)),
                SafeZone("sz3", "Zona Segura: Plaza Almagro", GeoPoint(-33.4550, -70.6500))
            )
        }
    }

    // Ordenar zonas seguras desde la posición VIVA actual del usuario
    val sortedSafeZones = remember(currentStartPoint, safeZonesList) {
        safeZonesList.sortedBy { zone ->
            currentStartPoint.distanceToAsDouble(zone.location)
        }
    }

    val primaryDestination = sortedSafeZones.firstOrNull() ?: SafeZone("def", "Zona Segura Principal", GeoPoint(-33.4638, -70.6610))
    val secondaryDestination = sortedSafeZones.getOrNull(1) ?: primaryDestination

    // 5. Generar opciones de rutas basadas en el punto VIVO y el destino más cercano
    val baseRoutes = remember(mobilityMode, currentStartPoint, primaryDestination, secondaryDestination, verifiedBlockedSegments) {
        when (mobilityMode) {
            "Vehículo" -> {
                if (hasVerifiedRoadBlock) {
                    listOf(
                        RealStreetRoute(
                            id = 0,
                            title = "Ruta vehicular recomendada (Desvío)",
                            destination = primaryDestination.name,
                            distance = "Calculando...",
                            estimatedTime = "Calculando...",
                            riskLevel = "Bajo",
                            riskColor = SafeGreen,
                            riskBg = SafeGreenLight,
                            routeLineColor = EvacuBlue,
                            criteriaReason = "Calculada dinámicamente desde tu posición actual evitando tramos bloqueados.",
                            startPoint = currentStartPoint,
                            endPoint = primaryDestination.location
                        ),
                        RealStreetRoute(
                            id = 1,
                            title = "Ruta vehicular alternativa",
                            destination = secondaryDestination.name,
                            distance = "Calculando...",
                            estimatedTime = "Calculando...",
                            riskLevel = "Muy bajo",
                            riskColor = SafeGreen,
                            riskBg = SafeGreenLight,
                            routeLineColor = SafeGreen,
                            criteriaReason = "Alternativa secundaria hacia la siguiente marca real reportada.",
                            startPoint = currentStartPoint,
                            endPoint = secondaryDestination.location
                        )
                    )
                } else {
                    listOf(
                        RealStreetRoute(
                            id = 0,
                            title = "Ruta vehicular directa",
                            destination = primaryDestination.name,
                            distance = "Calculando...",
                            estimatedTime = "Calculando...",
                            riskLevel = "Medio",
                            riskColor = WarningAmber,
                            riskBg = WarningAmberLight,
                            routeLineColor = EvacuBlue,
                            criteriaReason = "Ruta directa trazada por calles navegables desde tu posición actual.",
                            startPoint = currentStartPoint,
                            endPoint = primaryDestination.location
                        )
                    )
                }
            }
            "Bicicleta" -> listOf(
                RealStreetRoute(
                    id = 0,
                    title = "Ruta ciclista recomendada",
                    destination = primaryDestination.name,
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = EvacuBlue,
                    criteriaReason = "Ruta adaptada para ciclismo desde donde te encuentres.",
                    startPoint = currentStartPoint,
                    endPoint = primaryDestination.location
                )
            )
            "Movilidad reducida" -> listOf(
                RealStreetRoute(
                    id = 0,
                    title = "Ruta accesible recomendada",
                    destination = primaryDestination.name,
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = SafeGreen,
                    criteriaReason = "Trayecto optimizado sin barreras arquitectónicas conocidas.",
                    startPoint = currentStartPoint,
                    endPoint = primaryDestination.location
                )
            )
            else -> listOf(
                RealStreetRoute(
                    id = 0,
                    title = "Ruta peatonal recomendada",
                    destination = primaryDestination.name,
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = EvacuBlue,
                    criteriaReason = "Ruta a pie adaptada en tiempo real a tu ubicación actual.",
                    startPoint = currentStartPoint,
                    endPoint = primaryDestination.location
                ),
                RealStreetRoute(
                    id = 1,
                    title = "Ruta peatonal alternativa",
                    destination = secondaryDestination.name,
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = SafeGreen,
                    criteriaReason = "Alternativa a pie hacia otra zona de evacuación reportada.",
                    startPoint = currentStartPoint,
                    endPoint = secondaryDestination.location
                )
            )
        }
    }

    var calculatedRoutes by remember(baseRoutes) { mutableStateOf(baseRoutes) }

    LaunchedEffect(baseRoutes.size) {
        if (selectedRouteId >= baseRoutes.size) {
            selectedRouteId = 0
        }
    }

    // Consulta a OSRM con el origen dinámico
    LaunchedEffect(baseRoutes, mobilityMode, verifiedBlockedSegments) {
        calculatedRoutes = baseRoutes.map { route ->
            val result = OsrmRoutingService.fetchRealStreetRoute(
                start = route.startPoint,
                end = route.endPoint,
                profile = mobilityMode
            )

            route.copy(
                polylinePoints = result.points,
                distance = result.distanceText,
                estimatedTime = "${result.durationText} ($mobilityMode)",
                isLoading = false,
                routingError = result.errorMessage
            )
        }
    }

    val activeRoute = calculatedRoutes.getOrElse(selectedRouteId) {
        calculatedRoutes.firstOrNull() ?: baseRoutes.first()
    }

    // Interfaz de Usuario
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rutas de evacuación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Surface(
                    color = EvacuBlueLight,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Modo: $mobilityMode",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EvacuBlue
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                items(mobilityModes) { mode ->
                    val icon = when (mode) {
                        "Vehículo" -> Icons.Default.DirectionsCar
                        "Bicicleta" -> Icons.Default.DirectionsBike
                        "Movilidad reducida" -> Icons.Default.WheelchairPickup
                        else -> Icons.Default.DirectionsWalk
                    }

                    FilterChip(
                        selected = mobilityMode == mode,
                        onClick = {
                            selectedRouteId = 0
                            onChangeMobility(mode)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(text = mode, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EvacuBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }
        }

        ActiveEvacuationRouteWidget(
            route = activeRoute,
            mobilityMode = mobilityMode,
            onStartNavigation = onSelectRoute
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                RealStreetRouteMapOSM(
                    route = activeRoute,
                    layerName = selectedLayerName
                )
            }

            FloatingActionButton(
                onClick = {
                    selectedLayerName = when (selectedLayerName) {
                        "Estándar" -> "Topográfico"
                        "Topográfico" -> "Transporte"
                        else -> "Estándar"
                    }
                },
                containerColor = SurfaceWhite,
                contentColor = EvacuBlue,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Cambiar capa del mapa",
                    modifier = Modifier.size(20.dp)
                )
            }

            if (activeRoute.isLoading) {
                Surface(
                    color = SurfaceWhite.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Calculando ruta por calles reales...",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasVerifiedRoadBlock) DangerRedLight else WarningAmberLight
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasVerifiedRoadBlock) Icons.Default.Warning else Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = if (hasVerifiedRoadBlock) DangerRed else WarningAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasVerifiedRoadBlock) "Incidente verificado: ruta recalculada" else "Sin bloqueos viales verificados",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasVerifiedRoadBlock) DangerRed else WarningAmber
                        )
                    }
                    Surface(
                        color = if (hasVerifiedRoadBlock) DangerRed else WarningAmber,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (hasVerifiedRoadBlock) "${verifiedRoadIncidents.size} bloqueo(s)" else "Sin desvíos",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = if (hasVerifiedRoadBlock) {
                        "El consenso comunitario verificó un incidente y el sistema selecciona rutas alternativas desde tu posición actual."
                    } else {
                        "Los reportes de la comunidad modificarán la ruta cuando alcancen una confianza Beta superior al 75%."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            calculatedRoutes.forEachIndexed { index, route ->
                SelectableRealRouteCard(
                    route = route,
                    isSelected = selectedRouteId == index,
                    onSelect = { selectedRouteId = index }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActiveEvacuationRouteWidget(
    route: RealStreetRoute,
    mobilityMode: String,
    onStartNavigation: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, route.routeLineColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = route.routeLineColor,
                        shape = CircleShape,
                        modifier = Modifier.size(12.dp)
                    ) {}

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "RUTA DE EVACUACIÓN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EvacuBlue
                        )

                        Text(
                            text = route.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Surface(
                    color = route.riskBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Riesgo: ${route.riskLevel}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = route.riskColor
                    )
                }
            }

            Text(
                text = "Destino: ${route.destination}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RouteMetric(label = "DISTANCIA", value = route.distance, modifier = Modifier.weight(1f))
                RouteMetric(label = "TIEMPO EST.", value = route.estimatedTime, modifier = Modifier.weight(1f))
                RouteMetric(label = "MODO", value = mobilityMode, modifier = Modifier.weight(1f))
            }

            if (!route.isLoading) {
                Button(
                    onClick = onStartNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = route.routeLineColor,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "INICIAR EVACUACIÓN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RouteMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = AppBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun RealStreetRouteMapOSM(route: RealStreetRoute, layerName: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val (mapView, polyline, startMarker, endMarker) = remember {
        Configuration.getInstance().userAgentValue =
            "EvacuApp-UBO-StudentProject/1.0 (${context.packageName})"

        val map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.0)
        }

        val start = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Tu ubicación actual"
            setInfoWindow(null)
        }

        val end = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = route.destination
            setInfoWindow(null)
        }

        val line = Polyline(map).apply {
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.strokeWidth = 14f
        }

        map.overlays.add(line)
        map.overlays.add(start)
        map.overlays.add(end)

        arrayOf(map, line, start, end)
    }

    LaunchedEffect(layerName) {
        val map = mapView as MapView
        when (layerName) {
            "Topográfico" -> map.setTileSource(TileSourceFactory.OpenTopo)
            "Transporte" -> map.setTileSource(TileSourceFactory.PUBLIC_TRANSPORT)
            else -> map.setTileSource(TileSourceFactory.MAPNIK)
        }
        map.invalidate()
    }

    LaunchedEffect(route) {
        val map = mapView as MapView
        val line = polyline as Polyline
        val start = startMarker as Marker
        val end = endMarker as Marker

        start.position = route.startPoint
        end.position = route.endPoint
        end.title = route.destination

        if (route.polylinePoints.size >= 2) {
            line.setPoints(route.polylinePoints)
        } else {
            line.setPoints(emptyList())
        }

        line.outlinePaint.color = route.routeLineColor.toArgb()

        val centerLat = (route.startPoint.latitude + route.endPoint.latitude) / 2.0
        val centerLon = (route.startPoint.longitude + route.endPoint.longitude) / 2.0

        map.controller.animateTo(GeoPoint(centerLat, centerLon), 15.8, 400L)
        map.invalidate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val map = mapView as MapView
            when (event) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                Lifecycle.Event.ON_DESTROY -> map.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            (mapView as MapView).onDetach()
        }
    }

    AndroidView(factory = { mapView as MapView }, modifier = Modifier.fillMaxSize())
}

@Composable
fun SelectableRealRouteCard(
    route: RealStreetRoute,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EvacuBlueLight else SurfaceWhite
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) route.routeLineColor else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = route.routeLineColor,
                    shape = CircleShape,
                    modifier = Modifier.size(12.dp)
                ) {}

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = route.destination,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (isSelected) {
                    Surface(
                        color = route.routeLineColor,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "ACTIVA",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${route.estimatedTime} · ${route.distance}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Surface(
                    color = route.riskBg,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Riesgo: ${route.riskLevel}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = route.riskColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = route.criteriaReason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}