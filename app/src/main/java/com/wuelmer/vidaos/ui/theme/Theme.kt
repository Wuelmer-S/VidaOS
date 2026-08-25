package com.wuelmer.vidaos.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Superficie,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo600,
    secondary = AzulAcento,
    onSecondary = Superficie,
    background = Fondo,
    onBackground = TextoFuerte,
    surface = Superficie,
    onSurface = TextoFuerte,
    surfaceVariant = Fondo,
    onSurfaceVariant = TextoSuave,
    outline = Borde,
    error = ColorGasto,
    onError = Superficie
)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = TextoFuerte,
    primaryContainer = Indigo600,
    onPrimaryContainer = Indigo100,
    secondary = AzulAcento,
    onSecondary = TextoFuerte,
    background = Color(0xFF12142B),
    onBackground = Color(0xFFE4E5FE),
    surface = Color(0xFF1B1F3B),
    onSurface = Color(0xFFE4E5FE),
    surfaceVariant = Color(0xFF262A4A),
    onSurfaceVariant = TextoSuave,
    outline = Color(0xFF3A3F63),
    error = ColorGasto,
    onError = Superficie
)

@Composable
fun VidaOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
