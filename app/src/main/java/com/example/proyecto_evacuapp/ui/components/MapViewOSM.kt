package com.example.proyecto_evacuapp.ui.components
/* CAMBIOS BASE 1 DESEPTIEMBRE */
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

@Composable
fun MapViewOSM(
    modifier: Modifier = Modifier,
    latitude: Double = -33.5951,
    longitude: Double = -70.7022,
    zoomLevel: Double = 15.5
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        // Asegurar que el UserAgent se registre antes de solicitar mosaicos
        Configuration.getInstance().userAgentValue = "EvacuApp-UBO-StudentProject/1.0 (${context.packageName})"

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(zoomLevel)
            controller.setCenter(GeoPoint(latitude, longitude))

            overlays.clear()

            // Marcador de usuario
            val userMarker = Marker(this).apply {
                position = GeoPoint(latitude, longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Tu ubicación actual"
                setInfoWindow(null)
            }
            overlays.add(userMarker)

            // Marcador de zona segura
            val safeZoneMarker = Marker(this).apply {
                position = GeoPoint(-33.5890, -70.6970)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Zona Segura: Parque García de la Huerta"
                setInfoWindow(null)
            }
            overlays.add(safeZoneMarker)

            // Marcador de incidente
            val hazardMarker = Marker(this).apply {
                position = GeoPoint(-33.5920, -70.7010)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Incidente: Bloqueo de vía"
                setInfoWindow(null)
            }
            overlays.add(hazardMarker)

            invalidate()
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