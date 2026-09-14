package com.example.proyecto_evacuapp.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import com.example.proyecto_evacuapp.R
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.proyecto_evacuapp.data.remote.PointOfInterestResponse
import com.example.proyecto_evacuapp.data.remote.SafeZoneNearbyDto
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapViewOSM(
    modifier: Modifier = Modifier,
    latitude: Double?,
    longitude: Double?,
    isTrackingUser: Boolean = true,
    zoomLevel: Double = 16.5,
    recenterTrigger: Int = 0,
    overviewTrigger: Int = 0,
    destinationPoint: GeoPoint? = null,
    routePoints: List<GeoPoint> = emptyList(),
    safeZones: List<SafeZoneNearbyDto> = emptyList(),
    pointsOfInterest: List<PointOfInterestResponse> = emptyList(), // 👈 Añadido
    onPoiSelected: (PointOfInterestResponse) -> Unit = {},         // 👈 Añadido
    onSafeZoneSelected: (GeoPoint, String) -> Unit = { _, _ -> },
    onMapTouched: () -> Unit = {},
    onMapLongClick: (GeoPoint) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        Configuration.getInstance().userAgentValue = "EvacuApp-UBO-StudentProject/1.0 (${context.packageName})"
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(zoomLevel)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    onMapTouched()
                }
                false
            }
        }
    }

    // Overlay para capturar toques largos en el mapa
    val eventsOverlay = remember(mapView) {
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onMapTouched()
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                onMapLongClick(p)
                return true
            }
        }
        MapEventsOverlay(receiver).also {
            mapView.overlays.add(it)
        }
    }

    // Capa independiente para pintar las Zonas Seguras
    val safeZonesOverlay = remember(mapView) {
        FolderOverlay().also {
            mapView.overlays.add(it)
        }
    }

    // Capa independiente para pintar los Puntos de Interés
    val poiOverlay = remember(mapView) {
        FolderOverlay().also {
            mapView.overlays.add(it)
        }
    }

    // Polilínea para trazar la ruta
    val routePolyline = remember(mapView) {
        Polyline(mapView).apply {
            outlinePaint.strokeWidth = 14f
            outlinePaint.color = AndroidColor.parseColor("#108981")
            mapView.overlays.add(this)
        }
    }

    // Marcador de Ubicación del Usuario
    val userMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Tu ubicación actual"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)
            setInfoWindow(null)
            mapView.overlays.add(this)
        }
    }

    // Marcador de Destino
    val destinationMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destino Seleccionado"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass)
            mapView.overlays.add(this)
        }
    }

    // Actualizar Ubicación de Usuario
    LaunchedEffect(latitude, longitude, isTrackingUser) {
        if (latitude != null && longitude != null) {
            val userLocation = GeoPoint(latitude, longitude)
            userMarker.position = userLocation
            userMarker.isEnabled = true

            if (isTrackingUser) {
                mapView.controller.animateTo(userLocation, zoomLevel, 800L)
            }
            mapView.invalidate()
        }
    }

    // Re-centrar Cámara
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && latitude != null && longitude != null) {
            val userLocation = GeoPoint(latitude, longitude)
            mapView.controller.animateTo(userLocation, zoomLevel, 800L)
            mapView.invalidate()
        }
    }

    // Renderizar Zonas Seguras desde la API
    LaunchedEffect(safeZones) {
        safeZonesOverlay.items.clear()
        safeZones.forEach { zone ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(zone.latitude, zone.longitude)
                title = zone.name
                snippet = "${zone.description ?: "Zona segura"}\nCapacidad: ${zone.capacity ?: "N/A"}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                icon = ContextCompat.getDrawable(context, R.drawable.ic_safe_zone)

                setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    onSafeZoneSelected(m.position, m.title ?: "")
                    true
                }
            }
            safeZonesOverlay.add(marker)
        }
        mapView.invalidate()
    }

    // Renderizar Puntos de Interés desde la API
    LaunchedEffect(pointsOfInterest) {
        poiOverlay.items.clear()
        pointsOfInterest.forEach { poi ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(poi.latitude, poi.longitude)
                title = poi.name
                snippet = "Tipo: ${poi.type} - ${poi.address ?: "Sin dirección"}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                val drawableRes = when (poi.type.uppercase()) {
                    "FIRE_STATION" -> R.drawable.ic_fire_station   // Tu PNG de Bomberos
                    "HEALTH_CENTER" -> R.drawable.ic_health_center // Tu PNG de CESFAM/Salud
                    "POLICE", "POLICE_STATION" -> R.drawable.ic_police // Tu PNG de Policía
                    else -> R.drawable.ic_safe_zone
                }
                icon = ContextCompat.getDrawable(context, drawableRes)

                setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    onPoiSelected(poi)
                    true
                }
            }
            poiOverlay.add(marker)
        }
        mapView.invalidate()
    }

    // Actualizar Ruta y Destino
    LaunchedEffect(destinationPoint, routePoints) {
        if (destinationPoint != null) {
            destinationMarker.position = destinationPoint
            destinationMarker.setVisible(true)
            routePolyline.setPoints(routePoints)
        } else {
            destinationMarker.setVisible(false)
            routePolyline.setPoints(emptyList())
        }
        mapView.invalidate()
    }

    // Vista General de la Ruta
    LaunchedEffect(overviewTrigger) {
        if (overviewTrigger > 0 && routePoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(routePoints)
            mapView.zoomToBoundingBox(boundingBox, true, 120)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    )
}