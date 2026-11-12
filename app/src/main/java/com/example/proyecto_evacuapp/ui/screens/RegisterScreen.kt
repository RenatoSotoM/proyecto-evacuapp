package com.example.proyecto_evacuapp.ui.screens

import android.content.Context
import android.util.Log
import android.util.Patterns
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var nameText by remember { mutableStateOf("") }
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var phoneText by remember { mutableStateOf("") }

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
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = emailText,
            onValueChange = { emailText = it },
            label = { Text("Correo Electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phoneText,
            onValueChange = { phoneText = it },
            label = { Text("Teléfono (Opcional)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passwordText,
            onValueChange = { passwordText = it },
            label = { Text("Contraseña (mínimo 6 caracteres)") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val name = nameText.trim()
                val email = emailText.trim()
                val password = passwordText

                if (name.isBlank()) {
                    errorMessage = "Por favor ingresa tu nombre completo."
                    return@Button
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Por favor ingresa un correo electrónico válido."
                    return@Button
                }

                if (password.length < 6) {
                    errorMessage = "La contraseña debe tener al menos 6 caracteres."
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val phoneVal = phoneText.trim().ifBlank { null }
                        val request = RegisterRequest(
                            name = name,
                            email = email,
                            password = password,
                            phone = phoneVal
                        )

                        Log.d("REGISTER_DIAG", "Enviando registro POST /auth/register: $email | Phone: $phoneVal")

                        val response = withContext(Dispatchers.IO) {
                            RetrofitClient.authApiService.register(request)
                        }

                        if (response.isSuccessful && response.body() != null) {
                            val authData = response.body()!!
                            Log.d("REGISTER_DIAG", "Registro exitoso en NestJS: ${authData.user.email}")
                            RetrofitClient.authToken = authData.accessToken

                            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("jwt_token", authData.accessToken).apply()

                            UserSessionState.currentUser = UserSessionState.currentUser.copy(
                                id = authData.user.id ?: "",
                                name = authData.user.name ?: name,
                                email = authData.user.email ?: email,
                                role = authData.user.role ?: "USER",
                                isLoggedIn = true
                            )

                            onRegisterSuccess()
                        } else {
                            val code = response.code()
                            val rawError = response.errorBody()?.string()
                            Log.e("REGISTER_DIAG", "HTTP Error $code al registrar: $rawError")

                            errorMessage = when {
                                code == 400 -> {
                                    val parsedMessage = parseNestJsErrorString(rawError)
                                    if (parsedMessage.isNotBlank()) parsedMessage else "Error en la solicitud (400). Revisa los datos ingresados."
                                }
                                code == 409 || (rawError != null && rawError.contains("already exists", ignoreCase = true)) -> {
                                    "El correo electrónico ya está registrado."
                                }
                                else -> "Error en el servidor (HTTP $code)"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("REGISTER_DIAG", "Excepción al registrar usuario: ${e.message}", e)
                        errorMessage = "Error de conexión con el servidor. Verifica tu red."
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

private fun parseNestJsErrorString(rawError: String?): String {
    if (rawError.isNullOrBlank()) return ""
    return try {
        val json = org.json.JSONObject(rawError)
        if (json.has("message")) {
            val msgObj = json.get("message")
            if (msgObj is org.json.JSONArray) {
                val list = mutableListOf<String>()
                for (i in 0 until msgObj.length()) {
                    val item = msgObj.getString(i)
                    val translated = when {
                        item.contains("password", ignoreCase = true) -> "La contraseña debe tener al menos 6 caracteres."
                        item.contains("email", ignoreCase = true) -> "El correo electrónico debe ser válido."
                        item.contains("already exists", ignoreCase = true) -> "El correo electrónico ya está registrado."
                        else -> item
                    }
                    list.add(translated)
                }
                list.distinct().joinToString("\n")
            } else {
                msgObj.toString()
            }
        } else {
            ""
        }
    } catch (_: Exception) {
        ""
    }
}
