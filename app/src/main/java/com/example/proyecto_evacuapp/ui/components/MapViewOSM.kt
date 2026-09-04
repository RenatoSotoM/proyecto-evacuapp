package com.example.proyecto_evacuapp.ui.components
// Centralizado funcional en ruta
import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapViewOSM(
    modifier: Modifier = Modifier,
    latitude: Double?,
    longitude: Double?,
    isTrackingUser: Boolean = true,
    onMapTouched: () -> Unit = {},
    zoomLevel: Double = 16.5,
    recenterTrigger: Int = 0
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val (mapView, userMarker) = remember {
        Configuration.getInstance().userAgentValue = "EvacuApp-UBO-StudentProject/1.0 (${context.packageName})"
        val map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(zoomLevel)
            overlays.clear()

            // 🟢 INTERCEPTA EL GESTO TÁCTIL PARA APAGAR EL SEGUIMIENTO AUTOMÁTICO
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    onMapTouched()
                }
                false // Retorna false para que osmdroid siga procesando los gestos de arrastrar y zoom
            }
        }

        val marker = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Tu ubicación actual"
            setInfoWindow(null)
        }
        map.overlays.add(marker)

        Pair(map, marker)
    }

    // 🟢 Actualiza la posición del marcador siempre, pero solo mueve la cámara si isTrackingUser es true
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

    // 🟢 Fuerza el recentrado de la cámara cuando se presiona explícitamente el botón (recenterTrigger)
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0 && latitude != null && longitude != null) {
            val userLocation = GeoPoint(latitude, longitude)
            mapView.controller.animateTo(userLocation, zoomLevel, 800L)
            mapView.invalidate()
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