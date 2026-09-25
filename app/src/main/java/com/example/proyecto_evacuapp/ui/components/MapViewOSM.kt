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
    routeAlternatives: List<LocalRouteResult> = emptyList(),
    selectedRouteVariant: LocalRouteResult? = null,
    incidents: List<SharedIncident> = emptyList(),
    safeZones: List<SafeZoneNearbyDto> = emptyList(),
    pointsOfInterest: List<PointOfInterestResponse> = emptyList(),
    onPoiSelected: (PointOfInterestResponse) -> Unit = {},
    onSafeZoneSelected: (GeoPoint, String) -> Unit = { _, _ -> },
    onRouteVariantSelected: (LocalRouteResult) -> Unit = {},
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

    // Overlay para capturar toques en el mapa
    remember(mapView) {
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

    val safeZonesOverlay = remember(mapView) {
        FolderOverlay().also { mapView.overlays.add(it) }
    }

    val poiOverlay = remember(mapView) {
        FolderOverlay().also { mapView.overlays.add(it) }
    }

    val incidentsOverlay = remember(mapView) {
        FolderOverlay().also { mapView.overlays.add(it) }
    }

    val routePolylinesOverlay = remember(mapView) {
        FolderOverlay().also { mapView.overlays.add(it) }
    }

    val routePolyline = remember(mapView) {
        Polyline(mapView).apply {
            outlinePaint.strokeWidth = 14f
            outlinePaint.color = AndroidColor.parseColor("#108981")
            mapView.overlays.add(this)
        }
    }

    val userMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Tu ubicación actual"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_myplaces)
            setInfoWindow(null)
            mapView.overlays.add(this)
        }
    }

    val destinationMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Destino Seleccionado"
            icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass)
            mapView.overlays.add(this)
        }
    }

    // Actualizar Ubicación de Usuario y Orientación por Brújula
    LaunchedEffect(latitude, longitude, isTrackingUser, UserLocationState.currentBearing) {
        if (latitude != null && longitude != null) {
            val userLocation = GeoPoint(latitude, longitude)
            userMarker.position = userLocation
            userMarker.rotation = -(UserLocationState.currentBearing ?: 0f)
            userMarker.isEnabled = true

            if (isTrackingUser) {
                mapView.controller.setCenter(userLocation)
            }
            mapView.invalidate()
        }
    }

    // Re-centrar Cámara
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && latitude != null && longitude != null) {
            val userLocation = GeoPoint(latitude, longitude)
            mapView.controller.animateTo(userLocation, zoomLevel, 500L)
            mapView.invalidate()
        }
    }

    // Renderizar Zonas Seguras
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

    // Renderizar Puntos de Interés
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

    // Renderizar Incidentes y Radio de Impacto
    LaunchedEffect(incidents) {
        incidentsOverlay.items.clear()
        incidents.forEach { incident ->
            val incPoint = GeoPoint(incident.latitude, incident.longitude)

            val impactRadiusMeters = when (incident.severity) {
                IncidentSeverity.CRITICA, IncidentSeverity.ALTA -> 100.0
                IncidentSeverity.MEDIA -> 50.0
                IncidentSeverity.BAJA -> 25.0
            }
            val circlePoints = Polygon.pointsAsCircle(incPoint, impactRadiusMeters)
            val circleOverlay = Polygon(mapView).apply {
                points = circlePoints
                fillPaint.color = AndroidColor.argb(45, 220, 38, 38)
                outlinePaint.color = AndroidColor.parseColor("#DC2626")
                outlinePaint.strokeWidth = 3f
            }
            incidentsOverlay.add(circleOverlay)

            val marker = Marker(mapView).apply {
                position = incPoint
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

    // Actualizar Rutas
    LaunchedEffect(destinationPoint, routePoints, routeAlternatives, selectedRouteVariant) {
        routePolylinesOverlay.items.clear()
        routePolyline.setPoints(emptyList())

        if (routeAlternatives.isNotEmpty()) {
            routePolyline.setVisible(false)
            val polylines = RoutePolylineFactory.buildPolylines(
                routes = routeAlternatives,
                selectedRoute = selectedRouteVariant,
                onAlternateClicked = onRouteVariantSelected
            )
            polylines.forEach { routePolylinesOverlay.add(it) }
        } else if (routePoints.isNotEmpty()) {
            routePolyline.setPoints(routePoints)
            routePolyline.setVisible(true)
        } else {
            routePolyline.setVisible(false)
        }

        if (destinationPoint != null) {
            destinationMarker.position = destinationPoint
            destinationMarker.setVisible(true)
        } else {
            destinationMarker.setVisible(false)
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
