package com.example.proyecto_evacuapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.data.local.TokenManager
import com.example.proyecto_evacuapp.data.remote.LoginRequest
import com.example.proyecto_evacuapp.data.remote.RegisterRequest
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import kotlinx.coroutines.launch

@Composable
fun AuthTestScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    var emailText by remember { mutableStateOf("test@evacuapp.com") }
    var passwordText by remember { mutableStateOf("123456") }
    var statusText by remember { mutableStateOf("Ingresa credenciales y presiona un botón") }
    var isLoading by remember { mutableStateOf(false) }
    var hasToken by remember { mutableStateOf(!tokenManager.getToken().isNullOrEmpty()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Prueba Auth NestJS",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Botón REGISTRO
            Button(
                onClick = {
                    isLoading = true
                    statusText = "Registrando usuario..."
                    scope.launch {
                        try {
                            val request = RegisterRequest(
                                name = "Usuario Prueba",
                                email = emailText,
                                password = passwordText
                            )
                            val response = RetrofitClient.authApiService.register(request)
                            if (response.isSuccessful) {
                                val body = response.body()
                                body?.accessToken?.let { token ->
                                    tokenManager.saveToken(token)
                                    hasToken = true
                                }
                                statusText = "¡Registro exitoso!\nUsuario ID: ${body?.user?.id}\nRole: ${body?.user?.role}"
                            } else {
                                statusText = "Error en registro (${response.code()}):\n${response.errorBody()?.string()}"
                            }
                        } catch (e: Exception) {
                            statusText = "Error de red:\n${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Registrar")
            }

            // Botón LOGIN
            Button(
                onClick = {
                    isLoading = true
                    statusText = "Iniciando sesión..."
                    scope.launch {
                        try {
                            val request = LoginRequest(email = emailText, password = passwordText)
                            val response = RetrofitClient.authApiService.login(request)
                            if (response.isSuccessful) {
                                val body = response.body()
                                body?.accessToken?.let { token ->
                                    tokenManager.saveToken(token)
                                    hasToken = true
                                }
                                statusText = "¡Login exitoso!\nToken recibido correctamente.\nUsuario: ${body?.user?.name}"
                            } else {
                                statusText = "Error en login (${response.code()}):\n${response.errorBody()?.string()}"
                            }
                        } catch (e: Exception) {
                            statusText = "Error de red:\n${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Iniciar Sesión")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Probar /users/me con el Token obtenido
        OutlinedButton(
            onClick = {
                isLoading = true
                statusText = "Consultando /users/me..."
                scope.launch {
                    try {
                        val response = RetrofitClient.userApiService.getMe()
                        if (response.isSuccessful) {
                            statusText = "¡Perfil Obtenido de NestJS!\n${response.body()}"
                        } else {
                            statusText = "Error en /users/me: Code ${response.code()}"
                        }
                    } catch (e: Exception) {
                        statusText = "Error: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && hasToken,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Probar GET /users/me (Protegido)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}