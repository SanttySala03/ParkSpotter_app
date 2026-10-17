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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.parkspotter.ui.components.BotonPrimario
import com.example.parkspotter.ui.components.camposColores
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.util.LocationHelper
import com.example.parkspotter.util.ValidacionGaraje
import com.example.parkspotter.viewmodel.GarajeViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Formulario para que un propietario publique (garajeId = null) o edite un garaje.
 * Valida con las mismas reglas del backend (GarageRequest) antes de enviar; onGarajeGuardado
 * se dispara solo cuando el backend confirma (POST / PUT /garages).
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
    val viewModel: GarajeViewModel = viewModel(factory = GarajeViewModel.Factory)
    val isLoading by viewModel.loading.collectAsState()
    val backendError by viewModel.error.collectAsState()
    val garajeEditando by viewModel.garajeEditando.collectAsState()
    val esEdicion = garajeId != null

    var nombre by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var ciudad by rememberSaveable { mutableStateOf("Bogotá") }
    var departamento by rememberSaveable { mutableStateOf("Bogotá D.C.") }
    var pais by rememberSaveable { mutableStateOf("Colombia") }
    var codigoPostal by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var precio by rememberSaveable { mutableStateOf("") }
    var espacios by rememberSaveable { mutableStateOf("1") }
    var abierto24h by rememberSaveable { mutableStateOf(true) }
    var conTarifaPlena by rememberSaveable { mutableStateOf(false) }
    var tarifaPlena by rememberSaveable { mutableStateOf("") }
    var tarifaPlenaHoras by rememberSaveable { mutableStateOf("4") }
    var apertura by remember { mutableStateOf(LocalTime.of(6, 0)) }
    var cierre by remember { mutableStateOf(LocalTime.of(22, 0)) }
    var editandoHora by remember { mutableStateOf<Boolean?>(null) } // true = apertura, false = cierre
    var fotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var ubicacion by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var buscandoUbicacion by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val errorMessage = localError ?: backendError

    fun limpiar() { localError = null; viewModel.limpiarError() }

    // Modo edición: carga el garaje y rellena el formulario.
    LaunchedEffect(garajeId) { garajeId?.let { viewModel.cargarGaraje(it) } }
    LaunchedEffect(garajeEditando) {
        garajeEditando?.let { g ->
            nombre = g.nombre
            direccion = g.direccion
            ciudad = g.ciudad
            departamento = g.departamento
            pais = g.pais
            codigoPostal = g.codigoPostal
            telefono = g.telefono
            descripcion = g.descripcion
            precio = g.precioPorHora.toString()
            espacios = g.espaciosTotales.toString()
            abierto24h = g.abierto24h
            conTarifaPlena = g.tarifaPlena != null
            g.tarifaPlena?.let { tarifaPlena = it.toString() }
            g.tarifaPlenaDesdeHoras?.let { tarifaPlenaHoras = it.toString() }
            g.horaApertura?.let { apertura = it }
            g.horaCierre?.let { cierre = it }
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
                coords = locationHelper.geocodificar("$direccion, $ciudad, $pais")
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

    IconosBarraEstado(fondoOscuro = true)

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SurfaceWhite)
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
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Titulo("Fotos del garaje")
            Text(
                text = "Vista previa: el servidor aún no almacena fotos (pendiente de backend).",
                color = TextMedium,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(fotos) { uri ->
                    Box(modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp))) {
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
                            Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = SurfaceWhite, modifier = Modifier.size(14.dp))
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
            Titulo("Datos del garaje")
            Campo(nombre, { nombre = it; limpiar() }, "Nombre del garaje", "Ej: Garaje Chapinero")
            Campo(descripcion, { descripcion = it.take(1000); limpiar() }, "Descripción (opcional)", "Techado, fácil acceso, cámaras", lineas = 3)
            Campo(telefono, { telefono = it.take(20); limpiar() }, "Teléfono de contacto (opcional)", "+57 300 123 4567", teclado = KeyboardType.Phone)

            Spacer(modifier = Modifier.height(8.dp))
            Titulo("Ubicación")
            Campo(direccion, { direccion = it; ubicacion = null; limpiar() }, "Dirección", "Cra 7 # 60-15")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo(ciudad, { ciudad = it; ubicacion = null; limpiar() }, "Ciudad", "Bogotá", Modifier.weight(1f))
                Campo(departamento, { departamento = it; limpiar() }, "Departamento", "Bogotá D.C.", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo(pais, { pais = it; limpiar() }, "País", "Colombia", Modifier.weight(1f))
                Campo(codigoPostal, { codigoPostal = it.take(10); limpiar() }, "Cód. postal (opc.)", "110231", Modifier.weight(1f))
            }

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

            Spacer(modifier = Modifier.height(20.dp))
            Titulo("Capacidad y tarifa")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo(precio, { precio = it.filter(Char::isDigit).take(7); limpiar() }, "Precio / hora (COP)", "3000", Modifier.weight(1f), KeyboardType.Number)
                Campo(espacios, { espacios = it.filter(Char::isDigit).take(4); limpiar() }, "Plazas", "1", Modifier.weight(1f), KeyboardType.Number)
            }
            if (esEdicion) {
                Text(
                    "Al reducir las plazas solo se eliminan las libres o fuera de servicio.",
                    fontSize = 12.sp, color = TextMedium
                )
            }

            // Tarifa plena: como en muchos parqueaderos de Bogotá, desde cierta hora se cobra un valor fijo.
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite)
                    .clickable { conTarifaPlena = !conTarifaPlena; limpiar() }.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Tarifa plena", color = TextDark)
                    Text("Desde cierta hora se cobra un valor fijo", fontSize = 12.sp, color = TextMedium)
                }
                Switch(
                    checked = conTarifaPlena,
                    onCheckedChange = { conTarifaPlena = it; limpiar() },
                    colors = SwitchDefaults.colors(checkedTrackColor = GreenAccentDark, checkedThumbColor = SurfaceWhite)
                )
            }
            if (conTarifaPlena) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Campo(tarifaPlenaHoras, { tarifaPlenaHoras = it.filter(Char::isDigit).take(2); limpiar() }, "Desde (horas)", "4", Modifier.weight(1f), KeyboardType.Number)
                    Campo(tarifaPlena, { tarifaPlena = it.filter(Char::isDigit).take(7); limpiar() }, "Valor plena (COP)", "15000", Modifier.weight(1f), KeyboardType.Number)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Titulo("Horario")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite)
                    .clickable { abierto24h = !abierto24h; limpiar() }.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Abierto las 24 horas", color = TextDark, modifier = Modifier.weight(1f))
                Switch(
                    checked = abierto24h,
                    onCheckedChange = { abierto24h = it; limpiar() },
                    colors = SwitchDefaults.colors(checkedTrackColor = GreenAccentDark, checkedThumbColor = SurfaceWhite)
                )
            }
            if (!abierto24h) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectorHora("Abre", apertura, Modifier.weight(1f)) { editandoHora = true }
                    SelectorHora("Cierra", cierre, Modifier.weight(1f)) { editandoHora = false }
                }
            }

            errorMessage?.let {
                Text(text = it, color = RedError, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            BotonPrimario(
                texto = if (esEdicion) "Guardar cambios" else "Publicar garaje",
                onClick = {
                    val borrador = Garaje(
                        id = garajeId ?: "",
                        nombre = nombre,
                        direccion = direccion,
                        ciudad = ciudad,
                        departamento = departamento,
                        pais = pais,
                        codigoPostal = codigoPostal,
                        telefono = telefono,
                        descripcion = descripcion,
                        precioPorHora = precio.toIntOrNull() ?: -1,
                        espaciosTotales = espacios.toIntOrNull() ?: 0,
                        abierto24h = abierto24h,
                        horaApertura = apertura,
                        horaCierre = cierre,
                        tarifaPlena = if (conTarifaPlena) tarifaPlena.toIntOrNull() ?: -1 else null,
                        tarifaPlenaDesdeHoras = if (conTarifaPlena) tarifaPlenaHoras.toIntOrNull() ?: 0 else null,
                        fotos = fotos.map { it.toString() }
                    )
                    localError = ValidacionGaraje.validar(borrador)
                    if (localError == null) guardar(borrador)
                },
                cargando = isLoading || buscandoUbicacion,
                acento = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onCancelar,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Cancelar", color = TextMedium)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    editandoHora?.let { esApertura ->
        val base = if (esApertura) apertura else cierre
        val state = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { editandoHora = null },
            title = { Text(if (esApertura) "Hora de apertura" else "Hora de cierre") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val h = LocalTime.of(state.hour, state.minute)
                    if (esApertura) apertura = h else cierre = h
                    editandoHora = null
                    limpiar()
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { editandoHora = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    ejemplo: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    teclado: KeyboardType = KeyboardType.Text,
    lineas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = { Text(ejemplo) },
        singleLine = lineas == 1,
        minLines = lineas,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(12.dp),
        colors = camposColores(),
        modifier = modifier.padding(bottom = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorHora(etiqueta: String, hora: LocalTime, modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = SurfaceWhite)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(etiqueta, fontSize = 11.sp, color = TextMedium)
                Text(Formatos.hora(hora), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            }
        }
    }
}
