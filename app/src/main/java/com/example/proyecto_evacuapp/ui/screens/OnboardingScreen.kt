package com.example.proyecto_evacuapp.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingStage(
    val badgeText: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTintColor: Color,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val stages = listOf(
        OnboardingStage(
            badgeText = "ETAPA 1 DE 3 • MODO AUTÓNOMO",
            icon = Icons.Default.Map,
            iconBgColor = Color(0xFF0284C7).copy(alpha = 0.2f),
            iconTintColor = Color(0xFF38BDF8),
            title = "Guía hacia Zonas Seguras",
            description = "Calcula rutas precisas calle por calle hacia el refugio más cercano, funcionando 100% sin internet ni red celular."
        ),
        OnboardingStage(
            badgeText = "ETAPA 2 DE 3 • CONSENSO COMUNITARIO",
            icon = Icons.Default.ReportProblem,
            iconBgColor = Color(0xFFDC2626).copy(alpha = 0.2f),
            iconTintColor = Color(0xFFF87171),
            title = "Alertas en Tiempo Real",
            description = "Visualiza y reporta obstáculos (bloqueos, anegamientos) validados de forma descentralizada por la comunidad."
        ),
        OnboardingStage(
            badgeText = "ETAPA 3 DE 3 • COMUNICACIÓN DE EMERGENCIA",
            icon = Icons.Default.CellTower,
            iconBgColor = Color(0xFF7C3AED).copy(alpha = 0.2f),
            iconTintColor = Color(0xFFA78BFA),
            title = "Comunicación sin Cobertura",
            description = "Transmite alertas de auxilio y mensajes de texto mediante Bluetooth y SMS de emergencia cuando las redes colapsan."
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { stages.size })

    // Guarda de forma permanente que el onboarding fue completado
    val completeOnboarding: () -> Unit = {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        onFinish()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Fondo de alta visibilidad para misión crítica
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BARRA SUPERIOR: Saltado directo permanente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EVACUAPP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8),
                    letterSpacing = 2.sp
                )

                Surface(
                    onClick = { completeOnboarding() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    contentColor = Color(0xFF94A3B8)
                ) {
                    Text(
                        text = "Saltar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // CONTENIDO DESLIZABLE HORIZONTALMENTE
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                val stage = stages[pageIndex]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // Badge descriptivo de la etapa actual
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = stage.iconBgColor,
                        border = BorderStroke(1.dp, stage.iconTintColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = stage.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = stage.iconTintColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Contenedor circular con icono representativo
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(stage.iconBgColor)
                            .border(2.dp, stage.iconTintColor.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = stage.icon,
                            contentDescription = stage.title,
                            tint = stage.iconTintColor,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Título de la etapa
                    Text(
                        text = stage.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descripción detallada de la característica
                    Text(
                        text = stage.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // INDICADORES DE PÁGINA (DOTS ANIMADOS)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(stages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (isSelected) 32.dp else 10.dp,
                                height = 10.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÓN INFERIOR PROMINENTE DE ACCIÓN
            val isLastPage = pagerState.currentPage == stages.lastIndex

            Button(
                onClick = {
                    if (isLastPage) {
                        completeOnboarding()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLastPage) Color(0xFF00E676) else Color(0xFF0284C7),
                    contentColor = if (isLastPage) Color.Black else Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isLastPage) "COMENZAR" else "SIGUIENTE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isLastPage) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
