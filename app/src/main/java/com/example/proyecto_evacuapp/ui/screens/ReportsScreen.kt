package com.example.proyecto_evacuapp.ui.screens
// Cambios nuevos de reportes e incidente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.components.IncidentSeverity
import com.example.proyecto_evacuapp.ui.components.IncidentStatus
import com.example.proyecto_evacuapp.ui.components.IncidentType
import com.example.proyecto_evacuapp.ui.components.SharedIncident
import com.example.proyecto_evacuapp.ui.theme.AppBackground
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ReportType(
    val code: String,
    val emoji: String,
    val title: String,
    val description: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen() {
    val reportTypes = remember {
        listOf(
            ReportType(
                code = "BLOQUEO_VIAL",
                emoji = "🚧",
                title = "Calle bloqueada",
                description = "Escombros, corte de vía o paso no habilitado"
            ),
            ReportType(
                code = "INCENDIO",
                emoji = "🔥",
                title = "Incendio",
                description = "Fuego o propagación de humo denso"
            ),
            ReportType(
                code = "INUNDACION",
                emoji = "🌊",
                title = "Inundación",
                description = "Anegamiento en calzada o paso peatonal"
            ),
            ReportType(
                code = "DERRUMBE",
                emoji = "🪨",
                title = "Derrumbe",
                description = "Caída de material o fallas de muro"
            ),
            ReportType(
                code = "ACCIDENTE",
                emoji = "🚗",
                title = "Accidente vehicular",
                description = "Vehículo obstaculizando la ruta"
            ),
            ReportType(
                code = "RUTA_INACCESIBLE",
                emoji = "♿",
                title = "Ruta inaccesible",
                description = "Escaleras, desnivel, vereda rota o paso sin acceso"
            ),
            ReportType(
                code = "OTRO",
                emoji = "⚠️",
                title = "Otro peligro",
                description = "Riesgo no clasificado en las opciones anteriores"
            )
        )
    }

    val severityOptions = listOf(
        "Baja",
        "Media",
        "Alta",
        "Crítica"
    )

    var selectedType by rememberSaveable {
        mutableStateOf<ReportType?>(null)
    }

    var selectedSeverity by rememberSaveable {
        mutableStateOf("Media")
    }

    var reportDescription by rememberSaveable {
        mutableStateOf("")
    }

    var showReportForm by rememberSaveable {
        mutableStateOf(false)
    }

    var savedMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    /*
     * Ubicación temporal de demostración.
     *
     * En la siguiente etapa debe reemplazarse por GPS mediante
     * FusedLocationProviderClient y permisos de ubicación.
     */
    val demoLatitude = -33.4672
    val demoLongitude = -70.6576

    /*
     * Fuente de estado compartida entre ReportsScreen y RoutesScreen.
     *
     * Por ahora vive en memoria durante la sesión. Posteriormente debe
     * reemplazarse por Room para persistencia offline real.
     */
    val reports = IncidentSharedState.incidents

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Reportar incidente",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Informa bloqueos o peligros cercanos. El reporte se guarda primero en el dispositivo y se sincronizará cuando exista conexión.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        OfflineReportInfoCard(
            pendingCount = reports.count {
                it.status == IncidentStatus.LOCAL_PENDING ||
                        it.status == IncidentStatus.SYNC_FAILED
            }
        )

        if (!showReportForm) {
            Text(
                text = "Selecciona el tipo de incidente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            reportTypes.forEach { type ->
                ReportOptionItem(
                    icon = type.emoji,
                    title = type.title,
                    description = type.description,
                    onClick = {
                        selectedType = type
                        selectedSeverity = "Media"
                        reportDescription = ""
                        savedMessage = null
                        showReportForm = true
                    }
                )
            }
        } else {
            val activeType = selectedType

            if (activeType != null) {
                ReportFormCard(
                    type = activeType,
                    severityOptions = severityOptions,
                    selectedSeverity = selectedSeverity,
                    onSeverityChange = { selectedSeverity = it },
                    description = reportDescription,
                    onDescriptionChange = { reportDescription = it },
                    latitude = demoLatitude,
                    longitude = demoLongitude,
                    onCancel = {
                        showReportForm = false
                        selectedType = null
                        reportDescription = ""
                        savedMessage = null
                    },
                    onSave = {
                        IncidentSharedState.addLocalIncident(
                            SharedIncident(
                                type = reportTypeToIncidentType(activeType.code),
                                severity = severityToIncidentSeverity(
                                    selectedSeverity
                                ),
                                description = reportDescription
                                    .trim()
                                    .ifBlank {
                                        "Sin descripción adicional."
                                    },
                                latitude = demoLatitude,
                                longitude = demoLongitude,
                                status = IncidentStatus.LOCAL_PENDING,
                                affectedSegmentIds = affectedSegmentsFor(
                                    activeType.code
                                )
                            )
                        )

                        savedMessage =
                            "Reporte guardado localmente. Se sincronizará cuando exista conexión."

                        showReportForm = false
                        selectedType = null
                        reportDescription = ""
                    }
                )
            }
        }

        if (savedMessage != null) {
            Surface(
                color = SafeGreenLight,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = SafeGreen
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = savedMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }

        if (reports.isNotEmpty()) {
            HorizontalDivider(
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Reportes locales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Usa confirmar o descartar para simular el consenso Beta del incidente.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            reports.forEach { report ->
                SharedReportCard(
                    report = report,
                    onConfirm = {
                        IncidentSharedState.confirmIncident(report.localId)
                    },
                    onReject = {
                        IncidentSharedState.rejectIncident(report.localId)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportFormCard(
    type: ReportType,
    severityOptions: List<String>,
    selectedSeverity: String,
    onSeverityChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    latitude: Double,
    longitude: Double,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.emoji,
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = type.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "Nivel de severidad",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                severityOptions.forEach { severity ->
                    FilterChip(
                        selected = selectedSeverity == severity,
                        onClick = {
                            onSeverityChange(severity)
                        },
                        label = {
                            Text(text = severity)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = severityColor(
                                severity
                            ),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Descripción")
                },
                placeholder = {
                    Text("Ej.: Árbol caído bloquea ambas pistas.")
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EvacuBlue,
                    focusedLabelColor = EvacuBlue,
                    cursorColor = EvacuBlue
                )
            )

            Surface(
                color = EvacuBlueLight,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocationAlt,
                        contentDescription = null,
                        tint = EvacuBlue
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "Ubicación del reporte",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "Lat ${"%.5f".format(latitude)}, " +
                                    "Lon ${"%.5f".format(longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "GUARDAR REPORTE",
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "El reporte se guarda localmente primero; no se pierde si no hay conexión.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Text(
                text = "Cancelar",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onCancel()
                    }
                    .padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                style = MaterialTheme.typography.labelLarge,
                color = EvacuBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun OfflineReportInfoCard(
    pendingCount: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningAmberLight
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = WarningAmber,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Modo Offline-First",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = if (pendingCount == 0) {
                        "Los reportes se guardarán localmente y se sincronizarán al recuperar conexión."
                    } else {
                        "$pendingCount reporte(s) pendiente(s) de sincronización."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SharedReportCard(
    report: SharedIncident,
    onConfirm: () -> Unit,
    onReject: () -> Unit
) {
    val statusText = when (report.status) {
        IncidentStatus.LOCAL_PENDING -> "LOCAL"
        IncidentStatus.PENDING -> "PENDIENTE"
        IncidentStatus.PROBABLE -> "PROBABLE"
        IncidentStatus.VERIFIED -> "VERIFICADO"
        IncidentStatus.REJECTED -> "DESCARTADO"
        IncidentStatus.SYNC_FAILED -> "ERROR SYNC"
    }

    val statusColor = when (report.status) {
        IncidentStatus.VERIFIED -> SafeGreen
        IncidentStatus.PROBABLE -> WarningAmber
        IncidentStatus.LOCAL_PENDING,
        IncidentStatus.PENDING -> WarningAmber

        IncidentStatus.REJECTED,
        IncidentStatus.SYNC_FAILED -> DangerRed
    }

    val statusBackground = when (report.status) {
        IncidentStatus.VERIFIED -> SafeGreenLight
        IncidentStatus.PROBABLE -> WarningAmberLight
        IncidentStatus.LOCAL_PENDING,
        IncidentStatus.PENDING -> WarningAmberLight

        IncidentStatus.REJECTED,
        IncidentStatus.SYNC_FAILED -> DangerRedLight
    }

    val severityText = incidentSeverityLabel(report.severity)
    val severityColor = severityColor(severityText)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = incidentEmoji(report.type),
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = incidentTitle(report.type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = formatReportDate(report.createdAtMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    color = statusBackground,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = severityColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Severidad: $severityText",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = EvacuBlueLight,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Confianza: ${(report.confidence * 100).toInt()}%",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 3.dp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = EvacuBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )

            Text(
                text = "Ubicación: ${"%.5f".format(report.latitude)}, " +
                        "${"%.5f".format(report.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Text(
                text = "Consenso Beta: α = ${report.alpha.toInt()}, " +
                        "β = ${report.beta.toInt()}. " +
                        "Umbral de verificación: 75%.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "¿Confirmas este hecho?",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onConfirm,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Confirmar incidente",
                        tint = SafeGreen
                    )
                }

                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = "Descartar incidente",
                        tint = DangerRed
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportOptionItem(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Crear reporte de $title",
                tint = EvacuBlue
            )
        }
    }
}

private fun severityColor(severity: String): Color {
    return when (severity) {
        "Baja" -> SafeGreen
        "Media" -> WarningAmber
        "Alta" -> Color(0xFFF97316)
        "Crítica" -> DangerRed
        else -> WarningAmber
    }
}

private fun reportTypeToIncidentType(code: String): IncidentType {
    return when (code) {
        "BLOQUEO_VIAL" -> IncidentType.BLOQUEO_VIAL
        "INCENDIO" -> IncidentType.INCENDIO
        "INUNDACION" -> IncidentType.INUNDACION
        "DERRUMBE" -> IncidentType.DERRUMBE
        "ACCIDENTE" -> IncidentType.ACCIDENTE
        "RUTA_INACCESIBLE" -> IncidentType.RUTA_INACCESIBLE
        else -> IncidentType.OTRO
    }
}

private fun severityToIncidentSeverity(value: String): IncidentSeverity {
    return when (value) {
        "Baja" -> IncidentSeverity.BAJA
        "Media" -> IncidentSeverity.MEDIA
        "Alta" -> IncidentSeverity.ALTA
        "Crítica" -> IncidentSeverity.CRITICA
        else -> IncidentSeverity.MEDIA
    }
}

private fun incidentSeverityLabel(severity: IncidentSeverity): String {
    return when (severity) {
        IncidentSeverity.BAJA -> "Baja"
        IncidentSeverity.MEDIA -> "Media"
        IncidentSeverity.ALTA -> "Alta"
        IncidentSeverity.CRITICA -> "Crítica"
    }
}

private fun affectedSegmentsFor(typeCode: String): Set<String> {
    return when (typeCode) {
        "BLOQUEO_VIAL",
        "INUNDACION",
        "DERRUMBE",
        "RUTA_INACCESIBLE" -> setOf("av-viel-norte")

        else -> emptySet()
    }
}

private fun incidentEmoji(type: IncidentType): String {
    return when (type) {
        IncidentType.BLOQUEO_VIAL -> "🚧"
        IncidentType.INCENDIO -> "🔥"
        IncidentType.INUNDACION -> "🌊"
        IncidentType.DERRUMBE -> "🪨"
        IncidentType.ACCIDENTE -> "🚗"
        IncidentType.RUTA_INACCESIBLE -> "♿"
        IncidentType.OTRO -> "⚠️"
    }
}

private fun incidentTitle(type: IncidentType): String {
    return when (type) {
        IncidentType.BLOQUEO_VIAL -> "Calle bloqueada"
        IncidentType.INCENDIO -> "Incendio"
        IncidentType.INUNDACION -> "Inundación"
        IncidentType.DERRUMBE -> "Derrumbe"
        IncidentType.ACCIDENTE -> "Accidente vehicular"
        IncidentType.RUTA_INACCESIBLE -> "Ruta inaccesible"
        IncidentType.OTRO -> "Otro peligro"
    }
}

private fun formatReportDate(millis: Long): String {
    return SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(millis))
}