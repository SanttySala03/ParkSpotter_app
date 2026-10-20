package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.Notificacion
import com.example.parkspotter.ui.components.EstadoVacio
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.viewmodel.NotificacionesViewModel
import java.time.ZonedDateTime

/** Sprint 5 — Bandeja de avisos: pagos, reembolsos y recordatorios. Tocar un aviso abre sus reservas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(onVolver: () -> Unit, onAbrirReserva: () -> Unit) {
    val viewModel: NotificacionesViewModel = viewModel(factory = NotificacionesViewModel.Factory)
    val estado by viewModel.estado.collectAsState()
    val ahora = viewModel.ahora()

    LaunchedEffect(Unit) { viewModel.cargar() }
    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Avisos", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = SurfaceWhite) }
                },
                actions = {
                    if (estado.lista.any { !it.leida }) {
                        TextButton(onClick = viewModel::marcarTodas) { Text("Marcar todos como leídos", color = SurfaceWhite) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { padding ->
        PullToRefreshBox(isRefreshing = estado.cargando, onRefresh = viewModel::cargar, modifier = Modifier.fillMaxSize().padding(padding)) {
            if (estado.lista.isEmpty() && !estado.cargando) {
                EstadoVacio(
                    icono = Icons.Filled.NotificationsNone,
                    titulo = "No tienes avisos",
                    texto = estado.error ?: "Aquí verás los pagos, reembolsos y recordatorios de tus reservas."
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(estado.lista, key = { it.id }) { n ->
                        TarjetaAviso(n, ahora) {
                            viewModel.marcarLeida(n)
                            if (n.reservaId != null) onAbrirReserva()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAviso(n: Notificacion, ahora: ZonedDateTime, onClick: () -> Unit) {
    val (icono, color) = iconoDe(n.tipo)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (n.leida) SurfaceWhite else Blue50,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icono, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.titulo, fontWeight = if (n.leida) FontWeight.Medium else FontWeight.Bold, color = TextDark,
                        fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (!n.leida) Box(Modifier.size(8.dp).clip(CircleShape).background(GreenAccent))
                }
                Text(n.mensaje, fontSize = 13.sp, color = TextMedium)
                n.creadaEn?.let { Text("${Formatos.dia(it, ahora)}, ${Formatos.hora(it)}", fontSize = 11.sp, color = TextLight,
                    modifier = Modifier.padding(top = 4.dp)) }
            }
        }
    }
}

private fun iconoDe(tipo: String): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (tipo) {
    "PAYMENT_APPROVED" -> Icons.Filled.CheckCircle to GreenAccentDark
    "PAYMENT_DECLINED" -> Icons.Filled.ErrorOutline to RedError
    "PAYMENT_REFUNDED" -> Icons.Filled.Replay to BluePrimary
    else -> Icons.Filled.AccessTime to YellowWarning
}
