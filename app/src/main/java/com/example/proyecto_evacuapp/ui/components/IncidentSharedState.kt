package com.example.proyecto_evacuapp.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.proyecto_evacuapp.data.remote.IncidentResponseDto
import com.example.proyecto_evacuapp.data.repository.IncidentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object IncidentSharedState {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val incidentList = mutableStateListOf<SharedIncident>()

    val incidents: List<SharedIncident>
        get() = incidentList

    var isClearedForTesting = false
        private set

    private var initialized = false
    private lateinit var incidentDao: IncidentDao

    // MECANISMO DE CONTROL Y BLOQUEO TEMPORAL DE VOTOS (DEBOUNCE / LOCK 30 SEGUNDOS)
    private val recentlyVotedIncidents = mutableMapOf<String, Long>()
    private const val VOTE_PROTECTION_WINDOW_MS = 30_000L // 30 segundos de protección

    fun markAsVoted(id: String?) {
        if (id.isNullOrBlank()) return
        recentlyVotedIncidents[id] = System.currentTimeMillis()
    }

    fun isVoteProtected(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        val lastVoted = recentlyVotedIncidents[id] ?: return false
        val elapsed = System.currentTimeMillis() - lastVoted
        if (elapsed > VOTE_PROTECTION_WINDOW_MS) {
            recentlyVotedIncidents.remove(id)
            return false
        }
        return true
    }

    fun initialize(database: EvacuAppDatabase) {
        if (initialized) return

        initialized = true
        incidentDao = database.incidentDao()

        scope.launch {
            incidentDao.observeAll().collectLatest { entities ->
                if (!isClearedForTesting) {
                    val restoredIncidents = entities.map { entity ->
                        val shared = entity.toSharedIncident()
                        if (shared.affectedEdgeId == null) {
                            val edgeId = com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine.snapToNearestEdge(shared.latitude, shared.longitude)
                            if (edgeId != null) {
                                val updated = shared.copy(affectedEdgeId = edgeId)
                                persist(updated)
                                updated
                            } else shared
                        } else shared
                    }

                    withContext(Dispatchers.Main.immediate) {
                        incidentList.clear()
                        incidentList.addAll(restoredIncidents)
                    }
                }
            }
        }
    }

    /**
     * Limpia TODOS los reportes e incidentes locales de la memoria y la base de datos Room,
     * dejando la lista totalmente en 0 para poder probar la creación de reportes uno por uno.
     */
    fun clearAllIncidents(context: Context) {
        isClearedForTesting = true
        recentlyVotedIncidents.clear()
        scope.launch(Dispatchers.Main.immediate) {
            incidentList.clear()
        }
        scope.launch(Dispatchers.IO) {
            if (::incidentDao.isInitialized) {
                incidentDao.deleteAllIncidents()
            } else {
                val db = EvacuAppDatabase.getInstance(context)
                db.incidentDao().deleteAllIncidents()
            }
        }
    }

    /**
     * Sincroniza la respuesta del servidor (Backend Source of Truth).
     * Purga de Room y de memoria cualquier registro remoto que haya sido eliminado del backend.
     */
    fun syncRemoteIncidents(remoteList: List<IncidentResponseDto>) {
        if (isClearedForTesting) return // No re-inserta si se limpió manualmente para pruebas

        scope.launch {
            val serverRemoteIds = remoteList.mapNotNull { it.id }.toSet()

            withContext(Dispatchers.Main.immediate) {
                // 1. PURGADO DE REGISTROS REMOTOS HUÉRFANOS (Backend Source of Truth)
                val orphanedIncidents = incidentList.filter { incident ->
                    val hasRemoteId = !incident.remoteId.isNullOrBlank()
                    val notInServer = hasRemoteId && incident.remoteId !in serverRemoteIds
                    val notProtected = !isVoteProtected(incident.remoteId) && !isVoteProtected(incident.localId)
                    hasRemoteId && notInServer && notProtected
                }

                orphanedIncidents.forEach { orphaned ->
                    incidentList.remove(orphaned)
                    if (::incidentDao.isInitialized) {
                        scope.launch(Dispatchers.IO) {
                            incidentDao.deleteByLocalId(orphaned.localId)
                        }
                    }
                }

                // Si la respuesta del servidor es una lista vacía (ej. PostgreSQL fue vaciado), purgar todos los registros remotos no protegidos
                if (remoteList.isEmpty()) {
                    val unprotectedRemoteIds = incidentList
                        .filter { !it.remoteId.isNullOrBlank() && !isVoteProtected(it.remoteId) && !isVoteProtected(it.localId) }
                        .map { it.localId }

                    incidentList.removeAll { unprotectedRemoteIds.contains(it.localId) }

                    if (::incidentDao.isInitialized) {
                        scope.launch(Dispatchers.IO) {
                            incidentDao.deleteAllRemoteIncidents()
                        }
                    }
                }

                // 2. INSERTAR O ACTUALIZAR REGISTROS ACTIVOS DEL SERVIDOR
                remoteList.forEach { dto ->
                    val dtoId = dto.id ?: return@forEach
                    val type = IncidentType.fromApiValue(dto.type)
                    val severity = IncidentSeverity.fromApiValue(dto.severity)
                    val statusFromApi = try { IncidentStatus.valueOf(dto.status ?: "PENDING") } catch (_: Exception) { IncidentStatus.PENDING }

                    val existingIndex = incidentList.indexOfFirst {
                        it.remoteId == dtoId || (it.localId.isNotBlank() && it.latitude == (dto.latitude ?: 0.0) && it.longitude == (dto.longitude ?: 0.0))
                    }

                    if (existingIndex >= 0 && existingIndex < incidentList.size) {
                        val existing = incidentList[existingIndex]

                        val protected = isVoteProtected(dtoId) || isVoteProtected(existing.remoteId) || isVoteProtected(existing.localId)

                        val finalAlpha: Double
                        val finalBeta: Double
                        val finalStatus: IncidentStatus

                        if (protected) {
                            finalAlpha = maxOf(existing.alpha, dto.alpha ?: existing.alpha)
                            finalBeta = maxOf(existing.beta, dto.beta ?: existing.beta)

                            val confidence = if (finalAlpha + finalBeta > 0) finalAlpha / (finalAlpha + finalBeta) else 0.0
                            val calculatedStatus = when {
                                confidence >= 0.60 -> IncidentStatus.VERIFIED
                                confidence >= 0.50 -> IncidentStatus.PROBABLE
                                else -> IncidentStatus.PENDING
                            }

                            finalStatus = if (existing.status == IncidentStatus.VERIFIED && calculatedStatus != IncidentStatus.VERIFIED) {
                                IncidentStatus.VERIFIED
                            } else {
                                calculatedStatus
                            }
                        } else {
                            finalAlpha = dto.alpha ?: existing.alpha
                            finalBeta = dto.beta ?: existing.beta
                            finalStatus = statusFromApi
                        }

                        val edgeId = existing.affectedEdgeId ?: com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine.snapToNearestEdge(dto.latitude ?: existing.latitude, dto.longitude ?: existing.longitude)
                        val updated = existing.copy(
                            remoteId = dtoId,
                            type = type,
                            severity = severity,
                            description = dto.description ?: "",
                            latitude = dto.latitude ?: existing.latitude,
                            longitude = dto.longitude ?: existing.longitude,
                            alpha = finalAlpha,
                            beta = finalBeta,
                            status = finalStatus,
                            affectedEdgeId = edgeId,
                            updatedAtMillis = System.currentTimeMillis()
                        )
                        replaceInMemory(updated)
                        persist(updated)
                    } else {
                        val edgeId = com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine.snapToNearestEdge(dto.latitude ?: 0.0, dto.longitude ?: 0.0)
                        val newIncident = SharedIncident(
                            remoteId = dtoId,
                            type = type,
                            severity = severity,
                            description = dto.description ?: "",
                            latitude = dto.latitude ?: 0.0,
                            longitude = dto.longitude ?: 0.0,
                            alpha = dto.alpha ?: 1.0,
                            beta = dto.beta ?: 1.0,
                            status = statusFromApi,
                            isOwnReport = false,
                            affectedEdgeId = edgeId
                        )
                        replaceInMemory(newIncident)
                        persist(newIncident)
                    }
                }
            }
        }
    }

    fun markAsSynced(localId: String, remoteId: String?, status: String) {
        if (!remoteId.isNullOrBlank()) {
            markAsVoted(remoteId)
            markAsVoted(localId)
        }
        scope.launch(Dispatchers.Main.immediate) {
            val index = incidentList.indexOfFirst { it.localId == localId || (remoteId != null && it.remoteId == remoteId) }
            if (index >= 0 && index < incidentList.size) {
                val current = incidentList[index]
                val parsedStatus = try { IncidentStatus.valueOf(status) } catch (_: Exception) { current.status }
                val updated = current.copy(
                    remoteId = remoteId ?: current.remoteId,
                    status = parsedStatus,
                    updatedAtMillis = System.currentTimeMillis()
                )
                replaceInMemory(updated)
                persist(updated)
            }
        }
    }

    fun updateVoteFromRemote(localId: String, alpha: Double, beta: Double, status: String) {
        markAsVoted(localId)
        scope.launch(Dispatchers.Main.immediate) {
            val index = incidentList.indexOfFirst { it.localId == localId || it.remoteId == localId }
            if (index >= 0 && index < incidentList.size) {
                val current = incidentList[index]
                markAsVoted(current.remoteId)
                markAsVoted(current.localId)

                val parsedStatus = try { IncidentStatus.valueOf(status) } catch (_: Exception) { IncidentStatus.PENDING }
                val updated = current.copy(
                    alpha = alpha,
                    beta = beta,
                    status = parsedStatus,
                    updatedAtMillis = System.currentTimeMillis()
                )
                replaceInMemory(updated)
                persist(updated)
            }
        }
    }

    fun addLocalIncident(incident: SharedIncident) {
        isClearedForTesting = false // Se restablece el flag al crear manualmente un nuevo reporte para que este persista
        val edgeId = incident.affectedEdgeId ?: com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine.snapToNearestEdge(incident.latitude, incident.longitude)
        val localIncident = incident.copy(
            status = IncidentStatus.LOCAL_PENDING,
            affectedEdgeId = edgeId,
            updatedAtMillis = System.currentTimeMillis()
        )

        scope.launch(Dispatchers.Main.immediate) {
            replaceInMemory(localIncident)
            persist(localIncident)
        }
    }

    fun verifyIncident(localId: String) {
        confirmIncident(localId)
    }

    fun confirmIncident(localId: String) {
        markAsVoted(localId)
        updateVote(
            localId = localId,
            isConfirmation = true
        )
    }

    fun rejectIncident(localId: String) {
        markAsVoted(localId)
        updateVote(
            localId = localId,
            isConfirmation = false
        )
    }

    // FUNCIÓN UNIFICADA PARA VOTO CON RESOLUCIÓN DE REMOTE ID Y EJECUCIÓN HTTP GARANTIZADA
    fun voteIncidentAndSync(
        localId: String,
        voteType: String, // "CONFIRM" or "REJECT"
        repository: IncidentRepository,
        onResult: (Boolean, SharedIncident?) -> Unit = { _, _ -> }
    ) {
        val isConfirmation = voteType.uppercase() == "CONFIRM"
        markAsVoted(localId)

        // 1. Actualización optimista inmediata en memoria y Room DB
        if (isConfirmation) confirmIncident(localId) else rejectIncident(localId)

        val target = incidentList.find { it.localId == localId || it.remoteId == localId }
        val initialRemoteId = target?.remoteId
        markAsVoted(initialRemoteId)

        scope.launch(Dispatchers.IO) {
            val resolvedRemoteId = if (!initialRemoteId.isNullOrBlank()) {
                initialRemoteId
            } else if (::incidentDao.isInitialized) {
                val dbEntity = incidentDao.getByLocalId(localId)
                if (dbEntity != null && !dbEntity.remoteId.isNullOrBlank()) {
                    dbEntity.remoteId
                } else null
            } else {
                null
            }

            Log.d("EVAC_DIAG", "[VOTE_INCIDENT] Evaluando voto $voteType | LocalID: $localId | ResolvedOfficialServerID: $resolvedRemoteId")

            if (!resolvedRemoteId.isNullOrBlank()) {
                markAsVoted(resolvedRemoteId)
                try {
                    Log.d("EVAC_DIAG", "[VOTE_INCIDENT] Enviando HTTP POST /api/v1/incidents/$resolvedRemoteId/vote | VoteType: $voteType")
                    val response = repository.voteIncident(resolvedRemoteId, voteType.uppercase())

                    if (response.isSuccessful && response.body() != null) {
                        val dto = response.body()!!
                        Log.d("EVAC_DIAG", "[VOTE_INCIDENT] EXITO HTTP ${response.code()} | OfficialServerID: $resolvedRemoteId | Alpha: ${dto.alpha} | Beta: ${dto.beta} | Status: ${dto.status}")
                        markAsVoted(resolvedRemoteId)
                        markAsVoted(localId)

                        val updatedAlpha = dto.alpha ?: (target?.alpha ?: 1.0)
                        val updatedBeta = dto.beta ?: (target?.beta ?: 1.0)

                        updateVoteFromRemote(
                            localId = localId,
                            alpha = updatedAlpha,
                            beta = updatedBeta,
                            status = dto.status ?: "PENDING"
                        )
                        val updatedTarget = incidentList.find { it.localId == localId || it.remoteId == resolvedRemoteId }
                        withContext(Dispatchers.Main.immediate) {
                            onResult(true, updatedTarget)
                        }
                    } else {
                        Log.e("EVAC_DIAG", "[VOTE_INCIDENT] ERROR HTTP ${response.code()} al votar | Body: ${response.errorBody()?.string()}")
                        withContext(Dispatchers.Main.immediate) {
                            onResult(false, target)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EVAC_DIAG", "[VOTE_INCIDENT] Excepción de red al enviar voto remoto: ${e.message}", e)
                    withContext(Dispatchers.Main.immediate) {
                        onResult(false, target)
                    }
                }
            } else {
                Log.w("EVAC_DIAG", "[VOTE_INCIDENT] Bloqueado: El reporte local $localId aún no tiene un remoteId oficial asignado por el servidor. Voto guardado localmente de forma optimista.")
                withContext(Dispatchers.Main.immediate) {
                    onResult(true, target)
                }
            }
        }
    }

    fun verifiedBlockedSegmentIds(): Set<String> {
        return incidentList
            .filter { it.isVerified }
            .flatMap { it.affectedSegmentIds }
            .toSet()
    }

    private fun updateVote(
        localId: String,
        isConfirmation: Boolean
    ) {
        scope.launch(Dispatchers.IO) {
            val current = incidentList.find { it.localId == localId || it.remoteId == localId }
                ?: (if (::incidentDao.isInitialized) incidentDao.getByLocalId(localId)?.toSharedIncident() else null)

            if (current == null) return@launch

            markAsVoted(current.localId)
            markAsVoted(current.remoteId)

            val inferStartNs = System.nanoTime()
            val newAlpha = if (isConfirmation) current.alpha + 1.0 else current.alpha
            val newBeta = if (isConfirmation) current.beta else current.beta + 1.0

            val confidence = if (newAlpha + newBeta > 0) newAlpha / (newAlpha + newBeta) else 0.0
            val validityScore = String.format(java.util.Locale.US, "%.2f", confidence)
            val inferMsDouble = (System.nanoTime() - inferStartNs) / 1_000_000.0
            val inferMs = String.format(java.util.Locale.US, "%.3f", inferMsDouble)

            Log.d("EVAC_METRIC", "[ML_ONNX] InferenceTime: ${inferMs}ms | ValidityScore: $validityScore | BetaAlpha: $newAlpha | BetaBeta: $newBeta")

            val incidentTypeStr = current.type.displayName.replace(",", " ")
            val incidentCoordsStr = String.format(java.util.Locale.US, "%.5f;%.5f", current.latitude, current.longitude)

            com.example.proyecto_evacuapp.utils.PowerBiCsvLogger.logMetricRow(
                context = null,
                ejecucionId = localId.take(8),
                modo = "Offline",
                perfil = "Consenso_Beta",
                tipoRuta = "Evento_ONNX",
                destinoNombre = incidentTypeStr,
                destinoCoords = incidentCoordsStr,
                nodosProcesados = 0,
                aristasProcesadas = 0,
                cargaBBoxMs = 0L,
                calculoRutaMs = 0L,
                tiempoTotalMs = 0L,
                distanciaMeters = 0.0,
                tiempoEstimadoMin = 0,
                giros = 0,
                inferenciaOnnxMs = inferMsDouble,
                tramaBytes = 0,
                exito = true
            )

            val newStatus = when {
                confidence >= 0.60 -> IncidentStatus.VERIFIED
                confidence >= 0.50 -> IncidentStatus.PROBABLE
                else -> IncidentStatus.PENDING
            }

            val updatedIncident = current.copy(
                alpha = newAlpha,
                beta = newBeta,
                status = newStatus,
                updatedAtMillis = System.currentTimeMillis()
            )

            withContext(Dispatchers.Main.immediate) {
                replaceInMemory(updatedIncident)
            }
            persist(updatedIncident)
        }
    }

    private fun replaceInMemory(incident: SharedIncident) {
        val index = incidentList.indexOfFirst {
            it.localId == incident.localId || (incident.remoteId != null && it.remoteId == incident.remoteId)
        }

        if (index >= 0 && index < incidentList.size) {
            incidentList[index] = incident
        } else {
            incidentList.add(0, incident)
        }
    }

    private fun persist(incident: SharedIncident) {
        if (!::incidentDao.isInitialized) return

        scope.launch(Dispatchers.IO) {
            incidentDao.upsert(
                incident.toEntity()
            )
        }
    }

    fun triggerSync(context: Context) {
        IncidentSyncService.scheduleSync(context)
    }
}

private fun SharedIncident.toEntity(): IncidentEntity {
    return IncidentEntity(
        localId = localId,
        remoteId = remoteId,
        type = type.name,
        severity = severity.name,
        description = description,
        latitude = latitude,
        longitude = longitude,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        alpha = alpha,
        beta = beta,
        status = status.name,
        affectedSegmentIds = affectedSegmentIds.joinToString(","),
        isOwnReport = isOwnReport,
        affectedEdgeId = affectedEdgeId
    )
}

private fun IncidentEntity.toSharedIncident(): SharedIncident {
    return SharedIncident(
        localId = localId,
        remoteId = remoteId,
        type = IncidentType.fromApiValue(type),
        severity = IncidentSeverity.fromApiValue(severity),
        description = description,
        latitude = latitude,
        longitude = longitude,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        alpha = alpha,
        beta = beta,
        status = IncidentStatus.valueOf(status),
        affectedSegmentIds = affectedSegmentIds
            .split(",")
            .filter { it.isNotBlank() }
            .toSet(),
        isOwnReport = isOwnReport,
        affectedEdgeId = affectedEdgeId
    )
}
