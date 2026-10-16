package com.example.parkspotter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía de marca: sans serif geométrica (manual: Montserrat SemiBold para títulos y
 * Regular para subtítulos). Mientras no se agreguen los archivos de Montserrat en res/font
 * se usa la sans serif del sistema con los mismos pesos; para cambiarla basta con
 * reemplazar [MarcaFont] por FontFamily(Font(R.font.montserrat_regular), ...).
 */
val MarcaFont: FontFamily = FontFamily.SansSerif

private fun estilo(tamano: Int, peso: FontWeight, alto: Int, espaciado: Double = 0.0) = TextStyle(
    fontFamily = MarcaFont,
    fontWeight = peso,
    fontSize = tamano.sp,
    lineHeight = alto.sp,
    letterSpacing = espaciado.sp
)

val Typography = Typography(
    displaySmall = estilo(32, FontWeight.Bold, 40, -0.5),
    headlineMedium = estilo(26, FontWeight.Bold, 32, -0.3),
    headlineSmall = estilo(22, FontWeight.SemiBold, 28),
    titleLarge = estilo(20, FontWeight.SemiBold, 26),
    titleMedium = estilo(16, FontWeight.SemiBold, 22, 0.1),
    titleSmall = estilo(14, FontWeight.SemiBold, 20, 0.1),
    bodyLarge = estilo(16, FontWeight.Normal, 24, 0.3),
    bodyMedium = estilo(14, FontWeight.Normal, 20, 0.2),
    bodySmall = estilo(12, FontWeight.Normal, 16, 0.3),
    labelLarge = estilo(14, FontWeight.SemiBold, 20, 0.1),
    labelMedium = estilo(12, FontWeight.Medium, 16, 0.4),
    labelSmall = estilo(11, FontWeight.Medium, 16, 0.5)
)
