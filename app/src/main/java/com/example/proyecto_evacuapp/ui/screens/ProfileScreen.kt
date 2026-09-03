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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary

@Composable
fun ProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Perfil de evacuación",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
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
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = EvacuBlue,
                        modifier = Modifier.padding(8.dp).size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "José Cataldo / Renato Soto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "San Bernardo, Santiago",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Text(
            text = "Parámetros de movilidad",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        ProfileItemRow("Tipo de movilidad", "A pie", Icons.Default.Person)
        ProfileItemRow("Acompañantes", "Adultos · Niños", Icons.Default.AccountCircle)
        ProfileItemRow("Zona base offline", "San Bernardo (OSM)", Icons.Default.LocationOn)

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EvacuBlue,
                contentColor = androidx.compose.ui.graphics.Color.White
            )
        ) {
            Text(text = "EDITAR PREFERENCIAS", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileItemRow(title: String, value: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = EvacuBlueLight, shape = CircleShape) {
                Icon(imageVector = icon, contentDescription = null, tint = EvacuBlue, modifier = Modifier.padding(9.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}