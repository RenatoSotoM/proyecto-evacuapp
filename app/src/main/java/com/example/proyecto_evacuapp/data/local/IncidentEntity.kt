package com.example.proyecto_evacuapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: String,
    val severity: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = "LOCAL_PENDING",
    val createdAtMillis: Long = System.currentTimeMillis()
)