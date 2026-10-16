package com.example.parkspotter.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ParkSpotterColors = lightColorScheme(
    primary = BluePrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = Blue50,
    onPrimaryContainer = BluePrimary,
    // Verde oscuro para que el texto blanco encima cumpla contraste AA; el verde menta del logo es "tertiary".
    secondary = GreenAccentDark,
    onSecondary = SurfaceWhite,
    secondaryContainer = Green50,
    onSecondaryContainer = GreenAccentDark,
    tertiary = GreenAccent,
    onTertiary = BluePrimary,
    background = BackgroundGray,
    surface = SurfaceWhite,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = TextMedium,
    onBackground = TextDark,
    onSurface = TextDark,
    outline = BorderGray,
    outlineVariant = Blue50,
    error = RedError,
)

@Composable
fun ParkSpotterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParkSpotterColors,
        typography = Typography,
        content = content
    )
}

/**
 * La app dibuja de borde a borde (Android 15+): cada pantalla indica si su parte superior es
 * oscura (barra azul -> íconos claros) o clara (mapa, login -> íconos oscuros).
 */
@Composable
fun IconosBarraEstado(fondoOscuro: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !fondoOscuro
    }
}
