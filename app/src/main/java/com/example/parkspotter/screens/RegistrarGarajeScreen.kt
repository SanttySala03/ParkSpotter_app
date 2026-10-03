package com.example.parkspotter.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.LocationHelper
import com.example.parkspotter.viewmodel.GarajeViewModel
import kotlinx.coroutines.launch

/**
 * Formulario para que un propietario publique (garajeId = null) o edite un garaje.
 * onGarajeGuardado se dispara solo después de que el backend confirma
 * la operación (respuesta 2xx de POST / PUT /garajes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarGarajeScreen(
    garajeId: String? = null,
    onGarajeGuardado: () -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    val viewModel: GarajeViewModel = viewModel(factory = GarajeViewModel.factory(context))
    val isLoading by viewModel.loading.collectAsState()
    val backendError by viewModel.error.collectAsState()
    val garajeEditando by viewModel.garajeEditando.collectAsState()
    val esEdicion = garajeId != null

    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var espacios by remember { mutableStateOf("1") }
    var fotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var ubicacion by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var buscandoUbicacion by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val errorMessage = localError ?: backendError

    // Modo edición: carga el garaje y rellena el formulario.
    LaunchedEffect(garajeId) { garajeId?.let { viewModel.cargarGaraje(it) } }
    LaunchedEffect(garajeEditando) {
        garajeEditando?.let { g ->
            nombre = g.nombre
            direccion = g.direccion
            descripcion = g.descripcion
            precio = g.precioPorHora.toString()
            espacios = g.espaciosTotales.toString()
            fotos = g.fotos.map { Uri.parse(it) }
            if (g.lat != null && g.lng != null) ubicacion = g.lat to g.lng
        }
    }

    fun usarUbicacionActual() {
        scope.launch {
            buscandoUbicacion = true
            val actual = locationHelper.obtenerUbicacionActual()
            buscandoUbicacion = false
            if (actual != null) {
                ubicacion = actual
                localError = null
            } else {
                localError = "No pudimos obtener tu ubicación. Activa el GPS o escribe la dirección completa."
            }
        }
    }

    val permisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) usarUbicacionActual()
        else localError = "Sin permiso de ubicación: escribe la dirección completa y la ubicaremos."
    }

    fun guardar(base: Garaje) {
        scope.launch {
            var coords = ubicacion
            if (coords == null) {
                // Sin GPS: se intenta ubicar la dirección escrita (Geocoder de Android).
                buscandoUbicacion = true
                coords = locationHelper.geocodificar(direccion)
                buscandoUbicacion = false
                if (coords == null) {
                    localError = "No encontramos esa dirección en el mapa. Usa \"Usar mi ubicación actual\"."
                    return@launch
                }
                ubicacion = coords
            }
            viewModel.guardarGaraje(base.copy(lat = coords.first, lng = coords.second)) { exito ->
                if (exito) onGarajeGuardado()
            }
        }
    }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> fotos = (fotos + uris).take(6) }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (esEdicion) "Editar garaje" else "Publicar garaje",
                        color = SurfaceWhite, fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = SurfaceWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Fotos del garaje",
                color = TextDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Agrega hasta 6 fotos. La primera será la principal.",
                color = TextMedium,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fotos) { uri ->
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { fotos = fotos - uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(TextDark.copy(alpha = 0.6f), RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = SurfaceWhite, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                            .background(SurfaceWhite)
                            .clickable { pickImagesLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Agregar foto", tint = BluePrimary)
                            Text("Agregar", color = BluePrimary, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; localError = null; viewModel.limpiarError() },
                label = { Text("Nombre del garaje") },
                placeholder = { Text("Ej: Garaje Chapinero") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = garajeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it; ubicacion = null; localError = null; viewModel.limpiarError() },
                label = { Text("Dirección") },
                placeholder = { Text("Calle, barrio, Bogotá") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = garajeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Ubicación en el mapa (necesaria para que los conductores lo encuentren — Sprint 3)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        if (locationHelper.tienePermiso()) usarUbicacionActual()
                        else permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    enabled = !buscandoUbicacion,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Usar mi ubicación actual", fontSize = 13.sp)
                }
                if (buscandoUbicacion) {
                    Spacer(modifier = Modifier.width(10.dp))
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = ubicacion?.let { "📍 Ubicación: %.5f, %.5f".format(it.first, it.second) }
                    ?: "Si no usas tu ubicación, la buscaremos a partir de la dirección.",
                color = if (ubicacion != null) GreenAccentDark else TextMedium,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it; localError = null; viewModel.limpiarError() },
                label = { Text("Descripción") },
                placeholder = { Text("Ej: Garaje techado, fácil acceso, cámara de seguridad") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = garajeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it.filter { c -> c.isDigit() } },
                    label = { Text("Precio / hora") },
                    placeholder = { Text("3000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = garajeFieldColors(),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = espacios,
                    onValueChange = { espacios = it.filter { c -> c.isDigit() } },
                    label = { Text("Espacios") },
                    placeholder = { Text("1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = garajeFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }

            errorMessage?.let {
                Text(text = it, color = RedError, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val precioInt = precio.toIntOrNull()
                    val espaciosInt = espacios.toIntOrNull()
                    localError = when {
                        nombre.isBlank() -> "Ingresa el nombre del garaje"
                        direccion.isBlank() -> "Ingresa la dirección"
                        precioInt == null || precioInt <= 0 -> "Ingresa un precio válido"
                        espaciosInt == null || espaciosInt <= 0 -> "Ingresa un número de espacios válido"
                        else -> {
                            val anterior = garajeEditando
                            guardar(
                                Garaje(
                                    id = garajeId ?: "",
                                    nombre = nombre,
                                    direccion = direccion,
                                    descripcion = descripcion,
                                    precioPorHora = precioInt,
                                    espaciosTotales = espaciosInt,
                                    espaciosDisponibles = anterior?.espaciosDisponibles ?: espaciosInt,
                                    // Nota: son URIs locales del celular, aún no se suben a
                                    // ningún storage (Firebase/S3) — pendiente.
                                    fotos = fotos.map { it.toString() },
                                    activo = anterior?.activo ?: true
                                )
                            )
                            null
                        }
                    }
                },
                enabled = !isLoading && !buscandoUbicacion,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccentDark, contentColor = SurfaceWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = SurfaceWhite,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        if (esEdicion) "Guardar cambios" else "Publicar garaje",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancelar,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Cancelar", color = TextMedium)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun garajeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BluePrimary,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = BluePrimary,
    unfocusedLabelColor = TextMedium,
    cursorColor = BluePrimary,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite
)
