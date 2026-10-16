package com.example.parkspotter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkspotter.model.Disponibilidad
import com.example.parkspotter.model.EstadoPlaza
import com.example.parkspotter.model.EstadoReserva
import com.example.parkspotter.ui.theme.Blue50
import com.example.parkspotter.ui.theme.BluePrimary
import com.example.parkspotter.ui.theme.BorderGray
import com.example.parkspotter.ui.theme.GrayClosed
import com.example.parkspotter.ui.theme.Green50
import com.example.parkspotter.ui.theme.GreenAccent
import com.example.parkspotter.ui.theme.GreenAccentDark
import com.example.parkspotter.ui.theme.RedError
import com.example.parkspotter.ui.theme.SurfaceWhite
import com.example.parkspotter.ui.theme.TextDark
import com.example.parkspotter.ui.theme.TextLight
import com.example.parkspotter.ui.theme.TextMedium
import com.example.parkspotter.ui.theme.YellowWarning

// ── Colores semánticos de estado (mismos en mapa, lista, detalle y reservas) ──

/** Color del marcador / punto de estado. */
fun colorDe(d: Disponibilidad): Color = when (d) {
    Disponibilidad.AVAILABLE -> GreenAccent
    Disponibilidad.FEW_SPOTS -> YellowWarning
    Disponibilidad.FULL -> RedError
    Disponibilidad.CLOSED -> GrayClosed
}

/** Color para texto sobre fondo claro (contraste AA). */
fun colorTextoDe(d: Disponibilidad): Color = when (d) {
    Disponibilidad.AVAILABLE -> GreenAccentDark
    Disponibilidad.FEW_SPOTS -> Color(0xFFB45309)
    Disponibilidad.FULL -> RedError
    Disponibilidad.CLOSED -> TextMedium
}

fun colorDe(e: EstadoPlaza): Color = when (e) {
    EstadoPlaza.AVAILABLE -> GreenAccent
    EstadoPlaza.OCCUPIED -> RedError
    EstadoPlaza.RESERVED -> YellowWarning
    EstadoPlaza.OUT_OF_SERVICE -> GrayClosed
}

fun colorDe(e: EstadoReserva): Pair<Color, Color> = when (e) {
    EstadoReserva.CONFIRMED -> Green50 to GreenAccentDark
    EstadoReserva.PENDING -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    EstadoReserva.CANCELLED -> Color(0xFFFEE2E2) to RedError
    EstadoReserva.COMPLETED -> Blue50 to BluePrimary
}

/** Píldora de estado: "Disponible", "Pocos cupos", "Lleno", "Cerrado"... */
@Composable
fun Pildora(texto: String, fondo: Color, color: Color, modifier: Modifier = Modifier, punto: Color? = null) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (punto != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(punto))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PildoraDisponibilidad(d: Disponibilidad, abiertoAhora: Boolean?, modifier: Modifier = Modifier) {
    val efectiva = if (abiertoAhora == false && d != Disponibilidad.CLOSED) Disponibilidad.CLOSED else d
    val texto = if (efectiva == Disponibilidad.CLOSED && abiertoAhora == false) "Cerrado ahora" else efectiva.etiqueta
    Pildora(
        texto = texto,
        fondo = colorDe(efectiva).copy(alpha = 0.14f),
        color = colorTextoDe(efectiva),
        punto = colorDe(efectiva),
        modifier = modifier
    )
}

@Composable
fun PildoraReserva(e: EstadoReserva, enCurso: Boolean = false, modifier: Modifier = Modifier) {
    val (fondo, color) = colorDe(e)
    Pildora(if (enCurso) "En curso" else e.etiqueta, fondo, color, modifier)
}

// ── Formularios ──

@Composable
fun camposColores(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BluePrimary,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = BluePrimary,
    unfocusedLabelColor = TextMedium,
    cursorColor = BluePrimary,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    errorContainerColor = SurfaceWhite
)

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
    acento: Boolean = false,
    icono: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !cargando,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (acento) GreenAccentDark else BluePrimary,
            contentColor = SurfaceWhite,
            disabledContainerColor = BorderGray,
            disabledContentColor = SurfaceWhite
        ),
        modifier = modifier.height(52.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(color = SurfaceWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            if (icono != null) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Estados vacíos / error (Definition of Done: loading, error, empty) ──

@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(Blue50),
            contentAlignment = Alignment.Center
        ) { Icon(icono, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(titulo, color = TextDark, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(texto, color = TextMedium, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (accion != null) {
            Spacer(Modifier.height(20.dp))
            BotonPrimario(accion, onAccion, acento = true)
        }
    }
}

// ── Navegación principal ──

enum class Pestana(val titulo: String, val icono: ImageVector, val iconoInactivo: ImageVector) {
    MAPA("Explorar", Icons.Filled.Map, Icons.Outlined.Map),
    RESERVAS("Reservas", Icons.Filled.EventNote, Icons.Outlined.EventNote),
    GARAJES("Mis garajes", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
    PERFIL("Perfil", Icons.Filled.Person, Icons.Outlined.Person)
}

/** Barra inferior: Explorar · Reservas · (Mis garajes si es propietario) · Perfil. */
@Composable
fun BarraInferior(actual: Pestana, esPropietario: Boolean, onSeleccion: (Pestana) -> Unit) {
    NavigationBar(containerColor = SurfaceWhite, tonalElevation = 0.dp) {
        Pestana.entries
            .filter { it != Pestana.GARAJES || esPropietario }
            .forEach { p ->
                val sel = p == actual
                NavigationBarItem(
                    selected = sel,
                    onClick = { if (!sel) onSeleccion(p) },
                    icon = { Icon(if (sel) p.icono else p.iconoInactivo, contentDescription = null) },
                    label = { Text(p.titulo, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BluePrimary,
                        selectedTextColor = BluePrimary,
                        indicatorColor = GreenAccent.copy(alpha = 0.22f),
                        unselectedIconColor = TextLight,
                        unselectedTextColor = TextMedium
                    )
                )
            }
    }
}
