package com.example.proyecto_evacuapp.ui.components

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Query(
        """
        SELECT * FROM incidents
        ORDER BY updatedAtMillis DESC
        """
    )
    fun observeAll(): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(incident: IncidentEntity)

    @Query("UPDATE incidents SET remoteId = :serverId, status = :status WHERE localId = :localId")
    suspend fun updateRemoteId(localId: String, serverId: String, status: String)

    @Query(
        """
        DELETE FROM incidents
        WHERE localId = :localId
        """
    )
    suspend fun deleteByLocalId(localId: String)

    @Query("SELECT * FROM incidents WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): IncidentEntity?

    // --- PURGADO DE DATOS HUÉRFANOS PARA BACKEND SOURCE OF TRUTH ---

    /**
     * Elimina los registros sincronizados remotamente cuyo ID ya no existe en el servidor.
     */
    @Query("DELETE FROM incidents WHERE remoteId IS NOT NULL AND remoteId NOT IN (:activeRemoteIds)")
    suspend fun deleteOrphanedRemoteIncidents(activeRemoteIds: List<String>)

    /**
     * Elimina todos los registros de incidentes remotos cuando el servidor es vaciado/reseteado.
     */
    @Query("DELETE FROM incidents WHERE remoteId IS NOT NULL")
    suspend fun deleteAllRemoteIncidents()

    /**
     * Elimina TODOS los reportes registrados en la base de datos local.
     */
    @Query("DELETE FROM incidents")
    suspend fun deleteAllIncidents()

    /**
     * Obtiene la cantidad actual de reportes para verificar que el conteo sea 0.
     */
    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun getIncidentCount(): Int
}
