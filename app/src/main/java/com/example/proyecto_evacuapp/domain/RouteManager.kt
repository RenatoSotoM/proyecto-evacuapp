package com.example.proyecto_evacuapp.domain

import android.util.Log
import com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.LocalRouteResult
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import com.example.proyecto_evacuapp.ui.components.RouteMobilityProfile
import com.example.proyecto_evacuapp.ui.components.RouteVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "RouteManager"

/**
 * Gestor central del ruteo dinámico. Responsabilidades:
 *  1. Escuchar el DAO de incidentes y sincronizar sus cambios con el grafo vial
 *     (LocalRouteEngine.syncIncidents), que a su vez bloquea/penaliza aristas VERIFIED.
 *  2. Calcular, ante cada solicitud o recálculo, hasta 3 alternativas de ruta.
 *  3. Exponer el resultado como StateFlow para que la UI (panel inferior + mapa) reaccione.
 *
 * Requiere que LocalRouteEngine.initialize(context) ya se haya llamado (por ejemplo en el
 * LaunchedEffect(Unit) de MapScreen, antes de crear el RouteManager).
 */
class RouteManager(
    private val scope: CoroutineScope
) {
    private val _routeOptions = MutableStateFlow<List<LocalRouteResult>>(emptyList())
    val routeOptions: StateFlow<List<LocalRouteResult>> = _routeOptions.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    private var lastOrigin: RouteCoordinate? = null
    private var lastDestination: RouteCoordinate? = null
    private var lastProfile: RouteMobilityProfile = RouteMobilityProfile.WALKING

    private var incidentListenerJob: Job? = null

    /**
     * Comienza a escuchar `IncidentDao.observeAll()`. Cuando un reporte pasa a VERIFIED (o
     * deja de estarlo) y eso cambia el grafo, se recalculan automáticamente las rutas activas.
     */
    fun startListeningForIncidents(incidents: Flow<List<IncidentEntity>>) {
        incidentListenerJob?.cancel()
        incidentListenerJob = scope.launch {
            incidents.collect { list ->
                val changed = LocalRouteEngine.syncIncidents(list)
                if (changed) {
                    Log.d(TAG, "Grafo actualizado por incidentes, recalculando rutas activas")
                    recalculateActiveRoutesIfAny()
                }
            }
        }
    }

    fun stopListeningForIncidents() {
        incidentListenerJob?.cancel()
        incidentListenerJob = null
    }

    /** Calcula las alternativas de ruta y las guarda como "request activo" para recálculo automático. */
    fun calculateRoutes(
        origin: RouteCoordinate,
        destination: RouteCoordinate,
        profile: RouteMobilityProfile
    ) {
        lastOrigin = origin
        lastDestination = destination
        lastProfile = profile

        scope.launch {
            _isCalculating.value = true
            try {
                val results = LocalRouteEngine.calculateRouteAlternatives(origin, destination, profile)
                _routeOptions.value = results
                _selectedIndex.value = results.indexOfFirst { it.variant == RouteVariant.PRINCIPAL }
                    .coerceAtLeast(0)
                if (results.isEmpty()) {
                    Log.w(TAG, "No fue posible calcular ninguna ruta hacia el destino solicitado")
                }
            } finally {
                _isCalculating.value = false
            }
        }
    }

    fun selectRoute(route: LocalRouteResult) {
        val index = _routeOptions.value.indexOf(route)
        if (index >= 0) _selectedIndex.value = index
    }

    fun selectedRoute(): LocalRouteResult? = _routeOptions.value.getOrNull(_selectedIndex.value)

    fun clear() {
        lastOrigin = null
        lastDestination = null
        _routeOptions.value = emptyList()
        _selectedIndex.value = 0
        headingDeviationStartTime = null
    }

    private fun recalculateActiveRoutesIfAny() {
        val origin = lastOrigin ?: return
        val destination = lastDestination ?: return
        calculateRoutes(origin, destination, lastProfile)
    }

    private var headingDeviationStartTime: Long? = null

    /**
     * Detección de cambio de sentido y recálculo automático:
     * Evalúa si la diferencia angular entre el rumbo del usuario y la dirección de la ruta activa es > 90°
     * durante más de 3 segundos sostenidos (3000ms), disparando recálculo automático.
     */
    fun checkHeadingDeviationAndRecalculate(
        userPoint: RouteCoordinate,
        userBearing: Float?,
        activeRoutePoints: List<RouteCoordinate>,
        onTriggerRecalculate: () -> Unit
    ) {
        if (userBearing == null || userBearing < 0 || activeRoutePoints.size < 2) {
            headingDeviationStartTime = null
            return
        }

        val segmentAzimuth = findActiveRouteSegmentAzimuth(userPoint, activeRoutePoints) ?: run {
            headingDeviationStartTime = null
            return
        }

        val angularDiff = com.example.proyecto_evacuapp.domain.engine.angularDifferenceDegrees(userBearing.toDouble(), segmentAzimuth)

        if (angularDiff > 90.0) {
            val now = System.currentTimeMillis()
            val startTime = headingDeviationStartTime
            if (startTime == null) {
                headingDeviationStartTime = now
            } else if (now - startTime >= 3000L) { // 3 segundos sostenidos
                Log.w(TAG, "Desviación angular de rumbo > 90° durante 3s (${angularDiff.toInt()}° vs tramo ${segmentAzimuth.toInt()}°). Disparando recálculo...")
                headingDeviationStartTime = null
                onTriggerRecalculate()
            }
        } else {
            headingDeviationStartTime = null
        }
    }

    private fun findActiveRouteSegmentAzimuth(
        userPoint: RouteCoordinate,
        routePoints: List<RouteCoordinate>
    ): Double? {
        var closestDist = Double.MAX_VALUE
        var bestAzimuth: Double? = null

        for (i in 0 until routePoints.size - 1) {
            val p1 = routePoints[i]
            val p2 = routePoints[i + 1]
            val dist = com.example.proyecto_evacuapp.domain.engine.distancePointToSegmentMeters(userPoint, p1, p2)
            if (dist < closestDist) {
                closestDist = dist
                bestAzimuth = com.example.proyecto_evacuapp.domain.engine.calculateAzimuthDegrees(p1, p2)
            }
        }

        return if (closestDist <= 60.0) bestAzimuth else null
    }

    /**
     * Requerimiento 3: Gestión diferenciada de reportes en ruta.
     * Clasifica los incidentes cercanos en la ruta:
     * - Alertas informativas (Tráfico, Hoyo, Clima, Precaución): dispara aviso de voz/panel sin alterar la ruta.
     * - Bloqueos críticos: asignan C(e) = infinity en RoadGraph.kt y fuerzan recálculo automático.
     */
    fun checkNearbyInformativeIncidents(
        userPoint: RouteCoordinate,
        incidents: List<IncidentEntity>,
        warningRadiusMeters: Double = 100.0,
        onInformativeAlert: (IncidentEntity) -> Unit
    ) {
        val informativeTypes = setOf("TRAFICO", "TRÁFICO", "HOYO", "BACHE", "CLIMA", "PRECAUCION", "PRECAUCIÓN", "OTRO")

        val nearbyInformative = incidents.find { incident ->
            val isInformative = incident.type.uppercase() in informativeTypes ||
                    (incident.status != "VERIFIED" && incident.severity !in listOf("CRITICA", "ALTA", "CRITICAL", "HIGH"))

            if (isInformative) {
                val incPoint = RouteCoordinate(incident.latitude, incident.longitude)
                val dist = com.example.proyecto_evacuapp.domain.engine.haversineMeters(userPoint, incPoint)
                dist <= warningRadiusMeters
            } else {
                false
            }
        }

        nearbyInformative?.let { onInformativeAlert(it) }
    }
}