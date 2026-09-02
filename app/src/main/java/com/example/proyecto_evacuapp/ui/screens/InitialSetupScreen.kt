package com.example.proyecto_evacuapp.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.theme.AppBackground
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.OutlineColor
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary

@Composable
fun InitialSetupScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var selectedMobility by remember { mutableStateOf("A pie") }
    var selectedCompanions by remember { mutableStateOf(setOf("Solo")) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Text(
                text = "Configura EvacuApp",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Paso $step de 3",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = EvacuBlue,
                trackColor = OutlineColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (step) {
                    1 -> SetupLocationContent()
                    2 -> SetupMobilityContent(
                        selectedMobility = selectedMobility,
                        onSelectMobility = { selectedMobility = it }
                    )
                    3 -> SetupOfflineMapContent(
                        selectedCompanions = selectedCompanions,
                        onToggleCompanion = { companion ->
                            selectedCompanions = when {
                                companion == "Solo" -> setOf("Solo")
                                companion in selectedCompanions -> {
                                    val updated = selectedCompanions - companion
                                    if (updated.isEmpty()) setOf("Solo") else updated
                                }
                                else -> (selectedCompanions - "Solo") + companion
                            }
                        }
                    )
                }
            }

            Button(
                onClick = { if (step < 3) step++ else onFinish() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvacuBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (step == 3) "CONFIGURAR EVACUAPP" else "CONTINUAR",
                    fontWeight = FontWeight.Bold
                )
            }

            if (step > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EvacuBlue)
                ) {
                    Text(text = "VOLVER", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SetupLocationContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "¿Cuál es tu zona principal?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Descargaremos capas OSM y puntos de encuentro para uso offline.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = EvacuBlueLight, shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EvacuBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "San Bernardo, Santiago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Zona piloto OSM lista para descarga",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupMobilityContent(
    selectedMobility: String,
    onSelectMobility: (String) -> Unit
) {
    val options = listOf(
        "A pie" to "🚶",
        "Vehículo" to "🚗",
        "Bicicleta" to "🚲",
        "Movilidad reducida" to "♿"
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "¿Cómo te desplazas habitualmente?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (name, emoji) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMobility == name) EvacuBlueLight else SurfaceWhite
                        ),
                        border = BorderStroke(
                            width = if (selectedMobility == name) 2.dp else 1.dp,
                            color = if (selectedMobility == name) EvacuBlue else OutlineColor
                        ),
                        onClick = { onSelectMobility(name) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetupOfflineMapContent(
    selectedCompanions: Set<String>,
    onToggleCompanion: (String) -> Unit
) {
    val companions = listOf("Solo", "Adultos", "Niños", "Adulto mayor", "Mascotas")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Información offline y acompañantes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = SafeGreenLight, shape = CircleShape) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mosaico OSM San Bernardo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mapnik OSM · Zonas seguras oficiales",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Text(
            text = "¿Viajas acompañado?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            companions.forEach { companion ->
                FilterChip(
                    selected = companion in selectedCompanions,
                    onClick = { onToggleCompanion(companion) },
                    label = { Text(text = companion, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = if (companion in selectedCompanions) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EvacuBlue,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        labelColor = TextPrimary
                    )
                )
            }
        }
    }
}