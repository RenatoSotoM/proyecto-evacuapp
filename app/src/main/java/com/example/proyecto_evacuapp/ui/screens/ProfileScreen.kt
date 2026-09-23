package com.example.proyecto_evacuapp.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.data.TransportMode
import com.example.proyecto_evacuapp.data.UserProfile
import com.example.proyecto_evacuapp.data.UserSessionState
import com.example.proyecto_evacuapp.data.remote.RetrofitClient
import com.example.proyecto_evacuapp.data.remote.UpdateMobilityProfileRequest
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = UserSessionState.currentUser

    // 1. OBTENER DATOS DE NESTJS SOLO SI ESTÁ LOGUEADO
    LaunchedEffect(user.isLoggedIn) {
        if (user.isLoggedIn) {
            isLoading = true
            try {
                val response = RetrofitClient.userApiService.getMe()
                if (response.isSuccessful && response.body() != null) {
                    val remoteUser = response.body()!!
                    val profile = remoteUser.mobilityProfile

                    UserSessionState.currentUser = user.copy(
                        id = remoteUser.id,
                        name = remoteUser.name,
                        email = remoteUser.email,
                        role = remoteUser.role ?: "USER",
                        mobilityType = profile?.mobilityType ?: "PEATON",
                        requiresAccessibleRoute = profile?.requiresAccessibleRoute ?: false,
                        travelsWithMinors = profile?.travelsWithMinors ?: false,
                        companionCount = profile?.companionCount ?: 0
                    )
                }
            } catch (e: Exception) {
                Log.e("PROFILE", "Error al obtener perfil desde la API", e)
            } finally {
                isLoading = false
            }
        }
    }

    val mobilityIcon = when (user.mobilityType.uppercase()) {
        "VEHICULO" -> Icons.Default.DirectionsCar
        "PEATON" -> Icons.Default.DirectionsWalk
        "BICICLETA" -> Icons.Default.DirectionsBike
        "PERSONA_MOVILIDAD_REDUCIDA" -> Icons.Default.WheelchairPickup
        else -> Icons.Default.DirectionsWalk
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado con Botón de Cerrar/Iniciar Sesión
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfil de Evacuación",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            TextButton(
                onClick = {
                    if (user.isLoggedIn) {
                        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        RetrofitClient.authToken = null
                        UserSessionState.clear()
                        onLogout()
                    } else {
                        showLoginDialog = true
                    }
                }
            ) {
                Icon(
                    imageVector = if (user.isLoggedIn) Icons.Default.Logout else Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (user.isLoggedIn) "Cerrar sesión" else "Iniciar sesión",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EvacuBlue)
        }

        // Tarjeta de Usuario (Identifica si es Invitado o Usuario Registrado)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(color = EvacuBlueLight, shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = EvacuBlue,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(50.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (user.isLoggedIn) user.name else "Usuario Invitado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (user.isLoggedIn) user.email else "Sin sesión iniciada (Modo Local)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Banner promocional para Invitados
        if (!user.isLoggedIn) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EvacuBlueLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Modo Invitado activo",
                        fontWeight = FontWeight.Bold,
                        color = EvacuBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tus preferencias se guardan solo en este dispositivo. Inicia sesión para sincronizarlas con el servidor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showLoginDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Iniciar sesión / Registrarse", fontWeight = FontWeight.Bold, color = EvacuBlue)
                    }
                }
            }
        }

        Text(
            text = "Parámetros de Movilidad",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Filas de Detalles de Perfil
        ProfileItemRow("Tipo de movilidad", formatMobilityType(user.mobilityType), mobilityIcon)
        ProfileItemRow("Ruta accesible", if (user.requiresAccessibleRoute) "Sí" else "No", Icons.Default.WheelchairPickup)
        ProfileItemRow("Viaja con menores", if (user.travelsWithMinors) "Sí" else "No", Icons.Default.ChildCare)
        ProfileItemRow("Nº de acompañantes", user.companionCount.toString(), Icons.Default.Group)

        if (user.companions.isNotBlank()) {
            ProfileItemRow("Detalles de acompañantes", user.companions, Icons.Default.People)
        }
        if (user.locationZone.isNotBlank()) {
            ProfileItemRow("Zona base offline", user.locationZone, Icons.Default.LocationOn)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showEditDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EvacuBlue,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "EDITAR PREFERENCIAS", fontWeight = FontWeight.Bold)
        }
    }

    // Diálogo de Edición de Preferencias
    if (showEditDialog) {
        EditPreferencesDialog(
            currentProfile = user,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProfile ->
                // Guardar localmente
                UserSessionState.currentUser = updatedProfile
                showEditDialog = false

                // Sincronizar solo si hay sesión iniciada
                if (user.isLoggedIn) {
                    scope.launch {
                        val success = syncMobilityProfileToBackend(updatedProfile)
                        val msg = if (success) "Preferencias guardadas en el servidor" else "Guardado localmente (sin conexión)"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Preferencias guardadas localmente (Invitado)", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Diálogo de Login Rápido
    if (showLoginDialog) {
        LoginRegisterDialog(
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = { name, email ->
                UserSessionState.currentUser = user.copy(name = name, email = email, isLoggedIn = true)
                showLoginDialog = false
                Toast.makeText(context, "Sesión iniciada como $name", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EditPreferencesDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var selectedMobilityType by remember { mutableStateOf(currentProfile.mobilityType) }
    var requiresAccessibleRoute by remember { mutableStateOf(currentProfile.requiresAccessibleRoute) }
    var travelsWithMinors by remember { mutableStateOf(currentProfile.travelsWithMinors) }
    var companionCount by remember { mutableStateOf(currentProfile.companionCount) }
    var companionsText by remember { mutableStateOf(currentProfile.companions) }
    var zoneText by remember { mutableStateOf(currentProfile.locationZone) }

    val options = listOf(
        "PEATON" to "Peatón",
        "VEHICULO" to "Vehículo / Automóvil",
        "BICICLETA" to "Bicicleta",
        "PERSONA_MOVILIDAD_REDUCIDA" to "Movilidad Reducida"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Preferencias", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Tipo de movilidad:", fontWeight = FontWeight.SemiBold, color = TextPrimary)

                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMobilityType == key,
                            onClick = { selectedMobilityType = key }
                        )
                        Text(text = label, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = requiresAccessibleRoute,
                        onCheckedChange = { requiresAccessibleRoute = it }
                    )
                    Text("Requiere ruta accesible")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = travelsWithMinors,
                        onCheckedChange = { travelsWithMinors = it }
                    )
                    Text("Viaja con menores de edad")
                }

                Column {
                    Text("Nº de acompañantes: $companionCount", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = companionCount.toFloat(),
                        onValueChange = { companionCount = it.roundToInt() },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                }

                OutlinedTextField(
                    value = companionsText,
                    onValueChange = { companionsText = it },
                    label = { Text("Detalle acompañantes (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = zoneText,
                    onValueChange = { zoneText = it },
                    label = { Text("Zona base offline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val matchingMode = TransportMode.entries.find { it.backendValue == selectedMobilityType }
                        ?: TransportMode.VEHICLE

                    onSave(
                        currentProfile.copy(
                            mobilityType = selectedMobilityType,
                            requiresAccessibleRoute = requiresAccessibleRoute,
                            travelsWithMinors = travelsWithMinors,
                            companionCount = companionCount,
                            companions = companionsText,
                            locationZone = zoneText,
                            transportMode = matchingMode
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun LoginRegisterDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (String, String) -> Unit
) {
    var isRegister by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }
    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isRegister) "Crear Cuenta" else "Iniciar Sesión", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRegister) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Nombre completo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it },
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { isRegister = !isRegister }) {
                    Text(if (isRegister) "¿Tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalName = if (nameText.isBlank()) "Usuario EvacuApp" else nameText
                onLoginSuccess(finalName, emailText)
            }) {
                Text(if (isRegister) "Registrar" else "Ingresar")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ProfileItemRow(title: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = EvacuBlueLight, shape = CircleShape) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EvacuBlue,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}

private fun formatMobilityType(type: String): String {
    return when (type.uppercase()) {
        "VEHICULO" -> "Vehículo / Automóvil"
        "PEATON" -> "Peatón"
        "BICICLETA" -> "Bicicleta"
        "PERSONA_MOVILIDAD_REDUCIDA" -> "Movilidad Reducida"
        else -> type
    }
}

private suspend fun syncMobilityProfileToBackend(profile: UserProfile): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val dto = UpdateMobilityProfileRequest(
                mobilityType = profile.mobilityType,
                requiresAccessibleRoute = profile.requiresAccessibleRoute,
                travelsWithMinors = profile.travelsWithMinors,
                companionCount = profile.companionCount
            )
            val response = RetrofitClient.userApiService.updateMobilityProfile(dto)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("PROFILE", "Error al sincronizar perfil con el servidor", e)
            false
        }
    }
}