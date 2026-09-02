package com.example.proyecto_evacuapp.ui.screens

import android.graphics.Paint
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.proyecto_evacuapp.ui.theme.AppBackground
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import com.example.proyecto_evacuapp.ui.theme.WarningAmber
import com.example.proyecto_evacuapp.ui.theme.WarningAmberLight
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

data class EmergencyCategory(
    val name: String,
    val emoji: String,
    val historicalRiskFactor: String,
    val adaptiveCriteria: String
)

data class RouteOption(
    val id: Int,
    val title: String,
    val destination: String,
    val distance: String,
    val time: String,
    val risk: String,
    val riskColor: Color,
    val riskBackground: Color,
    val routeColor: Color,
    val reason: String,
    val coordinates: List<GeoPoint>
)

@Composable
fun RoutesScreen(onSelectRoute: () -> Unit) {
    var selectedEmergency by remember { mutableStateOf("Terremoto") }
    var selectedRouteIndex by remember { mutableIntStateOf(0) }

    val categories = remember {
        listOf(
            EmergencyCategory("Terremoto", "🌎", "Historial sísmico SENAPRED: riesgo por caída de muros y cables", "Prioriza vías abiertas evitando pasos bajo nivel"),
            EmergencyCategory("Incendio", "🔥", "Historial CONAF: dispersión rápida de humo en vientos urbanos", "Busca áreas despejadas contra la dirección del viento"),
            EmergencyCategory("Inundación", "🌊", "Historial lluvias Santiago: anegamiento en pasos bajo nivel y calzadas bajas", "Descarta pasos a desnivel y busca zonas elevadas"),
            EmergencyCategory("Tsunami", "🌊", "Planes de evacuación SENAPRED: cota 30 metros sobre nivel del mar", "Evacúa horizontal y verticalmente hacia puntos seguros elevados"),
            EmergencyCategory("Derrumbe", "🪨", "Registros SERNAGEOMIN: inestabilidad de taludes y quebradas", "Evita avenidas adyacentes a pendientes inestables"),
            EmergencyCategory("Otra", "⚠️", "Registro histórico multiamenaza base urbana", "Optimiza por distancia y reporte de incidentes en tiempo real")
        )
    }

    val currentCategory = categories.first { it.name == selectedEmergency }

    // Generador de grafos adaptativos según el tipo de emergencia seleccionada
    val routesList = remember(selectedEmergency) {
        when (selectedEmergency) {
            "Terremoto" -> listOf(
                RouteOption(
                    id = 0,
                    title = "Ruta recomendada (Vía Amplia)",
                    destination = "Explanada Parque O'Higgins",
                    distance = "1,4 km",
                    time = "11 min",
                    risk = "Bajo",
                    riskColor = SafeGreen,
                    riskBackground = SafeGreenLight,
                    routeColor = EvacuBlue,
                    reason = "Vía ancha despejada de cornisas y cableado aéreo según datos históricos sísmicos.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4670, -70.6590),
                        GeoPoint(-33.4655, -70.6592),
                        GeoPoint(-33.4645, -70.6605),
                        GeoPoint(-33.4638, -70.6610)
                    )
                ),
                RouteOption(
                    id = 1,
                    title = "Ruta alternativa segura",
                    destination = "Parque Cousiño / Rondizzoni",
                    distance = "1,8 km",
                    time = "15 min",
                    risk = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBackground = SafeGreenLight,
                    routeColor = SafeGreen,
                    reason = "Circunvalación amplia sin estructuras de altura.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4685, -70.6575),
                        GeoPoint(-33.4690, -70.6610),
                        GeoPoint(-33.4675, -70.6625),
                        GeoPoint(-33.4660, -70.6630)
                    )
                )
            )
            "Inundación" -> listOf(
                RouteOption(
                    id = 0,
                    title = "Ruta adaptada a cota alta",
                    destination = "Zona Elevada: Campus Rondizzoni",
                    distance = "1,1 km",
                    time = "9 min",
                    risk = "Bajo",
                    riskColor = SafeGreen,
                    riskBackground = SafeGreenLight,
                    routeColor = EvacuBlue,
                    reason = "Evita pasos bajo nivel de Av. Matta propensos a anegamiento histórico.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4665, -70.6560),
                        GeoPoint(-33.4650, -70.6555),
                        GeoPoint(-33.4635, -70.6550)
                    )
                ),
                RouteOption(
                    id = 1,
                    title = "Ruta por calles altas",
                    destination = "Punto de Apoyo Beauchef",
                    distance = "1,6 km",
                    time = "13 min",
                    risk = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBackground = SafeGreenLight,
                    routeColor = SafeGreen,
                    reason = "Trazado sobre calzadas elevadas con drenaje operativo.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4680, -70.6590),
                        GeoPoint(-33.4695, -70.6605),
                        GeoPoint(-33.4710, -70.6620)
                    )
                )
            )
            "Incendio" -> listOf(
                RouteOption(
                    id = 0,
                    title = "Ruta contra el viento",
                    destination = "Zona Despejada: Parque O'Higgins",
                    distance = "1,5 km",
                    time = "12 min",
                    risk = "Bajo",
                    riskColor = SafeGreen,
                    riskBackground = SafeGreenLight,
                    routeColor = EvacuBlue,
                    reason = "Grafo recalculado evitando zonas arboladas densas y dirección del humo.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4675, -70.6600),
                        GeoPoint(-33.4660, -70.6620),
                        GeoPoint(-33.4640, -70.6635)
                    )
                ),
                RouteOption(
                    id = 1,
                    title = "Ruta secundaria por autopista",
                    destination = "Acceso Autopista Central",
                    distance = "1,9 km",
                    time = "16 min",
                    risk = "Medio",
                    riskColor = WarningAmber,
                    riskBackground = WarningAmberLight,
                    routeColor = SafeGreen,
                    reason = "Vía ancha con posibilidad de humo residual.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4690, -70.6570),
                        GeoPoint(-33.4710, -70.6565),
                        GeoPoint(-33.4730, -70.6560)
                    )
                )
            )
            else -> listOf(
                RouteOption(
                    id = 0,
                    title = "Ruta estándar recomendada",
                    destination = "Punto de Encuentro Oficial",
                    distance = "1,3 km",
                    time = "10 min",
                    risk = "Bajo",
                    riskColor = SafeGreen,
                    riskBackground = SafeGreenLight,
                    routeColor = EvacuBlue,
                    reason = "Ponderación OSRM según menor tiempo y sin reportes de bloqueo activos.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4670, -70.6590),
                        GeoPoint(-33.4655, -70.6592),
                        GeoPoint(-33.4645, -70.6605)
                    )
                ),
                RouteOption(
                    id = 1,
                    title = "Ruta alternativa de evacuación",
                    destination = "Zona Segura Secundaria",
                    distance = "1,7 km",
                    time = "14 min",
                    risk = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBackground = SafeGreenLight,
                    routeColor = SafeGreen,
                    reason = "Ruta secundaria de contingencia.",
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4685, -70.6575),
                        GeoPoint(-33.4690, -70.6610),
                        GeoPoint(-33.4675, -70.6625)
                    )
                )
            )
        }
    }

    val currentRoute = routesList.getOrElse(selectedRouteIndex) { routesList.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(AppBackground)
    ) {
        // 1. TÍTULO Y SELECTOR DE AMENAZA
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = "Rutas adaptativas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Ponderación vial según amenaza y datos históricos SENAPRED.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Selector horizontal de tipo de emergencia
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedEmergency == category.name,
                        onClick = {
                            selectedEmergency = category.name
                            selectedRouteIndex = 0
                        },
                        label = {
                            Text(
                                text = "${category.emoji} ${category.name}",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EvacuBlue,
                            selectedLabelColor = Color.White,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        }

        // 2. MAPA SUPERIOR OSM CON TRAZADO DE LÍNEA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                RouteMapOSM(route = currentRoute)
            }
        }

        // 3. TARJETA INFORMATIVA DE CRITERIO HISTÓRICO ADAPTATIVO
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EvacuBlueLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = EvacuBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Criterio adaptativo: ${currentCategory.name}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EvacuBlue
                    )
                    Text(
                        text = currentCategory.adaptiveCriteria,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        // 4. LISTA DE ALTERNATIVAS DE RUTA
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            routesList.forEachIndexed { index, route ->
                SelectableRouteCard(
                    route = route,
                    isSelected = selectedRouteIndex == index,
                    onSelect = { selectedRouteIndex = index },
                    onStartNavigation = onSelectRoute
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RouteMapOSM(route: RouteOption) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val (mapView, polyline, startMarker, endMarker) = remember {
        Configuration.getInstance().userAgentValue = "EvacuApp-UBO-StudentProject/1.0 (${context.packageName})"
        val map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.2)
        }

        val start = Marker(map).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Tu ubicación"
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

        listOf(map, line, start, end)
    }

    LaunchedEffect(route) {
        val map = mapView as MapView
        val line = polyline as Polyline
        val start = startMarker as Marker
        val end = endMarker as Marker

        line.setPoints(route.coordinates)
        line.outlinePaint.color = route.routeColor.toArgb()

        if (route.coordinates.isNotEmpty()) {
            start.position = route.coordinates.first()
            end.position = route.coordinates.last()
            end.title = route.destination

            val centerLat = (route.coordinates.first().latitude + route.coordinates.last().latitude) / 2
            val centerLng = (route.coordinates.first().longitude + route.coordinates.last().longitude) / 2
            map.controller.animateTo(GeoPoint(centerLat, centerLng), 16.0, 500L)
        }
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
fun SelectableRouteCard(
    route: RouteOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStartNavigation: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EvacuBlueLight else SurfaceWhite
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) route.routeColor else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = route.routeColor,
                    shape = CircleShape,
                    modifier = Modifier.size(14.dp)
                ) {}

                Spacer(modifier = Modifier.width(10.dp))

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
                        color = route.routeColor,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "SELECCIONADA",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${route.time} · ${route.distance}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Surface(
                    color = route.riskBackground,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Riesgo: ${route.risk}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = route.riskColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = route.reason,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (isSelected) {
                Button(
                    onClick = onStartNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (route.id == 0) EvacuBlue else SafeGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INICIAR EVACUACIÓN",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}