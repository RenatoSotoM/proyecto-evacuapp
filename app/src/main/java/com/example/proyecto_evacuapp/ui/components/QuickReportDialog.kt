package com.example.proyecto_evacuapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import java.util.UUID

@Composable
fun QuickReportDialog(
    currentLat: Double?,
    currentLon: Double?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(IncidentType.BLOQUEO_VIAL) }
    var selectedSeverity by remember { mutableStateOf(IncidentSeverity.ALTA) }
    var descriptionText by remember { mutableStateOf("") }

    val categories = listOf(
        IncidentType.BLOQUEO_VIAL to "🚧 Corte de Ruta / Obstáculo",
        IncidentType.INUNDACION to "🌊 Anegamiento / Aluvión",
        IncidentType.INCENDIO to "🔥 Incendio / Fuego",
        IncidentType.OTRO to "⚠️ Otro Peligro"
    )

    val severities = listOf(
        IncidentSeverity.BAJA to "Baja",
        IncidentSeverity.MEDIA to "Media",
        IncidentSeverity.ALTA to "Alta",
        IncidentSeverity.CRITICA to "Crítica"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🚨 Reportar Incidente en Terreno", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Categoría del Peligro:", fontWeight = FontWeight.SemiBold)
                categories.forEach { (type, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(text = label, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Nivel de Severidad:", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    severities.forEach { (severity, label) ->
                        FilterChip(
                            selected = selectedSeverity == severity,
                            onClick = { selectedSeverity = severity },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Descripción adicional (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = currentLat ?: -33.5925
                    val lon = currentLon ?: -70.7045
                    val edgeId = com.example.proyecto_evacuapp.domain.engine.LocalRouteEngine.snapToNearestEdge(lat, lon)

                    val newSharedIncident = SharedIncident(
                        localId = UUID.randomUUID().toString(),
                        type = selectedType,
                        severity = selectedSeverity,
                        description = descriptionText.trim().ifBlank { "Reporte ciudadano en terreno" },
                        latitude = lat,
                        longitude = lon,
                        createdAtMillis = System.currentTimeMillis(),
                        updatedAtMillis = System.currentTimeMillis(),
                        alpha = 1.0,
                        beta = 1.0,
                        status = IncidentStatus.LOCAL_PENDING,
                        isOwnReport = true,
                        affectedEdgeId = edgeId
                    )

                    IncidentSharedState.addLocalIncident(newSharedIncident)
                    Toast.makeText(context, "🚨 Reporte guardado localmente en Room DB y renderizado en mapa", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EvacuBlue, contentColor = Color.White)
            ) {
                Text("GUARDAR Y TRANSMITIR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
