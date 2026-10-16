package com.example.parkspotter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.parkspotter.ui.theme.BluePrimary
import com.example.parkspotter.ui.theme.GreenAccent
import com.example.parkspotter.ui.theme.SurfaceWhite
import com.example.parkspotter.ui.theme.TextMedium

/**
 * Símbolo de la marca: "P" estilizada que es a la vez una plaza de parqueo, con un vehículo
 * visto desde arriba en su interior y el semicírculo verde de disponibilidad.
 * P + vehículo + plaza de parqueo = ParkSpotter.
 *
 * Se dibuja en una grilla de 120 x 120 unidades (la misma del ícono de la app).
 * - Sobre fondo claro: P azul, detalles blancos.
 * - Sobre fondo oscuro: [colorP] = blanco y [colorDetalle] = azul.
 */
@Composable
fun LogoParkSpotter(
    modifier: Modifier = Modifier,
    colorP: Color = BluePrimary,
    colorDetalle: Color = SurfaceWhite,
    colorAcento: Color = GreenAccent
) {
    Canvas(modifier = modifier.semantics { contentDescription = "Logo de ParkSpotter" }) {
        val u = size.minDimension / 120f
        val dx = (size.width - 120f * u) / 2f
        val dy = (size.height - 120f * u) / 2f

        fun DrawScope.caja(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float, borde: Float? = null) {
            drawRoundRect(
                color = color,
                topLeft = Offset(dx + x * u, dy + y * u),
                size = Size(w * u, h * u),
                cornerRadius = CornerRadius(r * u),
                style = borde?.let { Stroke(width = it * u) } ?: androidx.compose.ui.graphics.drawscope.Fill
            )
        }

        // P: panza + palo
        caja(colorP, 14f, 8f, 66f, 70f, 16f)
        caja(colorP, 14f, 40f, 24f, 72f, 12f)
        // Plaza de parqueo (contorno) dentro de la panza
        caja(colorDetalle, 30f, 18f, 40f, 50f, 10f, borde = 4.5f)
        // Vehículo visto desde arriba
        caja(colorDetalle, 39f, 24f, 22f, 38f, 8f)
        caja(colorP, 42f, 31f, 16f, 8f, 3f)   // parabrisas
        caja(colorP, 42f, 50f, 16f, 6f, 2.5f) // vidrio trasero
        caja(colorDetalle, 35.5f, 34f, 4f, 3f, 1.5f) // espejos
        caja(colorDetalle, 60.5f, 34f, 4f, 3f, 1.5f)
        // Semicírculo verde: disponibilidad
        drawArc(
            color = colorAcento,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(dx + (86f - 28f) * u, dy + 15f * u),
            size = Size(56f * u, 56f * u)
        )
    }
}

/** Wordmark: "Park" en azul y "Spotter" en verde (sobre fondo oscuro "Park" va en blanco). */
@Composable
fun MarcaParkSpotter(
    modifier: Modifier = Modifier,
    tamano: TextUnit = 28.sp,
    sobreOscuro: Boolean = false,
    eslogan: Boolean = false
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Park",
                color = if (sobreOscuro) SurfaceWhite else BluePrimary,
                fontSize = tamano,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                "Spotter",
                color = GreenAccent,
                fontSize = tamano,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
        }
        if (eslogan) {
            Text(
                "Encuentra tu espacio",
                color = if (sobreOscuro) SurfaceWhite.copy(alpha = 0.85f) else TextMedium,
                fontSize = (tamano.value * 0.48f).sp,
                letterSpacing = 2.sp
            )
        }
    }
}
