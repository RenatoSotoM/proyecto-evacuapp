package com.example.proyecto_evacuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.data.IncidentRepository
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.example.proyecto_evacuapp.ui.theme.DangerRed
import com.example.proyecto_evacuapp.ui.theme.DangerRedLight
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import com.example.proyecto_evacuapp.ui.theme.WarningAmber
import com.example.proyecto_evacuapp.ui.theme.WarningAmberLight
import org.osmdroid.util.GeoPoint

@Composable
fun AlertsScreen() {
    // Ubicación GPS actual del usuario o fallback a San Bernardo
    val userLocation = UserLocationState.currentLocation ?: GeoPoint(-33.5925, -70.7045)
    val maxRadiusMeters = 5000.0 // Radio de acción: 5 km

    // Filtrar reportes activos en un radio <= 5 km
    val nearbyAlerts = IncidentRepository.activeReports.filter { report ->
        userLocation.distanceToAsDouble(report.location) <= maxRadiusMeters
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Alertas activas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Incidentes reportados a menos de 5 km de tu posición",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Si no hay alertas en un radio de 5 km
        if (nearbyAlerts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = SafeGreenLight, shape = CircleShape) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Sin alertas cercanas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "No hay incidentes reportados en un radio de 5 km.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            // Renderizado dinámico de alertas cercanas
            nearbyAlerts.forEach { report ->
                val distanceMeters = userLocation.distanceToAsDouble(report.location)
                val formattedDistance = if (distanceMeters < 1000) {
                    "${distanceMeters.toInt()} m de tu ubicación"
                } else {
                    String.format("%.1f km de tu ubicación", distanceMeters / 1000.0)
                }

                val minutesAgo = ((System.currentTimeMillis() - report.timestamp) / 60000).coerceAtLeast(1)
                val formattedTime = "Hace $minutesAgo min"

                // Determinar ícono y paleta según severidad
                val (icon, color, background) = when (report.severity) {
                    "Alto" -> Triple(Icons.Default.Warning, DangerRed, DangerRedLight)
                    "Medio" -> Triple(Icons.Default.Warning, WarningAmber, WarningAmberLight)
                    else -> Triple(Icons.Default.CloudDone, SafeGreen, SafeGreenLight)
                }

                AlertCardItem(
                    title = report.title,
                    description = report.description,
                    distance = formattedDistance,
                    time = formattedTime,
                    verifications = report.verificationCount,
                    icon = icon,
                    color = color,
                    background = background,
                    onVerifyClick = { IncidentRepository.verifyReport(report.id) }
                )
            }
        }
    }
}

@Composable
fun AlertCardItem(
    title: String,
    description: String,
    distance: String,
    time: String,
    verifications: Int,
    icon: ImageVector,
    color: Color,
    background: Color,
    onVerifyClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(color = background, shape = CircleShape) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.padding(10.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text(text = "$distance · $time", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra inferior de verificación comunitarias
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EvacuBlueLight,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Verificado por $verifications usuario${if (verifications > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EvacuBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = onVerifyClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvacuBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+1 VERIFICAR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}