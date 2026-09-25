package com.example.proyecto_evacuapp.domain.engine

import com.example.proyecto_evacuapp.ui.components.RouteCoordinate

data class OfflineBoundingBox(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
) {
    fun contains(point: RouteCoordinate): Boolean {
        return point.latitude in south..north && point.longitude in west..east
    }

    fun center(): RouteCoordinate = RouteCoordinate((north + south) / 2.0, (east + west) / 2.0)
}

/**
 * Helper para delimitar el área local de cartografía y red vial a un radio máximo
 * de 25 km alrededor de la posición GPS del usuario (~±0.225° lat/lon).
 */
fun calculateOfflineBoundingBox(lat: Double, lon: Double, radiusKm: Double = 25.0): OfflineBoundingBox {
    val latDelta = radiusKm / 111.0
    val cosLat = kotlin.math.cos(Math.toRadians(lat)).coerceAtLeast(0.01)
    val lonDelta = radiusKm / (111.0 * cosLat)

    return OfflineBoundingBox(
        north = lat + latDelta,
        south = lat - latDelta,
        east = lon + lonDelta,
        west = lon - lonDelta
    )
}
