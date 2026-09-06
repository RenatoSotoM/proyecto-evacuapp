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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.proyecto_evacuapp.ui.components.ConnectivityBadge
import com.example.proyecto_evacuapp.ui.components.OsrmRoutingService
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.example.proyecto_evacuapp.ui.theme.DangerRed
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
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

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(onFindRoute: () -> Unit) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    // ESTADOS DE GPS Y SEGUIMIENTO
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

    // ESTADOS PARA LA RUTA PERSONALIZADA VEHICULAR
    var customDestination by remember { mutableStateOf<GeoPoint?>(null) }
    var customRoutePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var customDistanceText by remember { mutableStateOf("") }
    var customDurationText by remember { mutableStateOf("") }
    var isCalculatingRoute by remember { mutableStateOf(false) }

    // Función para calcular la ruta vehicular por calles reales hacia el punto presionado
    fun calculateRouteToPoint(targetPoint: GeoPoint) {
        val startLat = currentLatitude
        val startLon = currentLongitude

        if (startLat == null || startLon == null) {
            Toast.makeText(context, "Esperando señal GPS para trazar la ruta...", Toast.LENGTH_SHORT).show()
            return
        }

        customDestination = targetPoint
        isCalculatingRoute = true

        coroutineScope.launch {
            val startPoint = GeoPoint(startLat, startLon)
            // 🚗 Se consulta con el perfil vehicular para medir calles reales
            val result = OsrmRoutingService.fetchRealStreetRoute(
                start = startPoint,
                end = targetPoint,
                profile = "Vehiculo"
            )

            if (result.points.isNotEmpty()) {
                customRoutePoints = result.points
                customDistanceText = result.distanceText
                customDurationText = result.durationText
            } else {
                Toast.makeText(context, "No se pudo calcular una ruta por calle hacia ese punto", Toast.LENGTH_SHORT).show()
            }
            isCalculatingRoute = false
        }
    }

    fun clearCustomRoute() {
        customDestination = null
        customRoutePoints = emptyList()
        customDistanceText = ""
        customDurationText = ""
    }

    // Suscripción al GPS
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    UserLocationState.currentLocation = GeoPoint(location.latitude, location.longitude)
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
                        recenterTrigger++
                    }
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            requestFreshLocation()
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            requestFreshLocation()
        }
    }

    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2500L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // MAPA INTERACTIVO
        MapViewOSM(
            latitude = currentLatitude,
            longitude = currentLongitude,
            recenterTrigger = recenterTrigger,
            isTrackingUser = isTrackingUser,
            destinationPoint = customDestination,
            routePoints = customRoutePoints,
            onMapTouched = {
                // 🛑 Al mover o tocar el mapa desactiva el seguimiento para permitir movimiento libre
                isTrackingUser = false
            },
            onMapLongClick = { point ->
                isTrackingUser = false
                calculateRouteToPoint(point)
            }
        )

        // BARRA SUPERIOR CON NAVEGACIÓN Y ESTADO DE RED
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
                    text = if (isCalculatingRoute) "Calculando calle..." else "Información actualizada",
                    color = SafeGreen,
                    backgroundColor = SafeGreenLight,
                    icon = Icons.Default.CloudDone
                )
                ConnectivityBadge(
                    text = if (hasLocationPermission) "GPS: activo" else "GPS: sin permiso",
                    color = if (hasLocationPermission) EvacuBlue else Color.Gray,
                    backgroundColor = EvacuBlueLight,
                    icon = Icons.Default.LocationOn
                )
            }
        }

        // INDICADOR DE CARGA CENTRADO AL CALCULAR
        if (isCalculatingRoute) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = EvacuBlue
            )
        }

        // BOTÓN RECENTRAR MAPA (Único evento que vuelve a centrar en la ubicación actual)
        FloatingActionButton(
            onClick = {
                isTrackingUser = true
                if (hasLocationPermission) {
                    requestFreshLocation()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            containerColor = SurfaceWhite,
            contentColor = if (isTrackingUser) EvacuBlue else TextSecondary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 260.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recentrar mapa en mi ubicación actual"
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
                if (customDestination != null) {
                    // 🚗 PANEL CON OPCIONES "IR" Y "CANCELAR VIAJE"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ruta vehicular trazada",
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
                                    text = if (customDistanceText.isNotEmpty()) "$customDistanceText ($customDurationText)" else "Midiendo distancia...",
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
                        // Botón CANCELAR VIAJE
                        OutlinedButton(
                            onClick = { clearCustomRoute() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = DangerRed
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "CANCELAR", fontWeight = FontWeight.Bold)
                        }

                        // Botón IR
                        Button(
                            onClick = {
                                Toast.makeText(context, "Iniciando viaje hacia el destino seleccionado...", Toast.LENGTH_SHORT).show()
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
                    // PANEL POR DEFECTO PARA EVACUACIÓN GENERAL
                    Text(
                        text = "¿Necesitas evacuar?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Mantén presionado cualquier punto del mapa para medir rutas por calle en vehículo o presiona para buscar zonas seguras.",
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
                        Text(
                            text = "ENCONTRAR RUTA SEGURA",
                            fontWeight = FontWeight.Bold
                        )
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
        // ✋ Intercepta los toques/desplazamientos para desactivar inmediatamente el autocentrado
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
            controller.setZoom(16.0)
        }

        val uMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Mi Ubicación"
        }

        val dMarker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destino Seleccionado"
        }

        val polyline = Polyline(map).apply {
            outlinePaint.strokeWidth = 14f
            outlinePaint.color = AndroidColor.parseColor("#10B981") // Color verde para la ruta
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

    // Actualización de ubicación del usuario
    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            val userPoint = GeoPoint(latitude, longitude)
            (userMarker as Marker).apply {
                position = userPoint
                setVisible(true)
            }
            // 📍 Solo centra automáticamente si isTrackingUser es true
            if (isTrackingUser) {
                (mapView as MapView).controller.animateTo(userPoint)
            }
            (mapView as MapView).invalidate()
        }
    }

    // Acción explícita de recentrado por botón
    LaunchedEffect(recenterTrigger) {
        if (latitude != null && longitude != null && recenterTrigger > 0) {
            (mapView as MapView).controller.animateTo(GeoPoint(latitude, longitude))
        }
    }

    // Dibujo del punto de destino y polyline de calles
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