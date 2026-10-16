package com.example.parkspotter.ui.theme

import androidx.compose.ui.graphics.Color

// ── Sistema de diseño ParkSpotter (Manual de Identidad Visual 2026) ──────────
// Azul oscuro #0B2D5B = confianza, seguridad, tecnología (color principal)
// Verde #00C896       = disponibilidad, movilidad, crecimiento (acento)
// Gris claro #F4F7FA  = simplicidad, claridad, equilibrio (fondos)
val BluePrimary    = Color(0xFF0B2D5B)  // Azul marino primario
val BlueLight      = Color(0xFF1E4A85)  // Azul hover / variante intermedia
val Blue50         = Color(0xFFE7EDF5)  // Azul muy claro (fondos de íconos, chips)
val SurfaceWhite   = Color(0xFFFFFFFF)  // Blanco puro
val BackgroundGray = Color(0xFFF4F7FA)  // Gris muy claro (igual al del manual de marca)
val TextDark       = Color(0xFF0F172A)  // Texto principal
val TextMedium     = Color(0xFF475569)  // Texto secundario
val TextLight      = Color(0xFF94A3B8)  // Texto terciario / placeholders
val BorderGray     = Color(0xFFCBD5E1)  // Bordes
val RedError       = Color(0xFFDC2626)  // Errores y "lleno" (contraste AA sobre blanco)
val YellowWarning  = Color(0xFFF59E0B)  // "Pocos cupos" y reservadas
val GrayClosed     = Color(0xFF94A3B8)  // Cerrado / fuera de servicio

// Acento de marca (logo, disponibilidad, botones de acción)
val GreenAccent     = Color(0xFF00C896)  // Verde menta del logo (marcadores, íconos, fondos)
val GreenAccentDark = Color(0xFF00875F)  // Verde para texto y botones con texto blanco (WCAG AA 4.5:1)
val GreenSuccess    = GreenAccentDark    // Alias usado por pantallas anteriores
val Green50         = Color(0xFFE6F9F3)  // Fondo de chips "disponible"
