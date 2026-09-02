package com.example.proyecto_evacuapp

import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.theme.ProyectoevacuappTheme

private val EvacuBlue = Color(0xFF145DA0)
private val EvacuBlueDark = Color(0xFF0A3D62)
private val EvacuBlueLight = Color(0xFFE8F2FB)

private val SafeGreen = Color(0xFF16803C)
private val SafeGreenLight = Color(0xFFEAF7EE)

private val WarningAmber = Color(0xFFC77900)
private val WarningAmberLight = Color(0xFFFFF4DE)

private val AppBackground = Color(0xFFF5F7FA)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF17202A)
private val TextSecondary = Color(0xFF5C6770)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProyectoevacuappTheme {
                EvacuAppFlow()
            }
        }
    }
}

private enum class AppScreen {
    SPLASH,
    ONBOARDING,
    INITIAL_SETUP,
    MAP
}

@Composable
fun EvacuAppFlow() {
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

    when (currentScreen) {
        AppScreen.SPLASH -> SplashScreen(
            onContinue = { currentScreen = AppScreen.ONBOARDING }
        )

        AppScreen.ONBOARDING -> OnboardingScreen(
            onFinish = { currentScreen = AppScreen.INITIAL_SETUP }
        )

        AppScreen.INITIAL_SETUP -> InitialSetupScreen(
            onFinish = { currentScreen = AppScreen.MAP }
        )

        AppScreen.MAP -> MapDashboardScreen()
    }
}

@Composable
fun SplashScreen(onContinue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EvacuBlueDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "EvacuApp",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(28.dp)
                        .size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "EvacuApp",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Orientación inteligente para emergencias",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(56.dp))

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Preparando información de emergencia...",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = EvacuBlueDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "CONTINUAR",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }

    val onboardingPages = listOf(
        OnboardingData(
            icon = Icons.Default.Download,
            title = "Prepárate antes de una emergencia",
            description = "EvacuApp mantiene información importante disponible en tu dispositivo para ayudarte incluso cuando no tengas conexión."
        ),
        OnboardingData(
            icon = Icons.Default.Navigation,
            title = "Encuentra una ruta segura",
            description = "Utiliza tu ubicación y las condiciones disponibles para encontrar una ruta hacia una zona segura."
        ),
        OnboardingData(
            icon = Icons.Default.Security,
            title = "Informa lo que ocurre",
            description = "Reporta calles bloqueadas, inundaciones, incendios u otros peligros para ayudar a mejorar la información disponible."
        )
    )

    val currentPage = onboardingPages[page]

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Omitir",
                    color = EvacuBlue,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = EvacuBlueLight,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = currentPage.icon,
                    contentDescription = null,
                    tint = EvacuBlue,
                    modifier = Modifier
                        .padding(36.dp)
                        .size(92.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = currentPage.title,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = currentPage.description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            OnboardingIndicator(
                currentPage = page,
                pageCount = onboardingPages.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (page < onboardingPages.lastIndex) {
                        page++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvacuBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (page == onboardingPages.lastIndex) {
                        "COMENZAR"
                    } else {
                        "SIGUIENTE"
                    },
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Puedes modificar estas opciones más adelante.",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class OnboardingData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

@Composable
fun OnboardingIndicator(
    currentPage: Int,
    pageCount: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (index == currentPage) 28.dp else 8.dp,
                        height = 8.dp
                    )
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) EvacuBlue else Color(0xFFD7DEE5)
                    )
            )
        }
    }
}

@Composable
fun InitialSetupScreen(onFinish: () -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var selectedMobility by remember { mutableStateOf("A pie") }

    var selectedCompanions by remember {
        mutableStateOf(setOf("Solo"))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Configura EvacuApp",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Paso $step de 3",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { step / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = EvacuBlue,
                trackColor = Color(0xFFDDE5EC)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (step) {
                    1 -> SetupLocationStep()

                    2 -> SetupMobilityStep(
                        selectedMobility = selectedMobility,
                        onSelectMobility = { selectedMobility = it }
                    )

                    3 -> SetupOfflineMapStep(
                        selectedCompanions = selectedCompanions,
                        onToggleCompanion = { companion ->
                            selectedCompanions = when {
                                companion == "Solo" -> setOf("Solo")

                                companion in selectedCompanions -> {
                                    val updated = selectedCompanions - companion
                                    if (updated.isEmpty()) setOf("Solo") else updated
                                }

                                else -> (selectedCompanions - "Solo") + companion
                            }
                        }
                    )
                }
            }

            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvacuBlue,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (step == 3) {
                        "CONFIGURAR EVACUAPP"
                    } else {
                        "CONTINUAR"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (step > 1) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { step-- },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EvacuBlue
                    )
                ) {
                    Text(
                        text = "VOLVER",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SetupLocationStep() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "¿Cuál es tu zona principal?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Descargaremos información importante para que puedas usar EvacuApp aun sin conexión.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EvacuBlueLight,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = EvacuBlue,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "San Bernardo, Santiago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = "Zona recomendada para descargar",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EvacuBlue
            )
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("USAR MI UBICACIÓN ACTUAL")
        }
    }
}

@Composable
fun SetupMobilityStep(
    selectedMobility: String,
    onSelectMobility: (String) -> Unit
) {
    val mobilityOptions = listOf(
        "A pie" to "🚶",
        "Vehículo" to "🚗",
        "Bicicleta" to "🚲",
        "Movilidad reducida" to "♿"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "¿Cómo te desplazarías?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Usaremos esta información para adaptar las rutas recomendadas.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        mobilityOptions.chunked(2).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowOptions.forEach { (name, emoji) ->
                    MobilityOptionCard(
                        modifier = Modifier.weight(1f),
                        name = name,
                        emoji = emoji,
                        selected = selectedMobility == name,
                        onClick = { onSelectMobility(name) }
                    )
                }
            }
        }
    }
}

@Composable
fun MobilityOptionCard(
    modifier: Modifier,
    name: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) EvacuBlueLight else SurfaceWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) EvacuBlue else Color(0xFFDDE5EC)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun SetupOfflineMapStep(
        selectedCompanions: Set<String>,
        onToggleCompanion: (String) -> Unit
    ) {
        val companions = listOf(
            "Solo",
            "Adultos",
            "Niños",
            "Adulto mayor",
            "Mascotas"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Información offline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Descargaremos zonas seguras, puntos de encuentro y rutas base para San Bernardo.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = SafeGreenLight,
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "San Bernardo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = "182 MB · Disponible sin conexión",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "✓ Zonas seguras\n✓ Puntos de encuentro\n✓ Rutas base\n✓ Información de riesgo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Text(
                text = "¿Viajas con alguien?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Selecciona todas las opciones que correspondan.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                companions.forEach { companion ->
                    CompanionChip(
                        text = companion,
                        selected = companion in selectedCompanions,
                        onClick = { onToggleCompanion(companion) }
                    )
                }
            }
        }
    }

    @Composable
    fun CompanionChip(
        text: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingIcon = if (selected) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else {
                null
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = EvacuBlue,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White,
                labelColor = TextPrimary
            )
        )
    }

@Composable
fun MapDashboardScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EvacuBlueLight)
    ) {
        Text(
            text = "Mapa de evacuación",
            modifier = Modifier.align(Alignment.Center),
            color = TextSecondary,
            style = MaterialTheme.typography.titleLarge
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            color = SafeGreenLight,
            shape = RoundedCornerShape(30.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Información actualizada",
                    color = SafeGreen,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            color = EvacuBlueLight,
            shape = RoundedCornerShape(30.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = EvacuBlue,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "GPS activo",
                    color = EvacuBlue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "¿Necesitas evacuar?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Encuentra una ruta segura según tu ubicación y las condiciones disponibles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafeGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "ENCONTRAR RUTA SEGURA",
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Última actualización: hace 15 min",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EvacuAppFlowPreview() {
    ProyectoevacuappTheme {
        EvacuAppFlow()
    }
}