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
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WheelchairPickup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.data.TransportMode
import com.example.proyecto_evacuapp.data.UserProfile
import com.example.proyecto_evacuapp.data.UserSessionState
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextPrimary
import com.example.proyecto_evacuapp.ui.theme.TextSecondary

@Composable
fun ProfileScreen() {
    var showEditDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }

    val user = UserSessionState.currentUser

    val mobilityIcon = when (user.transportMode) {
        TransportMode.VEHICLE -> Icons.Default.DirectionsCar
        TransportMode.WALKING -> Icons.Default.DirectionsWalk
        TransportMode.BICYCLE -> Icons.Default.DirectionsBike
        TransportMode.REDUCED_MOBILITY -> Icons.Default.WheelchairPickup
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Perfil de evacuación",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            TextButton(onClick = {
                if (user.isLoggedIn) {
                    UserSessionState.currentUser = user.copy(isLoggedIn = false)
                } else {
                    showLoginDialog = true
                }
            }) {
                Text(if (user.isLoggedIn) "Cerrar sesión" else "Iniciar sesión", fontWeight = FontWeight.Bold)
            }
        }

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
                        text = if (user.isLoggedIn) user.name else "Usuario Invitado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = user.locationZone,
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

        ProfileItemRow("Tipo de movilidad", user.transportMode.label, mobilityIcon)
        ProfileItemRow("Acompañantes", user.companions, Icons.Default.Person)
        ProfileItemRow("Zona base offline", user.locationZone, Icons.Default.LocationOn)

        Button(
            onClick = { showEditDialog = true },
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

    // Modal de Edición de Preferencias
    if (showEditDialog) {
        EditPreferencesDialog(
            currentProfile = user,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                UserSessionState.currentUser = updated
                showEditDialog = false
            }
        )
    }

    // Modal de Login / Registro
    if (showLoginDialog) {
        LoginRegisterDialog(
            onDismiss = { showLoginDialog = false },
            onLoginSuccess = { name, email ->
                UserSessionState.currentUser = user.copy(
                    name = name,
                    email = email,
                    isLoggedIn = true
                )
                showLoginDialog = false
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
    var selectedMode by remember { mutableStateOf(currentProfile.transportMode) }
    var companionsText by remember { mutableStateOf(currentProfile.companions) }
    var zoneText by remember { mutableStateOf(currentProfile.locationZone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Preferencias", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tipo de Movilidad:", fontWeight = FontWeight.SemiBold)
                TransportMode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = (selectedMode == mode),
                            onClick = { selectedMode = mode }
                        )
                        Text(mode.label)
                    }
                }

                OutlinedTextField(
                    value = companionsText,
                    onValueChange = { companionsText = it },
                    label = { Text("Acompañantes") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = zoneText,
                    onValueChange = { zoneText = it },
                    label = { Text("Zona base offline") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    currentProfile.copy(
                        transportMode = selectedMode,
                        companions = companionsText,
                        locationZone = zoneText
                    )
                )
            }) {
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
                        label = { Text("Nombre completo") }
                    )
                }
                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it },
                    label = { Text("Correo electrónico") }
                )
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation()
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
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
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