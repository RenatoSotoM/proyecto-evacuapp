package com.example.proyecto_evacuapp.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import org.osmdroid.views.overlay.Polyline

private const val SELECTED_STROKE_WIDTH = 15f
private const val ALTERNATE_STROKE_WIDTH = 8f
private const val ALTERNATE_ALPHA = 150 // 0-255

private const val COLOR_PRINCIPAL = "#1565D8" // EvacuBlue
private const val COLOR_SEGURA = "#2E7D32"    // SafeGreen
private const val COLOR_ACCESIBLE = "#B45309" // WarningAmber (tono oscuro)
private const val COLOR_ALT2 = "#7C3AED"      // Púrpura elegante

/**
 * Construye los overlays de mapa para el set de rutas alternativas calculado por RouteManager / LocalRouteEngine.
 * La ruta seleccionada se pinta sólida y gruesa; el resto queda punteado, delgado y
 * semitransparente. Las alternativas se agregan primero para que la seleccionada quede
 * siempre visualmente "encima" al añadirse al MapView.
 */
object RoutePolylineFactory {

    /**
     * @param onAlternateClicked callback invocado con la ruta tocada cuando el usuario toca
     *   una alternativa directamente en el mapa (permite alternar selección sin usar el panel).
     */
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
                strokeWidth = SELECTED_STROKE_WIDTH
                pathEffect = null
            } else {
                this.color = color
                alpha = ALTERNATE_ALPHA
                strokeWidth = ALTERNATE_STROKE_WIDTH
                pathEffect = DashPathEffect(floatArrayOf(22f, 16f), 0f)
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
