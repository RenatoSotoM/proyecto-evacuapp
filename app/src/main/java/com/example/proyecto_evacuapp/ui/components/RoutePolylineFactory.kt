package com.example.proyecto_evacuapp.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import org.osmdroid.views.overlay.Polyline

private const val SELECTED_STROKE_WIDTH = 20f  // Trazo principal prominente de alta visibilidad
private const val ALTERNATE_STROKE_WIDTH = 10f
private const val ALTERNATE_ALPHA = 180 // 0-255

private const val COLOR_SEGURA = "#00E5FF"    // Azul Cian de alta intensidad (P1 / Ruta Segura)
private const val COLOR_ALT1 = "#FF6D00"      // Naranja de alta visibilidad para rutas alternativas (P2)
private const val COLOR_ALT2 = "#FFAB00"      // Ámbar brillante (P3)
private const val COLOR_ACCESIBLE = "#7C3AED" // Púrpura elegante accesibilidad

/**
 * Construye los overlays de polilinea para el mapa con colores de alto contraste para situaciones de emergencia:
 * - Ruta Principal / Segura (P1): Azul Cian brillante de alta intensidad (`#00E5FF`, 20f).
 * - Ruta Alternativa (P2 / P3): Naranja/Ámbar de alta visibilidad (`#FF6D00` / `#FFAB00`).
 */
object RoutePolylineFactory {

    fun buildPolylines(
        routes: List<LocalRouteResult>,
        selectedRoute: LocalRouteResult?,
        onAlternateClicked: (LocalRouteResult) -> Unit = {}
    ): List<Polyline> {
        if (routes.isEmpty()) return emptyList()

        val alternates = routes.filter { it != selectedRoute }
        val selected = selectedRoute ?: routes.first()

        val overlays = mutableListOf<Polyline>()

        // 1. Trazado de sombra/borde oscuro de alto contraste bajo la ruta seleccionada
        val outlinePolyline = Polyline().apply {
            setPoints(selected.points.map { it.toGeoPoint() })
            outlinePaint.apply {
                color = AndroidColor.parseColor("#000000")
                alpha = 200
                strokeWidth = SELECTED_STROKE_WIDTH + 6f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
        }
        overlays += outlinePolyline

        // 2. Trazados de alternativas
        alternates.forEach { route ->
            overlays += route.toPolyline(isSelected = false, onAlternateClicked)
        }

        // 3. Trazado frontal de la ruta seleccionada
        overlays += selected.toPolyline(isSelected = true, onAlternateClicked)
        return overlays
    }

    private fun LocalRouteResult.toPolyline(
        isSelected: Boolean,
        onAlternateClicked: (LocalRouteResult) -> Unit
    ): Polyline {
        val route = this
        val polyline = Polyline().apply {
            setPoints(route.points.map { it.toGeoPoint() })
        }

        val baseColorHex = when (variant) {
            RouteVariant.SEGURA -> COLOR_SEGURA
            RouteVariant.ALTERNATIVA_1, RouteVariant.PRINCIPAL -> COLOR_ALT1
            RouteVariant.ALTERNATIVA_2 -> COLOR_ALT2
            RouteVariant.OFFLINE, RouteVariant.ACCESIBLE -> COLOR_ACCESIBLE
        }
        val color = AndroidColor.parseColor(baseColorHex)

        polyline.outlinePaint.apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            if (isSelected) {
                this.color = color
                alpha = 255
                strokeWidth = SELECTED_STROKE_WIDTH
                pathEffect = null
            } else {
                this.color = color
                alpha = ALTERNATE_ALPHA
                strokeWidth = ALTERNATE_STROKE_WIDTH
                pathEffect = DashPathEffect(floatArrayOf(24f, 16f), 0f)
            }
        }

        if (!isSelected) {
            polyline.setOnClickListener { _, _, _ ->
                onAlternateClicked(route)
                true
            }
        }

        return polyline
    }
}
