package com.example.proyecto_evacuapp.ui.components

import kotlin.math.cos

data class OfflineMapBounds(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
)

object OfflineMapManager {

    const val MIN_RADIUS_KM = 15.0
    const val MAX_RADIUS_KM = 20.0

    fun createBounds(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 15.0
    ): OfflineMapBounds {
        val safeRadius = radiusKm.coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM)

        val latitudeDelta = safeRadius / 111.32
        val longitudeDelta = safeRadius /
                (111.32 * cos(Math.toRadians(latitude)))

        return OfflineMapBounds(
            west = longitude - longitudeDelta,
            south = latitude - latitudeDelta,
            east = longitude + longitudeDelta,
            north = latitude + latitudeDelta
        )
    }
}