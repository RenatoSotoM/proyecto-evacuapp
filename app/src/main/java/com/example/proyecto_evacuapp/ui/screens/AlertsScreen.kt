package com.example.proyecto_evacuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.theme.DangerRed
import com.example.proyecto_evacuapp.ui.theme.DangerRedLight
import com.example.proyecto_evacuapp.ui.theme.SafeGreen
import com.example.proyecto_evacuapp.ui.theme.SafeGreenLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import com.example.proyecto_evacuapp.ui.theme.WarningAmber
import com.example.proyecto_evacuapp.ui.theme.WarningAmberLight

@Composable
fun AlertsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Alertas activas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        AlertCardItem(
            icon = Icons.Default.Warning,
            title = "Bloqueo vial confirmado",
            description = "Calle Eyzaguirre con paso interrumpido por escombros",
            distance = "800 m de tu ubicación",
            time = "Hace 4 min",
            color = DangerRed,
            background = DangerRedLight
        )

        AlertCardItem(
            icon = Icons.Default.Warning,
            title = "Precaución: Inundación",
            description = "Acumulación de agua en paso bajo nivel Freire",
            distance = "1,4 km de tu ubicación",
            time = "Hace 15 min",
            color = WarningAmber,
            background = WarningAmberLight
        )

        AlertCardItem(
            icon = Icons.Default.CloudDone,
            title = "Ruta segura verificada",
            description = "Av. América despejada hacia Parque García de la Huerta",
            distance = "1,1 km de tu ubicación",
            time = "Hace 6 min",
            color = SafeGreen,
            background = SafeGreenLight
        )
    }
}

@Composable
fun AlertCardItem(
    icon: ImageVector,
    title: String,
    description: String,
    distance: String,
    time: String,
    color: Color,
    background: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
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
    }
}