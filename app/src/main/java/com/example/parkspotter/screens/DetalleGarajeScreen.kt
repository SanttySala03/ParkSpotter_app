package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.EstadoPlaza
import com.example.parkspotter.model.GarajeDetalle
import com.example.parkspotter.model.Plaza
import com.example.parkspotter.ui.components.BotonPrimario
import com.example.parkspotter.ui.components.EstadoVacio
import com.example.parkspotter.ui.components.PildoraDisponibilidad
import com.example.parkspotter.ui.components.colorDe
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.viewmodel.DetalleGarajeViewModel

/**
 * Sprint 3 — Detalle del garaje: datos, horario, disponibilidad por tipo de vehículo y el
 * mapa de plazas por piso, actualizado en vivo. Con [modoPropietario] cada plaza se puede tocar
 * para cambiar su estado (libre, ocupada, fuera de servicio).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleGarajeScreen(
    garajeId: String,
    modoPropietario: Boolean,
    onVolver: () -> Unit,
    onReservar: (String) -> Unit
) {
    val viewModel: DetalleGarajeViewModel = viewModel(factory = DetalleGarajeViewModel.Factory)
    val estado by viewModel.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var plazaElegida by remember { mutableStateOf<Plaza?>(null) }

    LaunchedEffect(garajeId) { viewModel.cargar(garajeId) }
    LaunchedEffect(estado.mensaje, estado.error, estado.detalle != null) {
        val texto = estado.mensaje ?: estado.error?.takeIf { estado.detalle != null }
        if (texto != null) {
            snackbar.showSnackbar(texto)
            viewModel.limpiarMensajes()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, estado.detalle != null) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_START) viewModel.iniciarEnVivo()
            if (e == Lifecycle.Event.ON_STOP) viewModel.detenerEnVivo()
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); viewModel.detenerEnVivo() }
    }

    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        estado.detalle?.garaje?.nombre ?: "Detalle del garaje",
                        color = SurfaceWhite, fontWeight = FontWeight.SemiBold, maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = SurfaceWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = {
            val g = estado.detalle?.garaje
            if (g != null && !modoPropietario) {
                Surface(color = SurfaceWhite, shadowElevation = 12.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(Formatos.pesos(g.precioPorHora), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            Text("por hora", fontSize = 12.sp, color = TextMedium)
                        }
                        BotonPrimario(
                            texto = if (g.reservable) "Reservar" else if (g.abiertoAhora == false) "Cerrado" else "Sin cupos",
                            onClick = { onReservar(g.id) },
                            enabled = g.reservable,
                            acento = true,
                            modifier = Modifier.width(180.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        val detalle = estado.detalle
        when {
            detalle == null && estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
            detalle == null -> EstadoVacio(
                icono = Icons.Filled.ErrorOutline,
                titulo = "No pudimos cargar el garaje",
                texto = estado.error ?: "Intenta de nuevo",
                accion = "Reintentar",
                onAccion = { viewModel.cargar(garajeId) },
                modifier = Modifier.padding(padding)
            )
            else -> ContenidoDetalle(
                detalle = detalle,
                modoPropietario = modoPropietario,
                plazaEnCambio = estado.plazaEnCambio,
                onPlaza = { if (modoPropietario) plazaElegida = it },
                modifier = Modifier.padding(padding)
            )
        }
    }

    plazaElegida?.let { plaza ->
        AlertDialog(
            onDismissRequest = { plazaElegida = null },
            title = { Text("Plaza ${plaza.codigo}") },
            text = {
                Column {
                    Text("Piso ${plaza.piso} · ${plaza.tipo.etiqueta}. Estado actual: ${plaza.estado.etiqueta.lowercase()}.", color = TextMedium)
                    Spacer(Modifier.height(12.dp))
                    if (plaza.estado == EstadoPlaza.RESERVED) {
                        Text("Las plazas reservadas se liberan cuando termina o se cancela la reserva.", fontSize = 13.sp, color = TextMedium)
                    } else {
                        listOf(EstadoPlaza.AVAILABLE, EstadoPlaza.OCCUPIED, EstadoPlaza.OUT_OF_SERVICE)
                            .filter { it != plaza.estado }
                            .forEach { nuevo ->
                                TextButton(onClick = {
                                    viewModel.cambiarEstadoPlaza(plaza, nuevo)
                                    plazaElegida = null
                                }) {
                                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(colorDe(nuevo)))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Marcar como ${nuevo.etiqueta.lowercase()}", color = TextDark)
                                }
                            }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { plazaElegida = null }) { Text("Cerrar") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenidoDetalle(
    detalle: GarajeDetalle,
    modoPropietario: Boolean,
    plazaEnCambio: String?,
    onPlaza: (Plaza) -> Unit,
    modifier: Modifier = Modifier
) {
    val g = detalle.garaje
    var piso by remember(detalle.pisos) { mutableStateOf(detalle.pisos.firstOrNull()) }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // ── Resumen ──
        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PildoraDisponibilidad(g.disponibilidad, g.abiertoAhora)
                Spacer(Modifier.width(8.dp))
                Text("Actualizado en vivo", fontSize = 12.sp, color = TextMedium)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Dato("Libres", "${g.espaciosDisponibles}", Modifier.weight(1f))
                Dato("Ocupadas", "${g.espaciosOcupados}", Modifier.weight(1f))
                Dato("Reservadas", "${g.espaciosReservados}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            BarraOcupacion(g.espaciosDisponibles, g.espaciosOcupados, g.espaciosReservados, detalle.fueraDeServicio)
        }

        Spacer(Modifier.height(12.dp))

        // ── Información ──
        Tarjeta {
            FilaInfo(Icons.Filled.LocationOn, listOf(g.direccion, g.ciudad).filter { it.isNotBlank() }.joinToString(", "))
            FilaInfo(
                Icons.Filled.AccessTime,
                if (g.abierto24h) "Abierto 24 horas"
                else "${Formatos.hora(g.horaApertura)} – ${Formatos.hora(g.horaCierre)}" +
                    if (g.abiertoAhora == false) " · Cerrado ahora" else " · Abierto ahora"
            )
            if (g.telefono.isNotBlank()) FilaInfo(Icons.Filled.Phone, g.telefono)
            if (g.descripcion.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(g.descripcion, fontSize = 14.sp, color = TextMedium)
            }
        }

        // ── Disponibilidad por tipo de vehículo ──
        if (detalle.porTipo.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Tarjeta {
                Text("Cupos por tipo de vehículo", fontWeight = FontWeight.SemiBold, color = TextDark)
                Spacer(Modifier.height(10.dp))
                detalle.porTipo.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(t.tipo.etiqueta, modifier = Modifier.weight(1f), color = TextDark, fontSize = 14.sp)
                        Text(
                            "${t.disponibles} de ${t.total} " + if (t.disponibles == 1) "libre" else "libres",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = if (t.disponibles > 0) GreenAccentDark else RedError
                        )
                    }
                }
            }
        }

        // ── Mapa de plazas ──
        if (detalle.plazas.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Tarjeta {
                Text(
                    if (modoPropietario) "Plazas · toca una para cambiar su estado" else "Plazas del parqueadero",
                    fontWeight = FontWeight.SemiBold, color = TextDark
                )
                if (detalle.pisos.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        detalle.pisos.forEach { p ->
                            FilterChip(
                                selected = p == piso,
                                onClick = { piso = p },
                                label = { Text(if (p < 0) "Sótano ${-p}" else "Piso $p") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BluePrimary, selectedLabelColor = SurfaceWhite
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    detalle.plazas.filter { piso == null || it.piso == piso }.forEach { plaza ->
                        CeldaPlaza(plaza, cambiando = plaza.id == plazaEnCambio, tocable = modoPropietario) { onPlaza(plaza) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EstadoPlaza.entries.forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(colorDe(e)))
                            Spacer(Modifier.width(5.dp))
                            Text(e.etiqueta, fontSize = 12.sp, color = TextMedium)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CeldaPlaza(plaza: Plaza, cambiando: Boolean, tocable: Boolean, onClick: () -> Unit) {
    val color = colorDe(plaza.estado)
    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.5.dp, color, RoundedCornerShape(10.dp))
            .then(if (tocable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (cambiando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = BluePrimary)
        else Text(plaza.codigo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextDark, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BarraOcupacion(libres: Int, ocupadas: Int, reservadas: Int, fuera: Int) {
    val total = (libres + ocupadas + reservadas + fuera).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(BackgroundGray)) {
        listOf(
            libres to colorDe(EstadoPlaza.AVAILABLE),
            reservadas to colorDe(EstadoPlaza.RESERVED),
            ocupadas to colorDe(EstadoPlaza.OCCUPIED),
            fuera to colorDe(EstadoPlaza.OUT_OF_SERVICE)
        ).filter { it.first > 0 }.forEach { (n, c) ->
            Box(Modifier.weight(n.toFloat() / total).fillMaxHeight().background(c))
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = contenido)
    }
}

@Composable
private fun FilaInfo(icono: ImageVector, texto: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, fontSize = 14.sp, color = TextDark)
    }
}
