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

    @Query(
        """
        DELETE FROM incidents
        WHERE localId = :localId
        """
    )
    suspend fun deleteByLocalId(localId: String)

    // --- NUEVOS MÉTODOS PARA PRUEBAS Y LIMPIEZA ---

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