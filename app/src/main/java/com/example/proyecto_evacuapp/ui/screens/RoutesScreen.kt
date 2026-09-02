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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

data class EmergencyThreat(
    val name: String,
    val emoji: String,
    val historicalRiskNote: String
)

data class EvaluatedRoute(
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
    val isBlocked: Boolean,
    val coordinates: List<GeoPoint>
)

@Composable
fun RoutesScreen(
    mobilityMode: String,
    onSelectRoute: () -> Unit,
    onChangeMobility: (String) -> Unit
) {
    var selectedThreat by remember { mutableStateOf("Terremoto") }
    var selectedRouteId by remember { mutableIntStateOf(0) }

    // Simulación del Modelo Bayesiano (Distribución Beta por Clúster)
    // alpha = Confirmaciones ciudadanas | beta = Negaciones
    var alphaVotes by remember { mutableDoubleStateOf(7.0) } // 7 personas confirmaron
    var betaVotes by remember { mutableDoubleStateOf(1.0) }  // 1 persona negó

    // Cálculo formal de la media de la distribución Beta: E[p] = alpha / (alpha + beta)
    val betaConfidence = remember(alphaVotes, betaVotes) {
        alphaVotes / (alphaVotes + betaVotes)
    }
    val isIncidentVerified = betaConfidence >= 0.75 // Umbral de bloqueo >= 75%

    val mobilityModes = listOf("Vehículo", "A pie", "Bicicleta", "Movilidad reducida")

    val threats = listOf(
        EmergencyThreat("Terremoto", "🌎", "Historial sísmico: caída de postes y muros"),
        EmergencyThreat("Incendio", "🔥", "Historial: avance de llamas y humo en calzada"),
        EmergencyThreat("Inundación", "🌊", "Historial: anegamiento en pasos bajo nivel"),
        EmergencyThreat("Tsunami", "🌊", "SENAPRED: evacuación sobre cota 30"),
        EmergencyThreat("Derrumbe", "🪨", "SERNAGEOMIN: inestabilidad de laderas")
    )

    // Generación dinámica del grafo vial según Movilidad + Tipo de Amenaza + Validación Beta
    val generatedRoutes = remember(mobilityMode, selectedThreat, isIncidentVerified) {
        when (mobilityMode) {
            "Vehículo" -> {
                if (isIncidentVerified) {
                    listOf(
                        EvaluatedRoute(
                            id = 0,
                            title = "Ruta vehicular recomendada (Desvío)",
                            destination = "Zona Segura: Autopista Central Norte",
                            distance = "2,6 km",
                            estimatedTime = "6 min (Auto)",
                            riskLevel = "Bajo",
                            riskColor = SafeGreen,
                            riskBg = SafeGreenLight,
                            routeLineColor = EvacuBlue, // Línea Azul
                            criteriaReason = "Bloqueo en Av. Viel VERIFICADO por consenso ciudadano ($alphaVotes reportes). OSRM desvía por Av. Matta y Autopista.",
                            isBlocked = false,
                            coordinates = listOf(
                                GeoPoint(-33.4672, -70.6576), // UBO Rondizzoni
                                GeoPoint(-33.4660, -70.6550), // Av. Viel
                                GeoPoint(-33.4610, -70.6540), // Av. Matta
                                GeoPoint(-33.4580, -70.6580), // Enlace Autopista
                                GeoPoint(-33.4550, -70.6620)  // Zona Segura vehicular
                            )
                        ),
                        EvaluatedRoute(
                            id = 1,
                            title = "Ruta alternativa por calzada sur",
                            destination = "Zona Segura: Caletera Sur",
                            distance = "3,2 km",
                            estimatedTime = "9 min (Auto)",
                            riskLevel = "Muy bajo",
                            riskColor = SafeGreen,
                            riskBg = SafeGreenLight,
                            routeLineColor = SafeGreen, // Línea Verde
                            criteriaReason = "Vía vehicular secundaria con capacidad de flujo ante evacuación masiva.",
                            isBlocked = false,
                            coordinates = listOf(
                                GeoPoint(-33.4672, -70.6576),
                                GeoPoint(-33.4700, -70.6580),
                                GeoPoint(-33.4730, -70.6610),
                                GeoPoint(-33.4750, -70.6650)
                            )
                        )
                    )
                } else {
                    listOf(
                        EvaluatedRoute(
                            id = 0,
                            title = "Ruta directa por Av. Viel",
                            destination = "Zona Segura: Parque O'Higgins",
                            distance = "1,8 km",
                            estimatedTime = "4 min (Auto)",
                            riskLevel = "Medio",
                            riskColor = WarningAmber,
                            riskBg = WarningAmberLight,
                            routeLineColor = EvacuBlue,
                            criteriaReason = "Alerta pendiente de verificación. Vía transitable con precaución.",
                            isBlocked = false,
                            coordinates = listOf(
                                GeoPoint(-33.4672, -70.6576),
                                GeoPoint(-33.4650, -70.6560),
                                GeoPoint(-33.4630, -70.6580)
                            )
                        )
                    )
                }
            }
            "Bicicleta" -> listOf(
                EvaluatedRoute(
                    id = 0,
                    title = "Ruta ciclovía protegida",
                    destination = "Zona Segura: Parque O'Higgins",
                    distance = "1,6 km",
                    estimatedTime = "7 min (Bici)",
                    riskLevel = "Bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = EvacuBlue,
                    criteriaReason = "Grafo prioriza ejes con ciclovías segregadas y ancho libre de escombros.",
                    isBlocked = false,
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4670, -70.6590),
                        GeoPoint(-33.4655, -70.6595),
                        GeoPoint(-33.4640, -70.6610)
                    )
                )
            )
            "Movilidad reducida" -> listOf(
                EvaluatedRoute(
                    id = 0,
                    title = "Ruta 100% Accesible y Nivelada",
                    destination = "Explanada Parque O'Higgins",
                    distance = "1,5 km",
                    estimatedTime = "16 min (Silla de ruedas/Asistida)",
                    riskLevel = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = SafeGreen,
                    criteriaReason = "Descarta pasarelas elevadas, escaleras y veredas angostas. Pendiente máxima 4%.",
                    isBlocked = false,
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4675, -70.6585),
                        GeoPoint(-33.4660, -70.6600),
                        GeoPoint(-33.4645, -70.6615)
                    )
                )
            )
            else -> listOf( // "A pie" (Peatonal)
                EvaluatedRoute(
                    id = 0,
                    title = "Ruta peatonal recomendada",
                    destination = "Zona Segura: Parque O'Higgins",
                    distance = "1,4 km",
                    estimatedTime = "12 min (A pie)",
                    riskLevel = "Bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = EvacuBlue,
                    criteriaReason = "Vía peatonal directa evitando calzadas vehiculares congestionadas.",
                    isBlocked = false,
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4670, -70.6590),
                        GeoPoint(-33.4655, -70.6592),
                        GeoPoint(-33.4638, -70.6610)
                    )
                ),
                EvaluatedRoute(
                    id = 1,
                    title = "Ruta peatonal alternativa",
                    destination = "Plaza Manuel Rodríguez",
                    distance = "1,7 km",
                    estimatedTime = "15 min (A pie)",
                    riskLevel = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = SafeGreen,
                    criteriaReason = "Circunvalación amplia con sombra y veredas anchas.",
                    isBlocked = false,
                    coordinates = listOf(
                        GeoPoint(-33.4672, -70.6576),
                        GeoPoint(-33.4685, -70.6575),
                        GeoPoint(-33.4690, -70.6610),
                        GeoPoint(-33.4660, -70.6630)
                    )
                )
            )
        }
    }

    val activeRoute = generatedRoutes.getOrElse(selectedRouteId) { generatedRoutes.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(AppBackground)
    ) {
        // 1. SELECTOR DE MODO DE TRANSPORTE
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
                // Indicador de modo activo
                Surface(
                    color = EvacuBlueLight,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Modo: $mobilityMode",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = EvacuBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Selector dinámico de movilidad
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
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
                            onChangeMobility(mode)
                            selectedRouteId = 0
                        },
                        leadingIcon = {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
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

        // 2. MAPA CON POLYLINE EN TIEMPO REAL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                RouteMapOSM(route = activeRoute)
            }
        }

        // 3. PANEL DE VALIDACIÓN BAYESIANA (DISTRIBUCIÓN BETA)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isIncidentVerified) DangerRedLight else WarningAmberLight
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
                            imageVector = if (isIncidentVerified) Icons.Default.Warning else Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = if (isIncidentVerified) DangerRed else WarningAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isIncidentVerified) "Bloqueo vial VERIFICADO" else "Reporte en validación",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncidentVerified) DangerRed else WarningAmber
                        )
                    }

                    // Porcentaje de confianza Beta: alpha / (alpha + beta)
                    Surface(
                        color = if (isIncidentVerified) DangerRed else WarningAmber,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Confianza Beta: ${(betaConfidence * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Consenso comunitario: ${alphaVotes.toInt()} confirmaciones (α) vs ${betaVotes.toInt()} descartes (β). Umbral de desvío: 75%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )

                // Botones interactivos para probar el impacto de los reportes en vivo
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "¿Estás en el lugar?", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { alphaVotes += 1.0 },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "Confirmar bloqueo", tint = SafeGreen)
                    }
                    IconButton(
                        onClick = { betaVotes += 1.0 },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ThumbDown, contentDescription = "Vía libre", tint = DangerRed)
                    }
                }
            }
        }

        // 4. LISTA DE TARJETAS DE RUTA ADAPTADAS AL MODO Y RIESGO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            generatedRoutes.forEachIndexed { index, route ->
                SelectableRouteCard(
                    route = route,
                    isSelected = selectedRouteId == index,
                    onSelect = { selectedRouteId = index },
                    onStartNavigation = onSelectRoute
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RouteMapOSM(route: EvaluatedRoute) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val (mapView, polyline, startMarker, endMarker) = remember {
        Configuration.getInstance().userAgentValue = "EvacuApp-UBO-StudentProject/1.0 (${context.packageName})"
        val map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(16.0)
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
        line.outlinePaint.color = route.routeLineColor.toArgb()

        if (route.coordinates.isNotEmpty()) {
            start.position = route.coordinates.first()
            end.position = route.coordinates.last()
            end.title = route.destination

            val centerLat = (route.coordinates.first().latitude + route.coordinates.last().latitude) / 2
            val centerLng = (route.coordinates.first().longitude + route.coordinates.last().longitude) / 2
            map.controller.animateTo(GeoPoint(centerLat, centerLng), 15.8, 400L)
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
    route: EvaluatedRoute,
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

            if (isSelected) {
                Button(
                    onClick = onStartNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (route.id == 0) EvacuBlue else SafeGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "INICIAR EVACUACIÓN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}