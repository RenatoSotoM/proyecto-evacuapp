package com.example.proyecto_evacuapp.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.graphics.Color
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.cachemanager.CacheManager
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
    incidents: List<SharedIncident> = emptyList(),
    safeZones: List<SafeZoneNearbyDto> = emptyList(),
    pointsOfInterest: List<PointOfInterestResponse> = emptyList(),
    onPoiSelected: (PointOfInterestResponse) -> Unit = {},
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

    // Capa independiente para pintar Incidentes
    val incidentsOverlay = remember(mapView) {
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

    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            precargarMapaLocal(mapView, latitude, longitude)
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
                    "FIRE_STATION" -> R.drawable.ic_fire_station
                    "HEALTH_CENTER" -> R.drawable.ic_health_center
                    "POLICE", "POLICE_STATION" -> R.drawable.ic_police
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

    // Renderizar Incidentes en el mapa
    LaunchedEffect(incidents) {
        incidentsOverlay.items.clear()
        incidents.forEach { incident ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(incident.latitude, incident.longitude)
                title = "${incident.type.emoji} ${incident.type.displayName}"
                snippet = buildString {
                    append("Confianza: ${incident.confidencePercentage}% (α: ${incident.alpha.toInt()}, β: ${incident.beta.toInt()})")
                    append("\nEstado: ${incident.status.name}")
                    append("\nSeveridad: ${incident.severity.name}")
                    if (incident.description.isNotBlank()) {
                        append("\n${incident.description}")
                    }
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = createIncidentMarkerBitmap(context, incident)
            }
            incidentsOverlay.add(marker)
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

private fun createIncidentMarkerBitmap(context: Context, incident: SharedIncident): Drawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (56 * density).toInt()
    val heightPx = (68 * density).toInt()

    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val statusColorInt = when (incident.status) {
        IncidentStatus.VERIFIED -> AndroidColor.parseColor("#16A34A")
        IncidentStatus.PROBABLE -> AndroidColor.parseColor("#F59E0B")
        IncidentStatus.LOCAL_PENDING, IncidentStatus.PENDING -> AndroidColor.parseColor("#2563EB")
        IncidentStatus.REJECTED, IncidentStatus.SYNC_FAILED -> AndroidColor.parseColor("#DC2626")
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    val centerX = widthPx / 2f
    val centerY = (widthPx / 2f) + (2 * density)
    val radius = (widthPx / 2f) - (2 * density)

    paint.color = statusColorInt
    canvas.drawCircle(centerX, centerY, radius, paint)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(centerX, centerY, radius - (4 * density), paint)

    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 20f * density
    val emojiText = incident.type.emoji
    val textY = centerY - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(emojiText, centerX, textY, paint)

    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = statusColorInt
        style = Paint.Style.FILL
    }
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 9.5f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val badgeText = "${incident.confidencePercentage}%"
    val badgeWidth = (34 * density)
    val badgeHeight = (15 * density)
    val badgeRect = RectF(
        centerX - (badgeWidth / 2f),
        heightPx - badgeHeight,
        centerX + (badgeWidth / 2f),
        heightPx.toFloat()
    )
    canvas.drawRoundRect(badgeRect, 6 * density, 6 * density, badgePaint)
    canvas.drawText(badgeText, centerX, heightPx - (2.5f * density), badgeTextPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun precargarMapaLocal(mapView: MapView, currentLat: Double, currentLng: Double) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val cacheManager = CacheManager(mapView)

            val deltaLat = 0.3
            val deltaLng = 0.35

            val box = BoundingBox(
                currentLat + deltaLat,
                currentLng + deltaLng,
                currentLat - deltaLat,
                currentLng - deltaLng
            )

            cacheManager.downloadAreaAsync(mapView.context, box, 13, 16, null)

            android.util.Log.d("OFFLINE_MAP", "Proceso de descarga offline de 30km iniciado.")
        } catch (e: Exception) {
            android.util.Log.e("OFFLINE_MAP", "Error al iniciar caché offline: ${e.message}")
        }
    }
}

fun dibujarZonaAfectada(mapView: MapView, puntos: List<GeoPoint>) {
    if (puntos.isEmpty()) return

    mapView.overlays.removeAll { overlay: Overlay ->
        overlay is Polygon && overlay.title == "ZONA_EMERGENCIA"
    }

    val polygonOverlay = Polygon(mapView).apply {
        title = "ZONA_EMERGENCIA"
        points = puntos

        fillPaint.color = Color.argb(60, 255, 0, 0)
        outlinePaint.color = Color.RED
        outlinePaint.strokeWidth = 4f
    }

    mapView.overlays.add(polygonOverlay)
    mapView.invalidate()
}
