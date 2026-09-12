package com.example.proyecto_evacuapp.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.data.UserSessionState
import com.example.proyecto_evacuapp.data.local.TokenManager
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var userName by remember { mutableStateOf<String>(UserSessionState.currentUser.name) }
    var userEmail by remember { mutableStateOf<String>(UserSessionState.currentUser.email) }
    var userRole by remember { mutableStateOf<String>(UserSessionState.currentUser.role) }
    var userMobility by remember { mutableStateOf<String>("Vehículo") }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                val response = RetrofitClient.userApiService.getMe()
                if (response.isSuccessful && response.body() != null) {
                    val userBody = response.body()!!
                    UserSessionState.updateFromUserMeResponse(userBody)

                    userName = UserSessionState.currentUser.name
                    userEmail = UserSessionState.currentUser.email
                    userRole = UserSessionState.currentUser.role
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar perfil: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Perfil de Usuario",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Nombre: $userName", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Correo: $userEmail", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Rol: $userRole", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Movilidad: $userMobility", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Limpiar token y preferencias de sesión
                tokenManager.clearToken()
                val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                sharedPref.edit().clear().apply()

                Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesión", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }
    }
}