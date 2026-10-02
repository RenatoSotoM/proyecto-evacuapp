package com.example.proyecto_evacuapp.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import org.osmdroid.views.overlay.Polyline

private const val SELECTED_STROKE_WIDTH = 19f // Ruta segura gruesa y prominente
private const val ALTERNATE_STROKE_WIDTH = 9f
private const val ALTERNATE_ALPHA = 160 // 0-255

private const val COLOR_PRINCIPAL = "#1565D8" // EvacuBlue (Dobles sentido / Vías principales)
private const val COLOR_SEGURA = "#00E676"    // Verde brillante de alta visibilidad para ruta segura
private const val COLOR_ACCESIBLE = "#F59E0B" // Ámbar/Naranja (Un sentido / Vías secundarias)
private const val COLOR_ALT2 = "#7C3AED"      // Púrpura elegante

/**
 * Construye los overlays de mapa con alto contraste para las rutas de evacuación.
 * - Ruta segura activa: Verde brillante y grueso (`#00E676`, 19f).
 * - Vías de doble sentido: Azul neutro de alta visibilidad (`#1565D8`).
 * - Vías alternativas / un sentido: Tonos ámbar/naranja (`#F59E0B`).
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
        alternates.forEach { route ->
            overlays += route.toPolyline(isSelected = false, onAlternateClicked)
        }
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
            RouteVariant.ALTERNATIVA_1, RouteVariant.PRINCIPAL -> COLOR_PRINCIPAL
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
                strokeWidth = if (variant == RouteVariant.SEGURA) SELECTED_STROKE_WIDTH + 2f else SELECTED_STROKE_WIDTH
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
