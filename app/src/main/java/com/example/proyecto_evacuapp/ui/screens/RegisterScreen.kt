package com.example.proyecto_evacuapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.data.UserSessionState
import com.example.proyecto_evacuapp.data.remote.RegisterRequest
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 1. PASO 1: DECLARAR LA VARIABLE DE ESTADO DEL TELÉFONO
    var nameText by remember { mutableStateOf("") }
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") } // <- Esta variable guarda el texto del fono

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Nombre Completo") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            label = { Text("Correo Electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. PASO 2: CAMPO DE TEXTO DEL TELÉFONO
        OutlinedTextField(
            value = phoneText,
            onValueChange = { phoneText = it },
            label = { Text("Teléfono (Opcional)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        // 3. PASO 3: PASAR phoneText EN LUGAR DE phone EN LA LÍNEA 145
                        val request = RegisterRequest(
                            name = nameText.trim(),
                            email = emailText.trim(),
                            password = passwordText,
                            phone = phoneText.trim().ifBlank { null } // <- Usa 'phoneText' aquí
                        )

                        val response = RetrofitClient.authApiService.register(request)
                        if (response.isSuccessful && response.body() != null) {
                            val authData = response.body()!!
                            RetrofitClient.authToken = authData.accessToken

                            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("jwt_token", authData.accessToken).apply()

                            UserSessionState.currentUser = UserSessionState.currentUser.copy(
                                name = authData.user.name,
                                email = authData.user.email,
                                isLoggedIn = true
                            )

                            onRegisterSuccess()
                        } else {
                            errorMessage = "Error en el registro (${response.code()})"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error de conexión con el servidor"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && nameText.isNotBlank() && emailText.isNotBlank() && passwordText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EvacuBlue)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("REGISTRARSE")
            }
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}