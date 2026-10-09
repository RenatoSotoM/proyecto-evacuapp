package com.example.proyecto_evacuapp

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Prueba unitaria para verificar matemáticamente el cálculo de distancia perpendicular
 * a la polilínea y el umbral de desvío de 40 metros para navegación segura.
 */
class PolylineDistanceTest {

    data class SimplePoint(val lat: Double, val lon: Double)

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun distancePointToSegmentMeters(point: SimplePoint, segA: SimplePoint, segB: SimplePoint): Double {
        val x = point.lon
        val y = point.lat
        val x1 = segA.lon
        val y1 = segA.lat
        val x2 = segB.lon
        val y2 = segB.lat

        val dx = x2 - x1
        val dy = y2 - y1

        if (dx == 0.0 && dy == 0.0) {
            return haversineMeters(point.lat, point.lon, segA.lat, segA.lon)
        }

        val t = (((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        val projLat = y1 + t * dy
        val projLon = x1 + t * dx
        return haversineMeters(point.lat, point.lon, projLat, projLon)
    }

    private fun distanceToPolylineMeters(point: SimplePoint, polyline: List<SimplePoint>): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) return haversineMeters(point.lat, point.lon, polyline.first().lat, polyline.first().lon)

        var minDistance = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val p1 = polyline[i]
            val p2 = polyline[i + 1]
            val dist = distancePointToSegmentMeters(point, p1, p2)
            if (dist < minDistance) {
                minDistance = dist
            }
        }
        return minDistance
    }

    @Test
    fun testPointOnPolylineReturnsLowDistance() {
        val polyline = listOf(
            SimplePoint(-33.5925, -70.7045),
            SimplePoint(-33.5925, -70.7100)
        )

        // Punto sobre la calle (a 12 metros de distancia)
        val pointOnStreet = SimplePoint(-33.5926, -70.7070)
        val distance = distanceToPolylineMeters(pointOnStreet, polyline)

        assertTrue("Distancia dentro de la calle debe ser < 40 m (Avance normal)", distance < 40.0)
    }

    @Test
    fun testPointOffRouteTriggersOffRouteThreshold() {
        val polyline = listOf(
            SimplePoint(-33.5925, -70.7045),
            SimplePoint(-33.5925, -70.7100)
        )

        // Punto desplazado a otra calle (a ~110 metros de distancia perpendicular)
        val pointOffRoute = SimplePoint(-33.5935, -70.7070)
        val distance = distanceToPolylineMeters(pointOffRoute, polyline)

        assertTrue("Distancia fuera de la ruta debe superar el umbral de 40 m", distance >= 40.0)
    }
}
