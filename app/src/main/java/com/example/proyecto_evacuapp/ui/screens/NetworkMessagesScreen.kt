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
import com.example.proyecto_evacuapp.comm.EmergencyCommManager
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import com.example.proyecto_evacuapp.ui.components.UserLocationState
import com.example.proyecto_evacuapp.ui.components.formatRelativeTime
import com.example.proyecto_evacuapp.ui.theme.*
import org.osmdroid.util.GeoPoint

/**
 * Pantalla dedicada al Canal de Terreno / Mensajes de Red Offline:
 * Muestra la bitácora reactiva de reportes comunitarios recibidos vía Bluetooth, SMS y Mesh.
 */
@Composable
fun NetworkMessagesScreen(
    onNavigateToMapWithPoint: (GeoPoint, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userLocation = UserLocationState.currentLocation ?: GeoPoint(-33.5925, -70.7045)

    var selectedChannelFilter by remember { mutableStateOf("TODOS") }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    // Lista reactiva desde Room DB
    val allIncidents = IncidentSharedState.incidents

    val filteredMessages = remember(allIncidents, selectedChannelFilter, userLocation) {
        allIncidents
            .filter { incident ->
                incident.status != IncidentStatus.REJECTED && incident.status != IncidentStatus.SYNC_FAILED
            }
            .filter { incident ->
                when (selectedChannelFilter) {
                    "SMS" -> incident.description.contains("[SMS", ignoreCase = true)
                    "BLUETOOTH" -> incident.description.contains("[Bluetooth", ignoreCase = true)
                    "MESH" -> incident.description.contains("[Mesh", ignoreCase = true) || incident.description.contains("Mesh", ignoreCase = true)
                    else -> true
                }
            }
            .sortedByDescending { it.createdAtMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Canal de Terreno (Red Offline)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Mensajes e incidentes recibidos vía Bluetooth LE, SMS y Mesh",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = { showBroadcastDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.CellTower,
                    contentDescription = "Transmitir en canal offline",
                    tint = EvacuBlue
                )
            }
        }

        // Filtro por canal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedChannelFilter == "TODOS",
                onClick = { selectedChannelFilter = "TODOS" },
                label = { Text("Todos") }
            )
            FilterChip(
                selected = selectedChannelFilter == "BLUETOOTH",
                onClick = { selectedChannelFilter = "BLUETOOTH" },
                label = { Text("📡 Bluetooth") }
            )
            FilterChip(
                selected = selectedChannelFilter == "SMS",
                onClick = { selectedChannelFilter = "SMS" },
                label = { Text("💬 SMS") }
            )
            FilterChip(
                selected = selectedChannelFilter == "MESH",
                onClick = { selectedChannelFilter = "MESH" },
                label = { Text("📻 Mesh") }
            )
        }

        if (filteredMessages.isEmpty()) {
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
                    Surface(color = EvacuBlueLight, shape = CircleShape) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = EvacuBlue,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Sin mensajes en el canal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "No hay paquetes de terreno recibidos para este filtro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            filteredMessages.forEach { incident ->
                val incPoint = GeoPoint(incident.latitude, incident.longitude)
                val distanceMeters = userLocation.distanceToAsDouble(incPoint)
                val distText = if (distanceMeters < 1000) "${distanceMeters.toInt()} m de ti" else "${"%.1f".format(distanceMeters / 1000.0)} km de ti"
                val relTimeText = formatRelativeTime(incident.createdAtMillis)

                val (channelBadgeText, channelColor) = when {
                    incident.description.contains("[SMS", ignoreCase = true) -> "💬 SMS Trama" to SafeGreen
                    incident.description.contains("[Bluetooth", ignoreCase = true) -> "📡 Bluetooth BLE" to EvacuBlue
                    else -> "📻 Red Mesh" to WarningAmber
                }

                NetworkMessageCardItem(
                    incident = incident,
                    distanceText = distText,
                    timeText = relTimeText,
                    channelBadgeText = channelBadgeText,
                    channelColor = channelColor,
                    onViewOnMapClick = {
                        onNavigateToMapWithPoint(incPoint, incident.type.displayName)
                    }
                )
            }
        }
    }

    if (showBroadcastDialog) {
        BroadcastOfflineDialog(
            userLat = userLocation.latitude,
            userLon = userLocation.longitude,
            onDismiss = { showBroadcastDialog = false },
            onSend = { type, desc, destSms ->
                EmergencyCommManager.broadcastAlertOffline(
                    context = context,
                    scope = scope,
                    type = type,
                    description = desc,
                    latitude = userLocation.latitude,
                    longitude = userLocation.longitude,
                    destinationSmsNumber = destSms,
                    onComplete = { smsSuccess, btCount ->
                        val msg = "Transmisión offline finalizada: SMS ${if (smsSuccess) "✅" else "❌"}, Nodos BT: $btCount"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                )
                showBroadcastDialog = false
            }
        )
    }
}

@Composable
fun NetworkMessageCardItem(
    incident: SharedIncident,
    distanceText: String,
    timeText: String,
    channelBadgeText: String,
    channelColor: Color,
    onViewOnMapClick: () -> Unit
) {
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
                    Text(
                        text = incident.type.emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = incident.type.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "📍 $distanceText · ⏱️ $timeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    color = channelColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = channelBadgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = channelColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = incident.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onViewOnMapClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvacuBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VER EN MAPA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BroadcastOfflineDialog(
    userLat: Double,
    userLon: Double,
    onDismiss: () -> Unit,
    onSend: (type: String, desc: String, destSms: String?) -> Unit
) {
    var typeText by remember { mutableStateOf("BLOQUEO_VIAL") }
    var descText by remember { mutableStateOf("Alerta de emergencia en terreno") }
    var destSmsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📡 Transmitir en Canal de Terreno", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transmitirá esta alerta mediante Bluetooth RFCOMM y Trama SMS compacta:", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = typeText,
                    onValueChange = { typeText = it },
                    label = { Text("Categoría (ej. INCENDIO, BLOQUEO_VIAL, ANEGAMIENTO)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Descripción corta") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destSmsText,
                    onValueChange = { destSmsText = it },
                    label = { Text("Número SMS Destino (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSend(typeText, descText, destSmsText.ifBlank { null })
                }
            ) {
                Text("DIFUNDIR OFFLINE")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
