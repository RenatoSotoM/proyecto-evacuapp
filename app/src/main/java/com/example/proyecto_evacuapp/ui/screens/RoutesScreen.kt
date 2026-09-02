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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentType
import com.example.proyecto_evacuapp.ui.components.OsrmRoutingService
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
    onSelectRoute: () -> Unit,
    onChangeMobility: (String) -> Unit
) {
    var selectedRouteId by remember { mutableIntStateOf(0) }
    var selectedLayerName by remember { mutableStateOf("Estándar") }

    val mobilityModes = listOf(
        "Vehículo",
        "A pie",
        "Bicicleta",
        "Movilidad reducida"
    )

    /*
     * Estado compartido que viene desde ReportsScreen.
     *
     * IncidentSharedState usa mutableStateListOf, por lo que la pantalla
     * se recompone cuando un reporte pasa de PENDING a VERIFIED.
     */
    val sharedIncidents = IncidentSharedState.incidents

    val verifiedRoadIncidents = sharedIncidents.filter { incident ->
        incident.isVerified &&
                incident.type in setOf(
            IncidentType.BLOQUEO_VIAL,
            IncidentType.INUNDACION,
            IncidentType.DERRUMBE,
            IncidentType.ACCIDENTE,
            IncidentType.RUTA_INACCESIBLE
        )
    }

    val verifiedBlockedSegments = IncidentSharedState
        .verifiedBlockedSegmentIds()

    val hasVerifiedRoadBlock = verifiedBlockedSegments.isNotEmpty()

    /*
     * Origen y zonas seguras de demostración.
     *
     * Próxima mejora: reemplazar uboStart por ubicación GPS real y
     * cargar zonas seguras desde datos offline persistentes.
     */
    val uboStart = remember {
        GeoPoint(-33.4672, -70.6576)
    }

    val safeParqueOhiggins = remember {
        GeoPoint(-33.4638, -70.6610)
    }

    val safeExplanadaSur = remember {
        GeoPoint(-33.4730, -70.6580)
    }

    /*
     * Las rutas base contienen solo origen/destino y metadata.
     *
     * La geometría real será devuelta por OsrmRoutingService y nunca se
     * generan puntos manuales ni una diagonal origen -> destino.
     */
    val baseRoutes = remember(
        mobilityMode,
        verifiedBlockedSegments
    ) {
        when (mobilityMode) {
            "Vehículo" -> {
                if (hasVerifiedRoadBlock) {
                    listOf(
                        RealStreetRoute(
                            id = 0,
                            title = "Ruta vehicular recomendada (Desvío)",
                            destination = "Zona Segura: Explanada Norte",
                            distance = "Calculando...",
                            estimatedTime = "Calculando...",
                            riskLevel = "Bajo",
                            riskColor = SafeGreen,
                            riskBg = SafeGreenLight,
                            routeLineColor = EvacuBlue,
                            criteriaReason = "Bloqueo vial verificado por consenso comunitario. Se evita el tramo afectado y se calcula un desvío por calles habilitadas.",
                            startPoint = uboStart,
                            endPoint = safeParqueOhiggins
                        ),
                        RealStreetRoute(
                            id = 1,
                            title = "Ruta vehicular alternativa",
                            destination = "Zona Segura: Caletera Sur",
                            distance = "Calculando...",
                            estimatedTime = "Calculando...",
                            riskLevel = "Muy bajo",
                            riskColor = SafeGreen,
                            riskBg = SafeGreenLight,
                            routeLineColor = SafeGreen,
                            criteriaReason = "Alternativa vehicular para distribuir el flujo y evitar el sector reportado.",
                            startPoint = uboStart,
                            endPoint = safeExplanadaSur
                        )
                    )
                } else {
                    listOf(
                        RealStreetRoute(
                            id = 0,
                            title = "Ruta vehicular directa",
                            destination = "Zona Segura: Parque O'Higgins",
                            distance = "Calculando...",
                            estimatedTime = "Calculando...",
                            riskLevel = "Medio",
                            riskColor = WarningAmber,
                            riskBg = WarningAmberLight,
                            routeLineColor = EvacuBlue,
                            criteriaReason = "No hay bloqueos viales verificados por la comunidad para este trayecto.",
                            startPoint = uboStart,
                            endPoint = safeParqueOhiggins
                        )
                    )
                }
            }

            "Bicicleta" -> listOf(
                RealStreetRoute(
                    id = 0,
                    title = "Ruta ciclista recomendada",
                    destination = "Zona Segura: Parque O'Higgins",
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = EvacuBlue,
                    criteriaReason = "Perfil ciclista: el motor busca caminos permitidos para bicicleta y vías de menor exposición.",
                    startPoint = uboStart,
                    endPoint = safeParqueOhiggins
                )
            )

            "Movilidad reducida" -> listOf(
                RealStreetRoute(
                    id = 0,
                    title = "Ruta accesible recomendada",
                    destination = "Explanada Parque O'Higgins",
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = SafeGreen,
                    criteriaReason = "Perfil accesible: actualmente usa ruteo peatonal. La siguiente fase aplicará filtros de escaleras, pendiente y superficie desde datos locales.",
                    startPoint = uboStart,
                    endPoint = safeParqueOhiggins
                )
            )

            else -> listOf(
                RealStreetRoute(
                    id = 0,
                    title = "Ruta peatonal recomendada",
                    destination = "Zona Segura: Parque O'Higgins",
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = EvacuBlue,
                    criteriaReason = "Perfil peatonal: utiliza caminos permitidos para desplazamiento a pie hacia una zona segura.",
                    startPoint = uboStart,
                    endPoint = safeParqueOhiggins
                ),
                RealStreetRoute(
                    id = 1,
                    title = "Ruta peatonal alternativa",
                    destination = "Zona Segura: Plaza Rondizzoni",
                    distance = "Calculando...",
                    estimatedTime = "Calculando...",
                    riskLevel = "Muy bajo",
                    riskColor = SafeGreen,
                    riskBg = SafeGreenLight,
                    routeLineColor = SafeGreen,
                    criteriaReason = "Alternativa peatonal disponible si la ruta principal está congestionada o cambia la condición de seguridad.",
                    startPoint = uboStart,
                    endPoint = safeExplanadaSur
                )
            )
        }
    }

    var calculatedRoutes by remember(baseRoutes) {
        mutableStateOf(baseRoutes)
    }

    /*
     * Al cambiar entre perfiles o cambiar la cantidad de rutas,
     * evita índices fuera de rango.
     */
    LaunchedEffect(baseRoutes.size) {
        if (selectedRouteId >= baseRoutes.size) {
            selectedRouteId = 0
        }
    }

    /*
     * Consulta temporal online para obtener la geometría real sobre las
     * calles OSM. Cuando se integre BRouter/GraphHopper local, solo se
     * reemplaza OsrmRoutingService, sin modificar la UI.
     */
    LaunchedEffect(
        baseRoutes,
        mobilityMode,
        verifiedBlockedSegments
    ) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 6.dp
            )
        ) {
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
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        ),
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
                        label = {
                            Text(
                                text = mode,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EvacuBlue,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
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
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            } else if (activeRoute.polylinePoints.size < 2) {
                Surface(
                    color = WarningAmberLight.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Text(
                        text = activeRoute.routingError
                            ?: "No hay una geometría de ruta disponible.",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasVerifiedRoadBlock) {
                    DangerRedLight
                } else {
                    WarningAmberLight
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasVerifiedRoadBlock) {
                                Icons.Default.Warning
                            } else {
                                Icons.Default.AutoGraph
                            },
                            contentDescription = null,
                            tint = if (hasVerifiedRoadBlock) {
                                DangerRed
                            } else {
                                WarningAmber
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (hasVerifiedRoadBlock) {
                                "Incidente verificado: ruta recalculada"
                            } else {
                                "Sin bloqueos viales verificados"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasVerifiedRoadBlock) {
                                DangerRed
                            } else {
                                WarningAmber
                            }
                        )
                    }

                    Surface(
                        color = if (hasVerifiedRoadBlock) {
                            DangerRed
                        } else {
                            WarningAmber
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (hasVerifiedRoadBlock) {
                                "${verifiedRoadIncidents.size} bloqueo(s)"
                            } else {
                                "Sin desvíos"
                            },
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 3.dp
                            ),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (hasVerifiedRoadBlock) {
                        "El consenso comunitario verificó un incidente y el sistema selecciona rutas alternativas. " +
                                "Segmentos bloqueados: ${verifiedBlockedSegments.joinToString()}."
                    } else {
                        "Los reportes de la pestaña Reportes modificarán la ruta solo cuando alcancen " +
                                "una confianza Beta igual o superior a 75%."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 16.dp,
                    vertical = 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            calculatedRoutes.forEachIndexed { index, route ->
                SelectableRealRouteCard(
                    route = route,
                    isSelected = selectedRouteId == index,
                    onSelect = {
                        selectedRouteId = index
                    },
                    onStartNavigation = onSelectRoute
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RealStreetRouteMapOSM(
    route: RealStreetRoute,
    layerName: String
) {
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

        /*
         * Solo se dibuja si OSRM devolvió dos o más coordenadas reales.
         * No se construye nunca una diagonal artificial de origen a destino.
         */
        if (route.polylinePoints.size >= 2) {
            line.setPoints(route.polylinePoints)
        } else {
            line.setPoints(emptyList())
        }

        line.outlinePaint.color = route.routeLineColor.toArgb()

        val centerLatitude = (
                route.startPoint.latitude + route.endPoint.latitude
                ) / 2.0

        val centerLongitude = (
                route.startPoint.longitude + route.endPoint.longitude
                ) / 2.0

        map.controller.animateTo(
            GeoPoint(centerLatitude, centerLongitude),
            15.8,
            400L
        )

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

    AndroidView(
        factory = { mapView as MapView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SelectableRealRouteCard(
    route: RealStreetRoute,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onStartNavigation: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                EvacuBlueLight
            } else {
                SurfaceWhite
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                route.routeLineColor
            } else {
                Color(0xFFE2E8F0)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect()
            }
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

                Column(
                    modifier = Modifier.weight(1f)
                ) {
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
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 3.dp
                            ),
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
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        ),
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

            if (route.isLoading) {
                Text(
                    text = "Consultando geometría vial...",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            if (route.routingError != null) {
                Text(
                    text = "Ruta no disponible: ${route.routingError}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerRed
                )
            }

            if (
                isSelected &&
                !route.isLoading &&
                route.polylinePoints.size >= 2
            ) {
                Button(
                    onClick = onStartNavigation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (route.id == 0) {
                            EvacuBlue
                        } else {
                            SafeGreen
                        },
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "INICIAR EVACUACIÓN",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}