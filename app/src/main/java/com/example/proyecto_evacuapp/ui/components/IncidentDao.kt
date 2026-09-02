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
}