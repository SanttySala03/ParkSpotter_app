package com.example.parkspotter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.navigation.NavGraph
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.LocationHelper
import com.example.parkspotter.viewmodel.FiltrosBusqueda
import com.example.parkspotter.viewmodel.GarajeViewModel
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

// ── Estado visual derivado del garaje real (Sprint 3: ya no hay datos de ejemplo) ──
enum class ParkingStatus { AVAILABLE, FULL }

private fun Garaje.status(): ParkingStatus =
    if (!activo || espaciosDisponibles <= 0) ParkingStatus.FULL else ParkingStatus.AVAILABLE

private val BOGOTA_DEFAULT = LatLng(4.6782, -74.0582)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContent {
            ParkSpotterTheme {
                NavGraph()
            }
        }
    }
}

@Composable
fun ParkSpotterApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    val viewModel: GarajeViewModel = viewModel(factory = GarajeViewModel.factory(context))

    val garajes by viewModel.garajes.collectAsState()
    val filtros by viewModel.filtros.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedParking by remember { mutableStateOf<Garaje?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun centrarYBuscarEnUbicacion() {
        scope.launch {
            val ubicacion = locationHelper.obtenerUbicacionActual()
            if (ubicacion != null) {
                userLocation = ubicacion
                mapView?.getMapAsync { map ->
                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(ubicacion.first, ubicacion.second))
                                .zoom(14.0)
                                .build()
                        )
                    )
                }
                viewModel.buscarGarajes(ubicacion.first, ubicacion.second)
            } else {
                viewModel.buscarGarajes()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) centrarYBuscarEnUbicacion()
    }

    // ── Carga inicial + actualización en tiempo real (Sprint 3) ──
    LaunchedEffect(Unit) {
        if (hasLocationPermission) centrarYBuscarEnUbicacion() else viewModel.buscarGarajes()
        viewModel.iniciarActualizacionEnVivo()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.detenerActualizacionEnVivo() }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Mapa ──────────────────────────────────────────────────────────────
        ParkSpotterMap(
            parkings = garajes,
            userLocation = userLocation,
            onMapReady = { mapView = it },
            onParkingSelected = { parking ->
                selectedParking = parking
                showBottomSheet = true
            }
        )

        // ── Navbar superior: búsqueda + filtros ──────────────────────────────
        TopNavBar(
            texto = filtros.texto,
            onTextoChange = { viewModel.actualizarFiltros(filtros.copy(texto = it)) },
            onBuscar = { viewModel.buscarGarajes(mostrarCargando = false) },
            onFiltrosClick = { showFilterSheet = true }
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            )
        }

        error?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp, start = 16.dp, end = 16.dp),
                color = RedError.copy(alpha = 0.95f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(msg, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }

        // ── Botón de ubicación ────────────────────────────────────────────────
        FloatingLocationButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (showBottomSheet) 320.dp else 24.dp),
            onClick = {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    centrarYBuscarEnUbicacion()
                }
            }
        )

        // ── Bottom Sheet de parqueadero ───────────────────────────────────────
        AnimatedVisibility(
            visible = showBottomSheet && selectedParking != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            selectedParking?.let { parking ->
                ParkingBottomSheet(
                    parking = parking,
                    onDismiss = { showBottomSheet = false },
                    onReserve = { /* TODO: implementar reserva (sprint futuro) */ }
                )
            }
        }

        if (showFilterSheet) {
            FiltrosBottomSheet(
                filtros = filtros,
                onAplicar = { nuevos ->
                    viewModel.actualizarFiltros(nuevos)
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
fun ParkSpotterMap(
    parkings: List<Garaje>,
    userLocation: Pair<Double, Double>?,
    onMapReady: (MapView) -> Unit = {},
    onParkingSelected: (Garaje) -> Unit
) {
    var mapRef by remember { mutableStateOf<MapView?>(null) }

    // Repinta las fuentes GeoJSON cada vez que cambian los garajes (refresco en vivo, Sprint 3).
    LaunchedEffect(parkings, userLocation) {
        val map = mapRef ?: return@LaunchedEffect
        map.getMapAsync { m ->
            m.style?.let { style ->
                (style.getSource("parkings-source") as? org.maplibre.android.style.sources.GeoJsonSource)
                    ?.setGeoJson(buildGarajesGeoJson(parkings))
                (style.getSource("user-location-source") as? org.maplibre.android.style.sources.GeoJsonSource)
                    ?.setGeoJson(buildUserLocationGeoJson(userLocation))
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).also { mv ->
                mapRef = mv
                onMapReady(mv)
                mv.getMapAsync { map ->
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->

                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(BOGOTA_DEFAULT)
                                    .zoom(12.0)
                                    .build()
                            )
                        )

                        style.addSource(
                            org.maplibre.android.style.sources.GeoJsonSource(
                                "parkings-source", buildGarajesGeoJson(parkings)
                            )
                        )
                        style.addLayer(
                            org.maplibre.android.style.layers.CircleLayer("parkings-layer", "parkings-source").apply {
                                setProperties(
                                    org.maplibre.android.style.layers.PropertyFactory.circleRadius(14f),
                                    org.maplibre.android.style.layers.PropertyFactory.circleColor(
                                        org.maplibre.android.style.expressions.Expression.get("color")
                                    ),
                                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(3f),
                                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor("#FFFFFF")
                                )
                            }
                        )

                        style.addSource(
                            org.maplibre.android.style.sources.GeoJsonSource(
                                "user-location-source", buildUserLocationGeoJson(userLocation)
                            )
                        )
                        style.addLayer(
                            org.maplibre.android.style.layers.CircleLayer("user-location-layer", "user-location-source").apply {
                                setProperties(
                                    org.maplibre.android.style.layers.PropertyFactory.circleRadius(8f),
                                    org.maplibre.android.style.layers.PropertyFactory.circleColor("#2563EB"),
                                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth(3f),
                                    org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor("#FFFFFF")
                                )
                            }
                        )

                        map.addOnMapClickListener { point ->
                            val pixel = map.projection.toScreenLocation(point)
                            val features = map.queryRenderedFeatures(pixel, "parkings-layer")
                            if (features.isNotEmpty()) {
                                val id = features[0].getStringProperty("id")
                                val parking = parkings.firstOrNull { it.id == id }
                                if (parking != null) {
                                    onParkingSelected(parking)
                                    return@addOnMapClickListener true
                                }
                            }
                            false
                        }
                    }
                }
            }
        }
    )
}

private fun buildGarajesGeoJson(parkings: List<Garaje>): String {
    val features = parkings.joinToString(",") { parking ->
        val color = when (parking.status()) {
            ParkingStatus.AVAILABLE -> "#059669"
            ParkingStatus.FULL -> "#EF4444"
        }
        """
        {
            "type": "Feature",
            "properties": { "id": "${parking.id}", "color": "$color", "name": "${parking.nombre}" },
            "geometry": { "type": "Point", "coordinates": [${parking.lng}, ${parking.lat}] }
        }
        """
    }
    return """{ "type": "FeatureCollection", "features": [$features] }"""
}

private fun buildUserLocationGeoJson(userLocation: Pair<Double, Double>?): String {
    if (userLocation == null) return """{ "type": "FeatureCollection", "features": [] }"""
    return """
    {
        "type": "FeatureCollection",
        "features": [{
            "type": "Feature",
            "properties": {},
            "geometry": { "type": "Point", "coordinates": [${userLocation.second}, ${userLocation.first}] }
        }]
    }
    """
}

@Composable
fun TopNavBar(
    texto: String,
    onTextoChange: (String) -> Unit,
    onBuscar: () -> Unit,
    onFiltrosClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(top = 32.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceWhite
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextLight)
                TextField(
                    value = texto,
                    onValueChange = onTextoChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar por nombre o dirección...", fontSize = 14.sp, color = TextLight) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    )
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Blue50)
                        .clickable { onFiltrosClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtros", tint = BluePrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosBottomSheet(
    filtros: FiltrosBusqueda,
    onAplicar: (FiltrosBusqueda) -> Unit,
    onDismiss: () -> Unit
) {
    var radioKm by remember { mutableStateOf(filtros.radioKm ?: 5.0) }
    var precioMaxTexto by remember { mutableStateOf(filtros.precioMax?.toString() ?: "") }
    var soloDisponibles by remember { mutableStateOf(filtros.soloDisponibles) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Filtros de búsqueda", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(20.dp))

            Text("Radio de búsqueda: ${radioKm.toInt()} km", fontSize = 14.sp, color = TextMedium)
            Slider(value = radioKm.toFloat(), onValueChange = { radioKm = it.toDouble() }, valueRange = 1f..20f)

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = precioMaxTexto,
                onValueChange = { precioMaxTexto = it.filter { c -> c.isDigit() } },
                label = { Text("Precio máximo por hora ($)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Solo con espacios disponibles", fontSize = 14.sp, color = TextDark)
                Switch(checked = soloDisponibles, onCheckedChange = { soloDisponibles = it })
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onAplicar(
                        filtros.copy(
                            radioKm = radioKm,
                            precioMax = precioMaxTexto.toIntOrNull(),
                            soloDisponibles = soloDisponibles
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("Aplicar filtros")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FloatingLocationButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .size(52.dp)
            .shadow(6.dp, CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = SurfaceWhite
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("◎", fontSize = 22.sp, color = BluePrimary)
        }
    }
}

@Composable
fun ParkingBottomSheet(
    parking: Garaje,
    onDismiss: () -> Unit,
    onReserve: () -> Unit
) {
    val status = parking.status()
    val statusColor = if (status == ParkingStatus.AVAILABLE) GreenSuccess else RedError
    val statusText = if (status == ParkingStatus.AVAILABLE) "Disponible" else "Sin espacios"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = SurfaceWhite,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderGray)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(parking.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(parking.direccion, fontSize = 13.sp, color = TextMedium)
                    parking.distanciaKm?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("A $it km de ti", fontSize = 12.sp, color = BluePrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.1f)) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoCard(modifier = Modifier.weight(1f), icon = "💰", label = "Precio", value = "$${parking.precioPorHora}/hora")
                InfoCard(
                    modifier = Modifier.weight(1f), icon = "🅿", label = "Espacios",
                    value = "${parking.espaciosDisponibles}/${parking.espaciosTotales}"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMedium)
                ) { Text("Cerrar", fontSize = 14.sp) }
                Button(
                    onClick = onReserve,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == ParkingStatus.FULL) BorderGray else BluePrimary,
                        contentColor = SurfaceWhite
                    ),
                    enabled = status != ParkingStatus.FULL
                ) {
                    Text(
                        text = if (status == ParkingStatus.FULL) "Sin espacios" else "Reservar",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = BackgroundGray) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = TextLight)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        }
    }
}
