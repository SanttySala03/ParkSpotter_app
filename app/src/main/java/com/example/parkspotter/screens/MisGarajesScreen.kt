package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.ui.components.BarraInferior
import com.example.parkspotter.ui.components.EstadoVacio
import com.example.parkspotter.ui.components.Pestana
import com.example.parkspotter.ui.components.PildoraDisponibilidad
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.viewmodel.GarajeViewModel

/**
 * Sprint 2 — Garajes del propietario autenticado (GET /garages/mine): publicar/ocultar,
 * ajustar cupos en tiempo real, gestionar plazas, editar y eliminar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisGarajesScreen(
    onAgregarGaraje: () -> Unit,
    onEditarGaraje: (String) -> Unit,
    onGestionarPlazas: (String) -> Unit,
    onPestana: (Pestana) -> Unit
) {
    val viewModel: GarajeViewModel = viewModel(factory = GarajeViewModel.Factory)
    val garajes by viewModel.garajes.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    val ocupados by viewModel.ocupados.collectAsState()
    var garajeAEliminar by remember { mutableStateOf<Garaje?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Errores de acciones sobre la lista (publicar, cupos, eliminar) se muestran como snackbar.
    LaunchedEffect(errorMessage) {
        if (errorMessage != null && garajes.isNotEmpty()) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.limpiarError()
        }
    }

    garajeAEliminar?.let { g ->
        AlertDialog(
            onDismissRequest = { garajeAEliminar = null },
            title = { Text("Eliminar garaje") },
            text = { Text("¿Seguro que quieres eliminar \"${g.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarGaraje(g.id)
                    garajeAEliminar = null
                }) { Text("Eliminar", color = RedError) }
            },
            dismissButton = {
                TextButton(onClick = { garajeAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    // Se recarga al volver a la pantalla (p. ej. desde RegistrarGarajeScreen).
    LaunchedEffect(Unit) { viewModel.cargarMisGarajes() }

    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Mis garajes", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = { BarraInferior(Pestana.GARAJES, esPropietario = true, onSeleccion = onPestana) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAgregarGaraje,
                containerColor = GreenAccentDark,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Publicar garaje") }
            )
        }
    ) { padding ->
        when {
            isLoading && garajes.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
            errorMessage != null && garajes.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.CloudOff,
                titulo = "No pudimos cargar tus garajes",
                texto = errorMessage.orEmpty(),
                accion = "Reintentar",
                onAccion = viewModel::cargarMisGarajes,
                modifier = Modifier.padding(padding)
            )
            garajes.isEmpty() -> EstadoVacio(
                icono = Icons.Filled.Warehouse,
                titulo = "Aún no tienes garajes publicados",
                texto = "Convierte tu garaje en desuso en un ingreso extra.",
                accion = "Publicar mi primer garaje",
                onAccion = onAgregarGaraje,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    val libres = garajes.sumOf { it.espaciosDisponibles }
                    val total = garajes.sumOf { it.espaciosTotales }
                    Text(
                        text = "${garajes.size} garaje(s) · $libres de $total plazas libres ahora",
                        color = TextMedium,
                        fontSize = 13.sp
                    )
                }
                items(garajes, key = { it.id }) { garaje ->
                    GarajeCard(
                        garaje = garaje,
                        ocupado = garaje.id in ocupados,
                        onToggleActivo = { activo -> viewModel.cambiarDisponibilidad(garaje.id, activo) },
                        onCupo = { liberar -> viewModel.ajustarCupo(garaje.id, liberar) },
                        onPlazas = { onGestionarPlazas(garaje.id) },
                        onEditar = { onEditarGaraje(garaje.id) },
                        onEliminar = { garajeAEliminar = garaje }
                    )
                }
            }
        }
    }
}

@Composable
private fun GarajeCard(
    garaje: Garaje,
    ocupado: Boolean,
    onToggleActivo: (Boolean) -> Unit,
    onCupo: (liberar: Boolean) -> Unit,
    onPlazas: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(garaje.nombre, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TextLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(garaje.direccion, color = TextMedium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (garaje.activo) PildoraDisponibilidad(garaje.disponibilidad, null)
            else EstadoBadgeInactivo()
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(
                titulo = "${Formatos.pesos(garaje.precioPorHora)}/h",
                modifier = Modifier.weight(0.8f)
            )
            // Cupos libres ahora: ocupar / liberar una plaza. Los conductores lo ven al instante.
            Row(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundGray),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onCupo(false) },
                    enabled = !ocupado && garaje.espaciosDisponibles > 0,
                    modifier = Modifier.size(38.dp)
                ) { Icon(Icons.Filled.Remove, contentDescription = "Ocupar una plaza", modifier = Modifier.size(18.dp)) }
                if (ocupado) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = BluePrimary)
                else Text(
                    "${garaje.espaciosDisponibles}/${garaje.espaciosTotales} libres",
                    color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { onCupo(true) },
                    enabled = !ocupado && garaje.espaciosOcupados > 0,
                    modifier = Modifier.size(38.dp)
                ) { Icon(Icons.Filled.Add, contentDescription = "Liberar una plaza", modifier = Modifier.size(18.dp)) }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (garaje.activo) "Visible para conductores" else "Oculto del mapa",
                color = TextMedium,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onPlazas) {
                Icon(Icons.Filled.GridView, contentDescription = "Gestionar plazas", tint = BluePrimary)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar garaje", tint = BluePrimary)
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar garaje", tint = RedError)
            }
            Switch(
                checked = garaje.activo,
                onCheckedChange = onToggleActivo,
                enabled = !ocupado,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceWhite,
                    checkedTrackColor = GreenAccentDark,
                    uncheckedTrackColor = BorderGray
                )
            )
        }
    }
}

@Composable
private fun EstadoBadgeInactivo() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TextLight.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = "Inactivo", color = TextMedium, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoChip(titulo: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundGray)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(titulo, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
