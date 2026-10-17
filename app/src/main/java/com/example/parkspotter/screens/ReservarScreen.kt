package com.example.parkspotter.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.ui.components.BotonPrimario
import com.example.parkspotter.ui.components.EstadoVacio
import com.example.parkspotter.ui.components.PildoraDisponibilidad
import com.example.parkspotter.ui.components.camposColores
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.util.ReglasReserva
import com.example.parkspotter.viewmodel.EstadoReservar
import com.example.parkspotter.viewmodel.PasoReserva
import com.example.parkspotter.viewmodel.ReservarViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private val DURACIONES = listOf(1, 2, 3, 4, 6, 8, 12)

/**
 * Sprint 4 — Flujo de reserva en 3 pasos (RNF: máximo 4 desde el mapa):
 * 1. Horario y vehículo  →  2. Confirmar resumen y precio  →  3. Reserva lista (código).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservarScreen(
    garajeId: String,
    onVolver: () -> Unit,
    onVerReservas: () -> Unit,
    onIrAlMapa: () -> Unit
) {
    val viewModel: ReservarViewModel = viewModel(factory = ReservarViewModel.Factory)
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(garajeId) { viewModel.cargar(garajeId) }
    BackHandler(enabled = estado.paso == PasoReserva.CONFIRMAR) { viewModel.volver() }
    BackHandler(enabled = estado.paso == PasoReserva.LISTO) { onVerReservas() }

    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Reservar", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (estado.paso != PasoReserva.LISTO) {
                        IconButton(onClick = { if (estado.paso == PasoReserva.CONFIRMAR) viewModel.volver() else onVolver() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = SurfaceWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            IndicadorPasos(estado.paso)
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = BluePrimary) }
                estado.detalle == null -> EstadoVacio(
                    icono = Icons.Filled.ErrorOutline,
                    titulo = "No pudimos cargar el garaje",
                    texto = estado.error ?: "Intenta de nuevo",
                    accion = "Reintentar",
                    onAccion = { viewModel.cargar(garajeId) }
                )
                else -> AnimatedContent(targetState = estado.paso, label = "paso") { paso ->
                    when (paso) {
                        PasoReserva.HORARIO -> PasoHorario(estado, viewModel)
                        PasoReserva.CONFIRMAR -> PasoConfirmar(estado, viewModel)
                        PasoReserva.LISTO -> estado.reserva?.let {
                            PasoListo(it, viewModel.ahora(), onVerReservas, onIrAlMapa)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicadorPasos(actual: PasoReserva) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceWhite).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PasoReserva.entries.forEachIndexed { i, paso ->
            val hecho = paso.ordinal < actual.ordinal
            val activo = paso == actual
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(if (hecho || activo) BluePrimary else BackgroundGray),
                contentAlignment = Alignment.Center
            ) {
                if (hecho) Icon(Icons.Filled.Check, null, tint = GreenAccent, modifier = Modifier.size(16.dp))
                else Text("${i + 1}", color = if (activo) SurfaceWhite else TextMedium, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                when (paso) { PasoReserva.HORARIO -> "Horario"; PasoReserva.CONFIRMAR -> "Confirmar"; PasoReserva.LISTO -> "Listo" },
                fontSize = 13.sp,
                color = if (activo) TextDark else TextMedium,
                fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal
            )
            if (i < PasoReserva.entries.lastIndex) {
                Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(2.dp).background(if (hecho) BluePrimary else BorderGray))
            }
        }
    }
}

// ─────────────────────────── Paso 1: horario ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PasoHorario(estado: EstadoReservar, vm: ReservarViewModel) {
    val garaje = estado.detalle!!.garaje
    val ahora = vm.ahora()
    var mostrarFecha by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }
    val inicio = estado.inicio(ahora)
    val fin = estado.fin(ahora)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            // Garaje
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(garaje.nombre, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    if (garaje.direccion.isNotBlank()) Text(garaje.direccion, fontSize = 13.sp, color = TextMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PildoraDisponibilidad(garaje.disponibilidad, garaje.abiertoAhora)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${Formatos.pesos(garaje.precioPorHora)}/h · " +
                                (garaje.tarifaPlena?.let { "plena ${Formatos.pesos(it)} desde ${garaje.tarifaPlenaDesdeHoras} h · " } ?: "") +
                                if (garaje.abierto24h) "24 horas" else "${Formatos.hora(garaje.horaApertura)} – ${Formatos.hora(garaje.horaCierre)}",
                            fontSize = 13.sp, color = TextMedium
                        )
                    }
                }
            }

            Seccion("Vehículo")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                estado.tiposDisponibles.forEach { tipo ->
                    val cupos = estado.cupos(tipo)
                    ChipOpcion(
                        texto = tipo.etiqueta + (cupos?.let { " · " + Formatos.libres(it) } ?: ""),
                        seleccionado = estado.tipoVehiculo == tipo,
                        habilitado = cupos == null || cupos > 0
                    ) { vm.elegirTipo(tipo) }
                }
            }
            if (ReglasReserva.placaObligatoria(estado.tipoVehiculo)) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = estado.placa,
                    onValueChange = vm::cambiarPlaca,
                    label = { Text("Placa") },
                    placeholder = { Text(ReglasReserva.ejemploPlaca(estado.tipoVehiculo)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    isError = estado.placa.length == 6 && ReglasReserva.errorPlaca(estado.placa, estado.tipoVehiculo) != null,
                    supportingText = {
                        Text(
                            ReglasReserva.errorPlaca(estado.placa, estado.tipoVehiculo).takeIf { estado.placa.length == 6 }
                                ?: "Placa de ${estado.tipoVehiculo.etiqueta.lowercase()}: ${ReglasReserva.formatoPlaca(estado.tipoVehiculo)}"
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = camposColores(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Seccion("¿Cuándo llegas?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                BotonSelector(
                    icono = Icons.Filled.CalendarMonth,
                    etiqueta = "Fecha",
                    valor = inicio?.let { Formatos.dia(it, ahora) } ?: "Elegir",
                    modifier = Modifier.weight(1f)
                ) { mostrarFecha = true }
                BotonSelector(
                    icono = Icons.Filled.Schedule,
                    etiqueta = "Hora de llegada",
                    valor = inicio?.let { Formatos.hora(it) } ?: "Elegir",
                    modifier = Modifier.weight(1f)
                ) { mostrarHora = true }
            }
            TextButton(onClick = vm::usarAhora) { Text("Lo antes posible", color = BluePrimary) }

            Seccion("¿Por cuánto tiempo?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DURACIONES.forEach { h ->
                    ChipOpcion("$h h", estado.duracionHoras == h) { vm.elegirDuracion(h) }
                }
            }

            estado.error?.let { MensajeError(it) }
            Spacer(Modifier.height(16.dp))
        }

        // Resumen fijo abajo
        Surface(color = SurfaceWhite, shadowElevation = 12.dp) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                if (inicio != null && fin != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Formatos.intervalo(inicio, fin, ahora), fontSize = 13.sp, color = TextMedium)
                            Text(
                                Formatos.pesos(ReglasReserva.costo(garaje, inicio, fin)),
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BluePrimary
                            )
                        }
                        BotonPrimario("Continuar", onClick = vm::continuar, modifier = Modifier.width(160.dp))
                    }
                } else {
                    BotonPrimario("Continuar", onClick = vm::continuar, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (mostrarFecha) {
        // El DatePicker de Material trabaja en milisegundos UTC a medianoche.
        val hoyUtc = ahora.toLocalDate().aMillisUtc()
        val limiteUtc = ahora.toLocalDate().plusDays(7).aMillisUtc()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (estado.fecha ?: ahora.toLocalDate()).aMillisUtc(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in hoyUtc..limiteUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.elegirFecha(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mostrarFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = state, title = { Text("Fecha de la reserva", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }

    if (mostrarHora) {
        val base = estado.horaInicio ?: LocalTime.now()
        val state = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { mostrarHora = false },
            title = { Text("Hora de llegada") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    vm.elegirHora(LocalTime.of(state.hour, state.minute))
                    mostrarHora = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { mostrarHora = false }) { Text("Cancelar") } }
        )
    }
}

// ─────────────────────────── Paso 2: confirmar ───────────────────────────

@Composable
private fun PasoConfirmar(estado: EstadoReservar, vm: ReservarViewModel) {
    val garaje = estado.detalle!!.garaje
    val ahora = vm.ahora()
    val inicio = estado.inicio(ahora) ?: return
    val fin = estado.fin(ahora) ?: return
    val horas = ReglasReserva.horasCobradas(inicio, fin)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Revisa tu reserva", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text("Confirma que todo esté bien antes de reservar.", fontSize = 14.sp, color = TextMedium)
            Spacer(Modifier.height(16.dp))
            Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    FilaResumen("Garaje", garaje.nombre)
                    if (garaje.direccion.isNotBlank()) FilaResumen("Dirección", garaje.direccion)
                    FilaResumen("Vehículo", estado.tipoVehiculo.etiqueta + (estado.placa.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""))
                    FilaResumen("Llegada", "${Formatos.dia(inicio, ahora)}, ${Formatos.hora(inicio)}")
                    FilaResumen("Salida", "${Formatos.dia(fin, ahora)}, ${Formatos.hora(fin)}")
                    FilaResumen("Duración", Formatos.duracion(java.time.Duration.between(inicio, fin)))
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = BackgroundGray)
                    FilaResumen(
                        "Tarifa",
                        if (ReglasReserva.aplicaTarifaPlena(garaje, horas)) "Tarifa plena (desde ${garaje.tarifaPlenaDesdeHoras} h)"
                        else "${Formatos.pesos(garaje.precioPorHora)} × $horas h"
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.weight(1f))
                        Text(
                            Formatos.pesos(ReglasReserva.costo(garaje, inicio, fin)),
                            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BluePrimary
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Nota("El pago se realiza en el garaje (los pagos en la app llegan en el Sprint 5). Puedes cancelar sin costo antes de la hora de llegada.")
            estado.error?.let { MensajeError(it) }
        }
        Surface(color = SurfaceWhite, shadowElevation = 12.dp) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = vm::volver,
                    enabled = !estado.enviando,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Modificar", color = BluePrimary) }
                BotonPrimario(
                    "Confirmar reserva",
                    onClick = vm::confirmar,
                    cargando = estado.enviando,
                    acento = true,
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

// ─────────────────────────── Paso 3: listo ───────────────────────────

@Composable
private fun PasoListo(
    reserva: Reserva,
    ahora: java.time.ZonedDateTime,
    onVerReservas: () -> Unit,
    onIrAlMapa: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(88.dp).clip(CircleShape).background(Green50), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CheckCircle, null, tint = GreenAccentDark, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("¡Reserva confirmada!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text(
            "Tu espacio te espera en ${reserva.garajeNombre}.",
            fontSize = 15.sp, color = TextMedium, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = BluePrimary, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Código de reserva", color = SurfaceWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                Text(reserva.codigo, color = GreenAccent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(Formatos.intervalo(reserva.inicio, reserva.fin, ahora), color = SurfaceWhite, fontSize = 14.sp)
                Text(
                    reserva.tipoVehiculo.etiqueta + (reserva.placa?.let { " · $it" } ?: "") + " · " + Formatos.pesos(reserva.total),
                    color = SurfaceWhite, fontSize = 14.sp
                )
                reserva.plazaCodigo?.let { Text("Plaza $it", color = SurfaceWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(Modifier.height(24.dp))
        BotonPrimario("Ver mis reservas", onClick = onVerReservas, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onIrAlMapa) { Text("Volver al mapa", color = BluePrimary) }
    }
}

// ─────────────────────────── Piezas ───────────────────────────

@Composable
private fun Seccion(titulo: String) {
    Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipOpcion(texto: String, seleccionado: Boolean, habilitado: Boolean = true, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        enabled = habilitado,
        label = { Text(texto, fontSize = 14.sp) },
        shape = RoundedCornerShape(12.dp),
        leadingIcon = if (seleccionado) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceWhite,
            selectedContainerColor = BluePrimary,
            selectedLabelColor = SurfaceWhite,
            selectedLeadingIconColor = GreenAccent
        )
    )
}

@Composable
private fun BotonSelector(icono: ImageVector, etiqueta: String, valor: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = SurfaceWhite)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = BluePrimary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(etiqueta, fontSize = 11.sp, color = TextMedium)
                Text(valor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            }
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(etiqueta, fontSize = 14.sp, color = TextMedium, modifier = Modifier.width(96.dp))
        Text(valor, fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Nota(texto: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Blue50).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.Info, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 13.sp, color = TextDark)
    }
}

@Composable
private fun MensajeError(texto: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp).clip(RoundedCornerShape(12.dp))
            .background(RedError.copy(alpha = 0.08f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = RedError, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = RedError, fontSize = 13.sp)
    }
}

private fun LocalDate.aMillisUtc(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
