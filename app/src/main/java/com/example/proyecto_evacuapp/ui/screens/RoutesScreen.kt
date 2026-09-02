package com.example.proyecto_evacuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import com.example.proyecto_evacuapp.ui.theme.WarningAmber
import com.example.proyecto_evacuapp.ui.theme.WarningAmberLight

@Composable
fun RoutesScreen(onSelectRoute: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Rutas de evacuación (OSRM)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Rutas calculadas sobre la red vial de San Bernardo ponderadas por riesgo.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        RouteCardItem(
            title = "Ruta recomendada",
            destination = "Parque García de la Huerta",
            distance = "1,4 km",
            time = "12 min",
            risk = "Bajo",
            riskColor = SafeGreen,
            riskBackground = SafeGreenLight,
            isRecommended = true,
            onClick = onSelectRoute
        )

        RouteCardItem(
            title = "Ruta alternativa 1",
            destination = "Plaza de Armas San Bernardo",
            distance = "1,7 km",
            time = "15 min",
            risk = "Muy bajo",
            riskColor = SafeGreen,
            riskBackground = SafeGreenLight,
            isRecommended = false,
            onClick = onSelectRoute
        )

        RouteCardItem(
            title = "Ruta alternativa 2",
            destination = "Estadio Municipal",
            distance = "1,1 km",
            time = "10 min",
            risk = "Medio",
            riskColor = WarningAmber,
            riskBackground = WarningAmberLight,
            isRecommended = false,
            onClick = onSelectRoute
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WarningAmberLight)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Se excluyen automáticamente tramos con reportes confirmados por consenso.",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun RouteCardItem(
    title: String,
    destination: String,
    distance: String,
    time: String,
    risk: String,
    riskColor: Color,
    riskBackground: Color,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended) EvacuBlueLight else SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRecommended) 4.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = destination, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }

                if (isRecommended) {
                    Surface(color = EvacuBlue, shape = RoundedCornerShape(50)) {
                        Text(
                            text = "RECOMENDADA",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "$time · $distance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Surface(color = riskBackground, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "Riesgo: $risk",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = riskColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecommended) SafeGreen else EvacuBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRecommended) "INICIAR NAVEGACIÓN" else "SELECCIONAR RUTA",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}