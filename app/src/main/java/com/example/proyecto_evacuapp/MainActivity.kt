package com.example.proyecto_evacuapp
// Actualizacion en rutas, ahora muestra ubicacion real al iniciar ruta
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.proyecto_evacuapp.ui.components.EvacuAppDatabase
import com.example.proyecto_evacuapp.ui.components.IncidentSharedState
import com.example.proyecto_evacuapp.ui.screens.ActiveNavigationScreen
import com.example.proyecto_evacuapp.ui.screens.AlertsScreen
import com.example.proyecto_evacuapp.ui.screens.EmergencyActiveScreen
import com.example.proyecto_evacuapp.ui.screens.EmergencyTypeSelectScreen
import com.example.proyecto_evacuapp.ui.screens.InitialSetupScreen
import com.example.proyecto_evacuapp.ui.screens.MapScreen
import com.example.proyecto_evacuapp.ui.screens.OnboardingScreen
import com.example.proyecto_evacuapp.ui.screens.ProfileScreen
import com.example.proyecto_evacuapp.ui.screens.RealStreetRoute // 🟢 Importación agregada
import com.example.proyecto_evacuapp.ui.screens.ReportsScreen
import com.example.proyecto_evacuapp.ui.screens.RoutesScreen
import com.example.proyecto_evacuapp.ui.screens.SplashScreen
import com.example.proyecto_evacuapp.ui.theme.AppBackground
import com.example.proyecto_evacuapp.ui.theme.DangerRed
import com.example.proyecto_evacuapp.ui.theme.EvacuBlue
import com.example.proyecto_evacuapp.ui.theme.EvacuBlueLight
import com.example.proyecto_evacuapp.ui.theme.ProyectoevacuappTheme
import com.example.proyecto_evacuapp.ui.theme.SurfaceWhite
import com.example.proyecto_evacuapp.ui.theme.TextSecondary
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = EvacuAppDatabase.getInstance(applicationContext)
        IncidentSharedState.initialize(database)

        Configuration.getInstance().apply {
            userAgentValue = "EvacuApp-UBO-StudentProject/1.0 (${packageName}; contact: evacuapp@ubo.cl)"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }

        setContent {
            ProyectoevacuappTheme {
                EvacuAppApp()
            }
        }
    }
}

private enum class ScreenFlow {
    SPLASH,
    ONBOARDING,
    INITIAL_SETUP,
    MAIN_TABS,
    EMERGENCY_SELECT,
    EMERGENCY_ACTIVE,
    ACTIVE_NAVIGATION
}

@Composable
fun EvacuAppApp() {
    var currentFlow by remember { mutableStateOf(ScreenFlow.SPLASH) }
    var selectedEmergency by remember { mutableStateOf("Terremoto") }

    // 🟢 ESTADO GLOBAL PARA ALMACENAR LA RUTA SELECCIONADA EN "RUTAS"
    var selectedRoute by remember { mutableStateOf<RealStreetRoute?>(null) }

    // ESTADOS PARA GUARDAR LA ZONA SEGURA Y COORDENADAS SELECCIONADAS
    var selectedDestinationName by remember { mutableStateOf("Zona Segura") }
    var selectedDestinationPoint by remember { mutableStateOf(GeoPoint(-33.5925, -70.7045)) }

    // Perfil de movilidad y acompañantes seleccionado en onboarding/setup
    var userMobilityProfile by remember { mutableStateOf("Vehículo") }
    var userCompanions by remember { mutableStateOf(setOf("Solo")) }

    when (currentFlow) {
        ScreenFlow.SPLASH -> SplashScreen(onContinue = { currentFlow = ScreenFlow.ONBOARDING })
        ScreenFlow.ONBOARDING -> OnboardingScreen(onFinish = { currentFlow = ScreenFlow.INITIAL_SETUP })
        ScreenFlow.INITIAL_SETUP -> InitialSetupScreen(
            onFinishWithProfile = { mobility, companions ->
                userMobilityProfile = mobility
                userCompanions = companions
                currentFlow = ScreenFlow.MAIN_TABS
            }
        )
        ScreenFlow.MAIN_TABS -> MainTabsContainer(
            userMobility = userMobilityProfile,
            onOpenEmergency = { currentFlow = ScreenFlow.EMERGENCY_SELECT },
            onSelectRoute = { route ->
                // 🟢 CAPTURA LA RUTA Y SUS COORDENADAS REALES SELECCIONADAS
                selectedRoute = route
                selectedDestinationName = route.destination
                selectedDestinationPoint = route.endPoint
                currentFlow = ScreenFlow.EMERGENCY_SELECT
            },
            onChangeMobility = { userMobilityProfile = it }
        )
        ScreenFlow.EMERGENCY_SELECT -> EmergencyTypeSelectScreen(
            onEmergencySelected = { type ->
                selectedEmergency = type
                currentFlow = ScreenFlow.EMERGENCY_ACTIVE
            },
            onBack = { currentFlow = ScreenFlow.MAIN_TABS }
        )
        ScreenFlow.EMERGENCY_ACTIVE -> EmergencyActiveScreen(
            emergencyType = selectedEmergency,
            selectedDestinationName = selectedDestinationName,   // 🟢 SE ENVÍA EL DESTINO SELECCIONADO
            selectedDestinationPoint = selectedDestinationPoint, // 🟢 SE ENVIAN LAS COORDENADAS SELECCIONADAS
            onStartNavigation = { destinationName, destinationPoint ->
                if (destinationName.isNotBlank() && destinationName != "Zona Segura") {
                    selectedDestinationName = destinationName
                    selectedDestinationPoint = destinationPoint
                }
                currentFlow = ScreenFlow.ACTIVE_NAVIGATION
            },
            onBack = { currentFlow = ScreenFlow.EMERGENCY_SELECT }
        )
        ScreenFlow.ACTIVE_NAVIGATION -> ActiveNavigationScreen(
            destinationName = selectedDestinationName,
            destinationPoint = selectedDestinationPoint,
            mobilityMode = userMobilityProfile,
            onFinish = { currentFlow = ScreenFlow.MAIN_TABS }
        )
    }
}

data class BottomNavTab(val label: String, val icon: ImageVector)

@Composable
fun MainTabsContainer(
    userMobility: String,
    onOpenEmergency: () -> Unit,
    onSelectRoute: (RealStreetRoute) -> Unit, // 🟢 Recibe el objeto con la ruta seleccionada
    onChangeMobility: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        BottomNavTab("Mapa", Icons.Default.Map),
        BottomNavTab("Rutas", Icons.Default.Route),
        BottomNavTab("Reportes", Icons.Default.ReportProblem),
        BottomNavTab("Alertas", Icons.Default.Notifications),
        BottomNavTab("Perfil", Icons.Default.Person)
    )
    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(text = tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EvacuBlue,
                            selectedTextColor = EvacuBlue,
                            indicatorColor = EvacuBlueLight,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { selectedTabIndex = 2 },
                    containerColor = DangerRed,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.ReportProblem, contentDescription = "Reportar incidente")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> MapScreen(onFindRoute = onOpenEmergency)
                1 -> RoutesScreen(
                    mobilityMode = userMobility,
                    onSelectRoute = onSelectRoute, // 🟢 Conectado con la función que guarda la ruta
                    onChangeMobility = onChangeMobility
                )
                2 -> ReportsScreen()
                3 -> AlertsScreen()
                4 -> ProfileScreen()
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EvacuAppAppPreview() {
    ProyectoevacuappTheme { EvacuAppApp() }
}