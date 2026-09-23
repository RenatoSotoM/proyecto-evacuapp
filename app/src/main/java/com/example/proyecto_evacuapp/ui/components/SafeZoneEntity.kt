package com.example.proyecto_evacuapp.ui.components

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Entity(tableName = "safe_zones")
data class SafeZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val capacity: Int,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface SafeZoneDao {
    @Query("SELECT * FROM safe_zones")
    suspend fun getAllSafeZones(): List<SafeZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(zones: List<SafeZoneEntity>)
}