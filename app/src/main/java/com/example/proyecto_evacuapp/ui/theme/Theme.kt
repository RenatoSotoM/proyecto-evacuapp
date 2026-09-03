package com.example.proyecto_evacuapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EvacuBlueLight,
    onPrimary = EvacuBlueDark,
    primaryContainer = EvacuBlueDark,
    onPrimaryContainer = EvacuBlueLight,
    secondary = SafeGreen,
    onSecondary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EvacuBlue,
    onPrimary = Color.White,
    primaryContainer = EvacuBlueLight,
    onPrimaryContainer = EvacuBlueDark,
    secondary = SafeGreen,
    onSecondary = Color.White,
    background = AppBackground,
    surface = SurfaceWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ProyectoevacuappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false para asegurar los colores de EvacuApp
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}