package com.example.proyecto_evacuapp.domain.engine

import android.util.Log
import com.example.proyecto_evacuapp.ui.components.IncidentEntity
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.IncidentType
import com.example.proyecto_evacuapp.ui.components.RouteCoordinate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "RoadNetworkRepository"
private const val MAX_MATCH_DISTANCE_METERS = 60.0

private val RISK_INCIDENT_TYPES = setOf(
    IncidentType.BLOQUEO_VIAL,
    IncidentType.INCENDIO,
    IncidentType.INUNDACION,
    IncidentType.DERRUMBE,
    IncidentType.ACCIDENTE
)

private fun parseIncidentStatus(value: String): IncidentStatus? =
    IncidentStatus.entries.find { it.name.equals(value, ignoreCase = true) }

private fun parseAffectedSegmentIds(raw: String): Set<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

/**
 * Repositorio optimizado en memoria: construye y carga la red vial dinámica
 * al vuelo para cualquier par de coordenadas (origen y destino), evitando
 * cualquier conflicto de base de datos o IDs duplicados.
 */
class RoadNetworkRepository {
    private val graph = RoadGraph()
    private val mutex = Mutex()

    /**
     * Genera y carga la red vial dinámica en memoria para la ruta actual.
     */
    suspend fun ensureLoadedForRoute(origin: RouteCoordinate, destination: RouteCoordinate) {
        mutex.withLock {
            // 1. Generamos la malla dinámica directamente en memoria con el seed
            val seed = PilotRoadNetworkSeed.buildDynamic(origin, destination)

            // 2. Convertimos el seed a los modelos del grafo en memoria
            val nodes = seed.nodes.map { GraphNode(it.id, RouteCoordinate(it.latitude, it.longitude)) }
            val edges = seed.edges.map {
                GraphEdge(
                    id = it.id,
                    fromId = it.fromNodeId,
                    toId = it.toNodeId,
                    distanceMeters = it.distanceMeters,
                    riskWeight = it.riskWeight,
                    accessibilityPenalty = it.accessibilityPenalty,
                    isBlocked = it.isBlocked,
                    bidirectional = it.isBidirectional,
                    blockingIncidentLocalId = it.blockingIncidentLocalId
                )
            }

            // 3. Cargamos el grafo limpiamente (reemplaza cualquier estado anterior)
            graph.load(nodes, edges)
            Log.d(TAG, "Red vial dinámica cargada en memoria: ${nodes.size} nodos, ${edges.size} aristas.")
        }
    }

    suspend fun ensureLoaded() {
        // Al ser completamente dinámico por ruta, no requiere precarga estática.
    }

    fun graphSnapshot(): RoadGraph = graph

    /**
     * Aplica el efecto de los incidentes sobre las aristas en memoria segun su fiabilidad y severidad.
     */
    suspend fun applyIncidents(incidents: List<IncidentEntity>): Boolean {
        var anyChanged = false

        mutex.withLock {
            val activeIncidentIds = HashSet<String>()

            for (incident in incidents) {
                val status = parseIncidentStatus(incident.status)
                val type = IncidentType.fromApiValue(incident.type)
                val severity = IncidentSeverity.fromApiValue(incident.severity)

                val isVerified = status == IncidentStatus.VERIFIED
                val isCriticalOrHigh = severity == IncidentSeverity.CRITICA || severity == IncidentSeverity.ALTA
                val isBlockingType = type in RISK_INCIDENT_TYPES || type == IncidentType.RUTA_INACCESIBLE

                if (isBlockingType && (isVerified || isCriticalOrHigh)) {
                    // 1. BLOQUEO COMPLETO (Costo Infinito C(e) = infinity)
                    activeIncidentIds += incident.localId
                    val targetEdges = resolveAffectedEdges(incident)
                    if (targetEdges.isEmpty()) continue

                    for (edge in targetEdges) {
                        val changed = if (type == IncidentType.RUTA_INACCESIBLE) {
                            graph.applyEdgeAccessibility(edge.id, penalty = 1.0, incidentLocalId = incident.localId)
                        } else {
                            graph.applyEdgeRisk(
                                edge.id,
                                riskWeight = 1.0,
                                blocked = true,
                                incidentLocalId = incident.localId
                            )
                        }
                        if (changed) {
                            anyChanged = true
                            Log.d(TAG, "Arista ${edge.id} BLOQUEADA por incidente VERIFIED/ALTA/CRITICA (${incident.localId})")
                        }
                    }
                } else if (isBlockingType && (status == IncidentStatus.PROBABLE || status == IncidentStatus.PENDING || status == IncidentStatus.LOCAL_PENDING)) {
                    // 2. INCIDENTE EN REVISIÓN / MENOR SEVERIDAD: Incrementa el costo de riesgo R(e)
                    activeIncidentIds += incident.localId
                    val targetEdges = resolveAffectedEdges(incident)
                    if (targetEdges.isEmpty()) continue

                    for (edge in targetEdges) {
                        val changed = graph.applyEdgeRisk(
                            edge.id,
                            riskWeight = 0.8,
                            blocked = false, // Sin bloqueo absoluto, pero alto riesgo R(e)
                            incidentLocalId = incident.localId
                        )
                        if (changed) {
                            anyChanged = true
                            Log.d(TAG, "Arista ${edge.id} asignada riesgo R(e)=0.8 por reporte en revisión (${incident.localId})")
                        }
                    }
                }
            }

            // Liberar aristas cuyo incidente ya no aplica
            val blockedByStaleIncident = graph.allEdges()
                .mapNotNull { it.blockingIncidentLocalId }
                .toSet()
                .filter { it !in activeIncidentIds }

            for (staleIncidentId in blockedByStaleIncident) {
                val relatedEdges = graph.findEdgesByIncidentId(staleIncidentId)
                for (edge in relatedEdges) {
                    val changed = graph.clearEdgeRisk(edge.id)
                    if (changed) {
                        anyChanged = true
                        Log.d(TAG, "Arista ${edge.id} liberada: incidente $staleIncidentId desactivado")
                    }
                }
            }
        }

        return anyChanged
    }

    private fun resolveAffectedEdges(incident: IncidentEntity): List<GraphEdge> {
        val explicitIds = parseAffectedSegmentIds(incident.affectedSegmentIds)
        if (explicitIds.isNotEmpty()) {
            return explicitIds.mapNotNull { graph.edgeById(it) }
        }
        val point = RouteCoordinate(incident.latitude, incident.longitude)
        return listOfNotNull(graph.nearestEdge(point, MAX_MATCH_DISTANCE_METERS))
    }
}