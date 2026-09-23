package com.example.proyecto_evacuapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine
import com.example.proyecto_evacuapp.ui.theme.DangerRed
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import com.example.proyecto_evacuapp.ui.theme.WarningAmber
import com.example.proyecto_evacuapp.ui.theme.WarningAmberLight

/**
 * Panel deslizable horizontal con las alternativas de ruta calculadas por RouteManager /
 * LocalRouteEngine.calculateRouteAlternatives. Se ubica sobre el panel de acción inferior
 * existente en MapScreen (por ejemplo, justo arriba del Card de "IR" / "CANCELAR").
 */
@Composable
fun RouteOptionsPanel(
    routes: List<LocalRouteResult>,
    selectedRoute: LocalRouteResult?,
    onRouteSelected: (LocalRouteResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (routes.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Elige tu ruta de evacuación",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(routes, key = { index, route -> "${route.variant}_$index" }) { _, route ->
                RouteOptionCard(
                    route = route,
                    isSelected = route == selectedRoute,
                    onClick = { onRouteSelected(route) }
                )
            }
        }
    }
}

private data class RouteVisualStyle(
    val icon: ImageVector,
    val accentColor: Color,
    val backgroundColor: Color,
    val badgeText: String?
)

private fun styleFor(route: LocalRouteResult): RouteVisualStyle = when (route.variant) {
    RouteVariant.PRINCIPAL -> RouteVisualStyle(
        icon = Icons.Default.Route,
        accentColor = EvacuBlue,
        backgroundColor = EvacuBlueLight,
        badgeText = null
    )
    RouteVariant.SEGURA -> RouteVisualStyle(
        icon = Icons.Default.Shield,
        accentColor = SafeGreen,
        backgroundColor = SafeGreenLight,
        badgeText = if (route.avoidsVerifiedRisk) "Evita riesgo verificado" else "Menor riesgo posible"
    )
    RouteVariant.ACCESIBLE -> RouteVisualStyle(
        icon = Icons.Default.Accessible,
        accentColor = WarningAmber,
        backgroundColor = WarningAmberLight,
        badgeText = "Ruta accesible"
    )
}

@Composable
private fun RouteOptionCard(
    route: LocalRouteResult,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val style = styleFor(route)

    Card(
        modifier = Modifier
            .width(190.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) style.backgroundColor else SurfaceWhite
        ),
        border = if (isSelected) BorderStroke(2.dp, style.accentColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.accentColor,
                    modifier = Modifier.height(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = route.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            val distanceText = LocalRouteEngine.formatDistance(route.distanceMeters)
            val durationText = LocalRouteEngine.formatDuration(route.durationSeconds)
            Text(
                text = "$distanceText · $durationText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = style.accentColor
            )

            if (!route.avoidsVerifiedRisk && route.variant != RouteVariant.SEGURA) {
                Text(
                    text = "⚠ Incluye tramo de riesgo",
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerRed
                )
            }

            style.badgeText?.let { badge ->
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}