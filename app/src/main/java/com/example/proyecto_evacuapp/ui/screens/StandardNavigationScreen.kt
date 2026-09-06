package com.example.proyecto_evacuapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.osmdroid.util.GeoPoint

@Composable
fun StandardNavigationScreen(
    destinationName: String,
    destinationPoint: GeoPoint,
    mobilityMode: String,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Navegación Estándar",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Destino: $destinationName")
            Text(text = "Modo: $mobilityMode")
            Text(text = "Coordenadas: ${destinationPoint.latitude}, ${destinationPoint.longitude}")

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onFinish) {
                Text(text = "Finalizar Ruta")
            }
        }
    }
}