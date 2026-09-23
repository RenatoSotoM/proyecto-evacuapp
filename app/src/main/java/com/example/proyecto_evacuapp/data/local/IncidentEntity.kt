package com.example.proyecto_evacuapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.osmdroid.util.GeoPoint

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val geoPoint: GeoPoint,           // ✅ no 'location'
    val severityLevel: String,        // ✅ no 'severity'
    val confirmations: Int,           // ✅ no 'verificationCount'
    val timestamp: Long,
    val status: String = "PENDING",
    val remoteId: String? = null
)