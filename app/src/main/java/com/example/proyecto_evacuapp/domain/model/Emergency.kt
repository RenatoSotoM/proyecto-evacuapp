package com.example.proyecto_evacuapp.domain.model

import org.osmdroid.util.GeoPoint

data class Emergency(
    val id: String,
    val type: String,
    val status: String,
    val title: String,
    val description: String?,
    val affectedAreaPoints: List<GeoPoint> // Convertimos el GeoJSON a puntos de Osmdroid
)