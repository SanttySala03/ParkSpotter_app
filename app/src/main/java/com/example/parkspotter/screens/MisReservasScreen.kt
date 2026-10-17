package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.ui.components.BarraInferior
import com.example.parkspotter.ui.components.EstadoVacio
import com.example.parkspotter.ui.components.Pestana
import com.example.parkspotter.ui.components.PildoraReserva
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.viewmodel.MisReservasViewModel
import java.time.ZonedDateTime

/** Sprint 4 — Historial de reservas: pestañas "Próximas" e "Historial", con cancelación confirmada. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisReservasScreen(
    esPropietario: Boolean,
    onPestana: (Pestana) -> Unit,
    onVerGaraje: (String) -> Unit
) {
    val viewModel: MisReservasViewModel = viewModel(factory = MisReservasViewModel.Factory)
    val estado by viewModel.estado.collectAsState()
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var aCancelar by remember { mutableStateOf<Reserva?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val ahora = viewModel.ahora()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(estado.mensaje, estado.error) {
        (estado.mensaje ?: estado.error)?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Mis reservas", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = viewModel::cargar) { Icon(Icons.Filled.Refresh, "Actualizar", tint = SurfaceWhite) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
                )
                TabRow(
                    selectedTabIndex = pestana,
                    containerColor = BluePrimary,
                    contentColor = SurfaceWhite,
                    indicator = { pos ->
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[pestana]), color = GreenAccent, height = 3.dp)
                    }
                ) {
                    Tab(pestana == 0, { pestana = 0 }, text = { Text("Próximas (${estado.activas.size})") })
                    Tab(pestana == 1, { pestana = 1 }, text = { Text("Historial") })
                }
            }
        },
        bottomBar = { BarraInferior(Pestana.RESERVAS, esPropietario, onPestana) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = estado.cargando,
            onRefresh = viewModel::cargar,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val lista = if (pestana == 0) estado.activas else estado.historial
            if (lista.isEmpty() && !estado.cargando) {
                EstadoVacio(
                    icono = Icons.Filled.EventBusy,
                    titulo = if (pestana == 0) "No tienes reservas próximas" else "Aún no hay historial",
                    texto = if (pestana == 0) "Busca un garaje en el mapa y asegura tu espacio antes de salir."
                            else "Aquí verás tus reservas completadas y canceladas.",
                    accion = if (pestana == 0) "Buscar parqueadero" else null,
                    onAccion = { onPestana(Pestana.MAPA) }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lista, key = { it.id }) { r ->
                        TarjetaReserva(
                            reserva = r,
                            ahora = ahora,
                            cancelando = estado.cancelando == r.id,
                            onCancelar = { aCancelar = r },
                            onVerGaraje = { onVerGaraje(r.garajeId) }
                        )
                    }
                }
            }
        }
    }

    aCancelar?.let { r ->
        AlertDialog(
            onDismissRequest = { aCancelar = null },
            title = { Text("¿Cancelar la reserva?") },
            text = {
                Text("Se liberará tu espacio en ${r.garajeNombre} para ${Formatos.intervalo(r.inicio, r.fin, ahora).lowercase()}. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelar(r); aCancelar = null }) {
                    Text("Sí, cancelar", color = RedError, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { aCancelar = null }) { Text("Mantener reserva") } }
        )
    }
}

@Composable
private fun TarjetaReserva(
    reserva: Reserva,
    ahora: ZonedDateTime,
    cancelando: Boolean,
    onCancelar: () -> Unit,
    onVerGaraje: () -> Unit
) {
    val enCurso = reserva.enCurso(ahora)
    Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(reserva.garajeNombre, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (reserva.garajeDireccion.isNotBlank()) {
                        Text(reserva.garajeDireccion, fontSize = 12.sp, color = TextMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                PildoraReserva(reserva.estadoVisible(ahora), enCurso = enCurso)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BackgroundGray).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(Formatos.intervalo(reserva.inicio, reserva.fin, ahora), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                    Text(
                        reserva.tipoVehiculo.etiqueta + (reserva.placa?.let { " · $it" } ?: "") +
                            (reserva.plazaCodigo?.let { " · Plaza $it" } ?: ""),
                        fontSize = 12.sp, color = TextMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Formatos.pesos(reserva.total), fontWeight = FontWeight.Bold, color = BluePrimary)
                    Text(reserva.codigo, fontSize = 12.sp, color = TextMedium, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onVerGaraje) { Text("Ver garaje", color = BluePrimary) }
                if (reserva.cancelable(ahora)) {
                    if (cancelando) CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp)
                    else TextButton(onClick = onCancelar) { Text("Cancelar", color = RedError) }
                }
            }
        }
    }
}
