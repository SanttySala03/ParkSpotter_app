package com.example.parkspotter.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkspotter.ui.components.LogoParkSpotter
import com.example.parkspotter.ui.components.MarcaParkSpotter
import com.example.parkspotter.ui.theme.BluePrimary
import com.example.parkspotter.ui.theme.IconosBarraEstado
import com.example.parkspotter.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay

/**
 * Splash inicial: versión de la marca para fondo oscuro (P blanca, vehículo azul y acento verde)
 * con animación de escala + fade y transición automática.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing))
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 500))
        delay(700)
        onFinished()
    }

    IconosBarraEstado(fondoOscuro = true)

    Box(
        modifier = Modifier.fillMaxSize().background(BluePrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale.value)) {
            LogoParkSpotter(
                modifier = Modifier.size(132.dp),
                colorP = SurfaceWhite,
                colorDetalle = BluePrimary
            )
            Spacer(Modifier.height(20.dp))
            MarcaParkSpotter(
                modifier = Modifier.alpha(alpha.value),
                tamano = 34.sp,
                sobreOscuro = true,
                eslogan = true
            )
        }
    }
}
