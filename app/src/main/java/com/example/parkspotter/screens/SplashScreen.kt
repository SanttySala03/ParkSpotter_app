package com.example.parkspotter.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkspotter.ui.theme.BluePrimary
import com.example.parkspotter.ui.theme.GreenAccent
import com.example.parkspotter.ui.theme.SurfaceWhite
import kotlinx.coroutines.delay

/**
 * Splash inicial: logo animado (escala + fade) y transición automática a Login
 * tras un breve delay. Sin dependencias de imágenes externas: el ícono se
 * construye con Compose usando el mismo lenguaje visual del logo
 * (P + auto + plaza de parqueo).
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        )
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 500))
        delay(900)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BluePrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            // Icono estilo logo: contenedor redondeado azul con auto blanco + acento verde
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceWhite.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = "ParkSpotter",
                        tint = BluePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 14.dp)
                        .size(width = 22.dp, height = 56.dp)
                        .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                        .background(GreenAccent)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.alpha(alpha.value)) {
                Text(
                    text = "Park",
                    color = SurfaceWhite,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Spotter",
                    color = GreenAccent,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Encuentra tu espacio",
                color = SurfaceWhite.copy(alpha = 0.85f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
