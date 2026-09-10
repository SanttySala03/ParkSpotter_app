package com.example.parkspotter.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ParkSpotterColors = lightColorScheme(
    primary        = BluePrimary,
    onPrimary      = SurfaceWhite,
    primaryContainer = Blue50,
    secondary      = GreenAccent,
    onSecondary    = SurfaceWhite,
    secondaryContainer = GreenAccent.copy(alpha = 0.15f),
    onSecondaryContainer = GreenAccentDark,
    background     = BackgroundGray,
    surface        = SurfaceWhite,
    onBackground   = TextDark,
    onSurface      = TextDark,
    outline        = BorderGray,
    error          = RedError,
)

@Composable
fun ParkSpotterTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BluePrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = ParkSpotterColors,
        typography  = Typography,
        content     = content
    )
}