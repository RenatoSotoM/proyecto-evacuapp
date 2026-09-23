package com.example.proyecto_evacuapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

// 1. Entidad Room para almacenar tramos de calles y nodos offline en el celular
@Entity(tableName = "offline_street_nodes")
data class OfflineStreetNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zoneName: String,
    val latitude: Double,
    val longitude: Double,
    val sequenceOrder: Int // Orden del nodo para formar la calle paso a paso
)

// 2. DAO para consultar los nodos guardados localmente
@Dao
interface OfflineStreetNodeDao {
    @Query("SELECT * FROM offline_street_nodes WHERE zoneName = :zoneName ORDER BY sequenceOrder ASC")
    suspend fun getNodesForZone(zoneName: String): List<OfflineStreetNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNodes(nodes: List<OfflineStreetNodeEntity>)

    @Query("DELETE FROM offline_street_nodes")
    suspend fun clearAllNodes()
}