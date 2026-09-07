package com.example.proyecto_evacuapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.osmdroid.util.GeoPoint

@Composable
fun SosMeshtaticMenu(
    currentLocation: GeoPoint?,
    onSendMessage: (String) -> Unit
) {
    val locationText = if (currentLocation != null && currentLocation.latitude != 0.0) {
        "Lat: ${String.format("%.4f", currentLocation.latitude)}, Lon: ${String.format("%.4f", currentLocation.longitude)}"
    } else {
        "Ubicación no disponible"
    }

    val sosOptions = listOf(
        "✅ Estoy bien",
        "📍 Me encuentro en: $locationText",
        "📞 Llámame / Contáctame cuando puedas",
        "🚨 PIDE AYUDA: Requiero asistencia en mi posición ($locationText)",
        "⚠️ Camino / Vía bloqueada cerca de $locationText"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "📡 Transmitir Alerta por Meshtatic (Sin Internet)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Selecciona un mensaje para transmitir por la red mesh a los nodos cercanos:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        HorizontalDivider()

        sosOptions.forEach { optionText ->
            OutlinedButton(
                onClick = { onSendMessage(optionText) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = optionText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}