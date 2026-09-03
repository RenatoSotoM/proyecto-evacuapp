package com.example.proyecto_evacuapp.ui.components

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey
    val localId: String,
    val remoteId: String?,
    val type: String,
    val severity: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val alpha: Double,
    val beta: Double,
    val status: String,
    val affectedSegmentIds: String,
    val isOwnReport: Boolean
)