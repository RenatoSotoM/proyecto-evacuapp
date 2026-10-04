package com.example.proyecto_evacuapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.example.proyecto_evacuapp.ui.theme.*
import org.osmdroid.util.GeoPoint

/**
 * Pantalla de Alertas Tácticas reactiva conectada en tiempo real a IncidentSharedState y Room DB.
 */
@Composable
fun AlertsScreen() {
    val context = LocalContext.current
    val userLocation = UserLocationState.currentLocation ?: GeoPoint(-33.5925, -70.7045)
    
    // Lista reactiva de incidentes compartidos desde la base de datos Room
    val allIncidents = IncidentSharedState.incidents

    // Filtrar incidentes activos ordenados por proximidad
    val activeAlerts = remember(allIncidents, userLocation) {
        allIncidents
            .filter { it.status != IncidentStatus.REJECTED && it.status != IncidentStatus.SYNC_FAILED }
            .sortedBy { incident ->
                userLocation.distanceToAsDouble(GeoPoint(incident.latitude, incident.longitude))
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alertas y Reportes en Terreno",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Sincronizado en tiempo real desde la red mesh y base de datos local Room",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (allIncidents.isNotEmpty()) {
                IconButton(
                    onClick = {
                        IncidentSharedState.clearAllIncidents(context)
                        Toast.makeText(context, "🗑️ Todos los reportes locales han sido eliminados de Room DB", Toast.LENGTH_LONG).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Eliminar todos los reportes",
                        tint = DangerRed
                    )
                }
            }
        }

        if (activeAlerts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            text = "Zona Despejada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "No existen reportes de peligro activos en esta área.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            activeAlerts.forEach { incident ->
                val incPoint = GeoPoint(incident.latitude, incident.longitude)
                val distanceMeters = userLocation.distanceToAsDouble(incPoint)
                val formattedDistance = if (distanceMeters < 1000) {
                    "${distanceMeters.toInt()} m"
                } else {
                    "${"%.1f".format(distanceMeters / 1000.0)} km"
                }

                val minutesAgo = ((System.currentTimeMillis() - incident.createdAtMillis) / 60000).coerceAtLeast(1)
                val formattedTime = "Hace $minutesAgo min"

                AlertCardItem(
                    incident = incident,
                    distance = formattedDistance,
                    time = formattedTime,
                    onVerifyClick = {
                        IncidentSharedState.confirmIncident(incident.localId)
                        Toast.makeText(context, "✅ Confirmación registrada (+1 α)", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AlertCardItem(
    incident: SharedIncident,
    distance: String,
    time: String,
    onVerifyClick: () -> Unit
) {
    val (severityColor, severityBg) = when (incident.severity) {
        IncidentSeverity.CRITICA -> DangerRed to DangerRedLight
        IncidentSeverity.ALTA -> DangerRed to DangerRedLight
        IncidentSeverity.MEDIA -> WarningAmber to WarningAmberLight
        IncidentSeverity.BAJA -> SafeGreen to SafeGreenLight
    }

    val statusBadgeText = when (incident.status) {
        IncidentStatus.VERIFIED -> "VERIFICADO"
        IncidentStatus.PROBABLE -> "PROBABLE"
        IncidentStatus.LOCAL_PENDING, IncidentStatus.PENDING -> "PENDIENTE"
        else -> "LOCAL"
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = severityBg, shape = CircleShape) {
                        Text(
                            text = incident.type.emoji,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = incident.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "📍 $distance de ti · $time",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = severityBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = incident.severity.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (incident.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = incident.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🧠 Consenso Beta: ${incident.confidencePercentage}% de Validez",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EvacuBlue
                    )
                    Text(
                        text = "📊 α (confirmaciones): ${incident.alpha.toInt()} | β (rechazos): ${incident.beta.toInt()} · [$statusBadgeText]",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VERIFICAR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
