package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.viewmodel.GarajeViewModel

/**
 * Listado de garajes del propietario autenticado, conectado al backend real
 * a través de GarajeViewModel (Sprint 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisGarajesScreen(
    onAgregarGaraje: () -> Unit,
    onEditarGaraje: (String) -> Unit,
    onVerMapa: () -> Unit,
    onPerfil: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GarajeViewModel = viewModel(factory = GarajeViewModel.factory(context))
    val garajes by viewModel.garajes.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val errorMessage by viewModel.error.collectAsState()
    var garajeAEliminar by remember { mutableStateOf<Garaje?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Errores de acciones sobre la lista (toggle, cupos, eliminar) se muestran como snackbar.
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

    // Se recarga cada vez que la pantalla vuelve a componerse (p. ej. al
    // regresar de RegistrarGarajeScreen), así la lista siempre queda al día.
    LaunchedEffect(Unit) {
        viewModel.cargarMisGarajes()
    }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Mis garajes", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onVerMapa) {
                        Icon(Icons.Filled.Map, contentDescription = "Ver mapa", tint = SurfaceWhite)
                    }
                    IconButton(onClick = onPerfil) {
                        Icon(Icons.Filled.Person, contentDescription = "Mi perfil", tint = SurfaceWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAgregarGaraje,
                containerColor = GreenAccentDark,
                contentColor = SurfaceWhite,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Publicar garaje")
            }
        }
    ) { padding ->
        when {
            isLoading && garajes.isEmpty() -> {
                Box(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            }
            errorMessage != null && garajes.isEmpty() -> {
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No pudimos cargar tus garajes", color = TextDark, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage ?: "", color = TextMedium, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.cargarMisGarajes() }) { Text("Reintentar") }
                }
            }
            garajes.isEmpty() -> {
                EmptyGarajesState(modifier = Modifier.padding(padding), onAgregarGaraje = onAgregarGaraje)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "${garajes.size} garaje(s) publicado(s)",
                            color = TextMedium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(garajes, key = { it.id }) { garaje ->
                        GarajeCard(
                            garaje = garaje,
                            onToggleActivo = { activo ->
                                viewModel.cambiarDisponibilidad(garaje.id, activo)
                            },
                            onCambiarEspacios = { nuevos ->
                                viewModel.cambiarEspaciosDisponibles(garaje.id, nuevos)
                            },
                            onEditar = { onEditarGaraje(garaje.id) },
                            onEliminar = { garajeAEliminar = garaje }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GarajeCard(
    garaje: Garaje,
    onToggleActivo: (Boolean) -> Unit,
    onCambiarEspacios: (Int) -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(garaje.nombre, color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TextLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(garaje.direccion, color = TextMedium, fontSize = 12.sp)
                }
            }
            EstadoBadge(activo = garaje.activo)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(
                titulo = "$${garaje.precioPorHora}/hora",
                modifier = Modifier.weight(1f)
            )
            // Cupos libres en este momento: el conductor los ve en el mapa en tiempo real.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BackgroundGray),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onCambiarEspacios(garaje.espaciosDisponibles - 1) },
                    enabled = garaje.espaciosDisponibles > 0,
                    modifier = Modifier.size(34.dp)
                ) { Icon(Icons.Filled.Remove, contentDescription = "Un cupo menos", modifier = Modifier.size(16.dp)) }
                Text(
                    "${garaje.espaciosDisponibles}/${garaje.espaciosTotales} disp.",
                    color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { onCambiarEspacios(garaje.espaciosDisponibles + 1) },
                    enabled = garaje.espaciosDisponibles < garaje.espaciosTotales,
                    modifier = Modifier.size(34.dp)
                ) { Icon(Icons.Filled.Add, contentDescription = "Un cupo más", modifier = Modifier.size(16.dp)) }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (garaje.activo) "Visible para conductores" else "Oculto del mapa",
                color = TextMedium,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar garaje", tint = BluePrimary)
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar garaje", tint = RedError)
            }
            Switch(
                checked = garaje.activo,
                onCheckedChange = onToggleActivo,
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
private fun EstadoBadge(activo: Boolean) {
    val bg = if (activo) GreenSuccess.copy(alpha = 0.12f) else TextLight.copy(alpha = 0.15f)
    val fg = if (activo) GreenSuccess else TextMedium
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (activo) "Activo" else "Inactivo",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoChip(titulo: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BackgroundGray)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(titulo, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyGarajesState(modifier: Modifier = Modifier, onAgregarGaraje: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Aún no tienes garajes publicados", color = TextDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Convierte tu garaje en desuso en un ingreso extra.",
            color = TextMedium,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAgregarGaraje,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenAccentDark, contentColor = SurfaceWhite)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Publicar mi primer garaje")
        }
    }
}
