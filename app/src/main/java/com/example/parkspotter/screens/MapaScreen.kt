package com.example.parkspotter.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.AreaMapa
import com.example.parkspotter.model.Disponibilidad
import com.example.parkspotter.model.FiltrosBusqueda
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.model.OrdenBusqueda
import com.example.parkspotter.model.TipoVehiculo
import com.example.parkspotter.ui.components.BarraInferior
import com.example.parkspotter.ui.components.BotonPrimario
import com.example.parkspotter.ui.components.EstadoVacio
import com.example.parkspotter.ui.components.Pestana
import com.example.parkspotter.ui.components.PildoraDisponibilidad
import com.example.parkspotter.ui.components.camposColores
import com.example.parkspotter.ui.components.colorDe
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.util.LocationHelper
import com.example.parkspotter.viewmodel.BusquedaViewModel
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private val BOGOTA = LatLng(4.6482, -74.0836)
private const val ESTILO_MAPA = "https://tiles.openfreemap.org/styles/liberty"
private const val FUENTE_GARAJES = "garajes-fuente"
private const val CAPA_GARAJES = "garajes-capa"
private const val CAPA_CUPOS = "garajes-cupos"
private const val FUENTE_USUARIO = "usuario-fuente"

/**
 * Sprint 3 — Pantalla principal del conductor: mapa interactivo con marcadores por
 * disponibilidad (verde / ámbar / rojo / gris), búsqueda, filtros, lista y actualización en vivo.
 */
@Composable
fun MapaScreen(
    esPropietario: Boolean,
    onPestana: (Pestana) -> Unit,
    onVerDetalle: (String) -> Unit,
    onReservar: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val locationHelper = remember { LocationHelper(context) }
    val viewModel: BusquedaViewModel = viewModel(factory = BusquedaViewModel.Factory)
    val estado by viewModel.estado.collectAsState()

    var seleccionadoId by rememberSaveable { mutableStateOf<String?>(null) }
    var modoLista by rememberSaveable { mutableStateOf(false) }
    var mostrarFiltros by remember { mutableStateOf(false) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }

    // El garaje seleccionado se lee de la lista viva: si cambian sus cupos, la tarjeta se actualiza sola.
    val seleccionado = seleccionadoId?.let { id ->
        estado.marcadores.firstOrNull { it.id == id } ?: estado.resultados.firstOrNull { it.id == id }
    }

    fun moverA(lat: Double, lng: Double, zoom: Double = 14.5) {
        mapa?.animateCamera(
            CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(LatLng(lat, lng)).zoom(zoom).build()),
            700
        )
    }

    fun ubicarme() {
        scope.launch {
            val u = locationHelper.obtenerUbicacionActual()
            if (u != null) {
                viewModel.actualizarUbicacion(u.first, u.second)
                moverA(u.first, u.second)
            }
        }
    }

    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) ubicarme()
    }

    LaunchedEffect(Unit) {
        if (locationHelper.tienePermiso()) ubicarme()
        else permiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) { viewModel.moverCamara.collect { (lat, lng) -> moverA(lat, lng, 15.0) } }

    // Stream en vivo solo mientras la pantalla está visible.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> viewModel.iniciarEnVivo()
                Lifecycle.Event.ON_STOP -> viewModel.detenerEnVivo()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            viewModel.detenerEnVivo()
        }
    }

    IconosBarraEstado(fondoOscuro = false)

    Scaffold(
        bottomBar = { BarraInferior(Pestana.MAPA, esPropietario, onPestana) },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            MapaGarajes(
                garajes = estado.marcadores,
                seleccionadoId = seleccionadoId,
                ubicacionUsuario = estado.ubicacionUsuario,
                onMapaListo = { mapa = it },
                onAreaVisible = viewModel::onAreaVisible,
                onSeleccion = { g ->
                    seleccionadoId = g?.id
                    focus.clearFocus()
                }
            )

            if (modoLista) {
                ListaResultados(
                    resultados = estado.resultados,
                    cargando = estado.cargandoLista,
                    orden = estado.filtros.orden,
                    hayUbicacion = estado.ubicacionUsuario != null,
                    onOrden = { viewModel.aplicarFiltros(estado.filtros.copy(orden = it)) },
                    onClick = { onVerDetalle(it.id) },
                    onLimpiar = viewModel::limpiarFiltros,
                    modifier = Modifier.fillMaxSize().background(BackgroundGray).statusBarsPadding().padding(top = 116.dp)
                )
            }

            // ── Búsqueda + filtros rápidos ──
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                BarraBusqueda(
                    texto = estado.filtros.texto,
                    filtrosActivos = estado.filtros.activos,
                    onTexto = viewModel::actualizarTexto,
                    onBuscar = {
                        focus.clearFocus()
                        viewModel.buscarTexto()
                    },
                    onFiltros = { mostrarFiltros = true }
                )
                Spacer(Modifier.height(10.dp))
                FiltrosRapidos(
                    filtros = estado.filtros,
                    enVivo = estado.enVivo,
                    onCambio = viewModel::aplicarFiltros
                )
                if (estado.cargandoMapa || estado.cargandoLista) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(50)),
                        color = GreenAccent,
                        trackColor = Blue50
                    )
                }
                AnimatedVisibility(estado.truncado && !modoLista) {
                    AvisoFlotante("Hay más garajes en esta zona: acerca el mapa para verlos todos")
                }
                estado.error?.let { msg ->
                    AvisoFlotante(msg, esError = true, onCerrar = viewModel::limpiarError)
                }
            }

            // ── Botones flotantes ──
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = if (seleccionado != null && !modoLista) 236.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (!modoLista) {
                    BotonCircular(Icons.Filled.MyLocation, "Mi ubicación") {
                        if (locationHelper.tienePermiso()) ubicarme()
                        else permiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = { modoLista = !modoLista; seleccionadoId = null },
                    containerColor = BluePrimary,
                    contentColor = SurfaceWhite,
                    shape = RoundedCornerShape(50),
                    icon = { Icon(if (modoLista) Icons.Filled.Map else Icons.AutoMirrored.Filled.List, null) },
                    text = { Text(if (modoLista) "Ver mapa" else "Ver lista (${estado.resultados.size})") }
                )
            }

            // ── Tarjeta del garaje seleccionado ──
            AnimatedVisibility(
                visible = seleccionado != null && !modoLista,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                seleccionado?.let { g ->
                    TarjetaSeleccion(
                        garaje = g,
                        distanciaKm = estado.resultados.firstOrNull { it.id == g.id }?.distanciaKm,
                        onCerrar = { seleccionadoId = null },
                        onDetalle = { onVerDetalle(g.id) },
                        onReservar = { onReservar(g.id) }
                    )
                }
            }
        }
    }

    if (mostrarFiltros) {
        FiltrosBottomSheet(
            filtros = estado.filtros,
            onAplicar = { viewModel.aplicarFiltros(it); mostrarFiltros = false },
            onDismiss = { mostrarFiltros = false }
        )
    }
}

// ─────────────────────────── Mapa (MapLibre) ───────────────────────────

@Composable
private fun MapaGarajes(
    garajes: List<Garaje>,
    seleccionadoId: String?,
    ubicacionUsuario: Pair<Double, Double>?,
    onMapaListo: (MapLibreMap) -> Unit,
    onAreaVisible: (AreaMapa) -> Unit,
    onSeleccion: (Garaje?) -> Unit
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estiloListo by remember { mutableStateOf(false) }
    // Los listeners del mapa se registran una vez: con rememberUpdatedState leen siempre el valor actual.
    val garajesActuales by rememberUpdatedState(garajes)
    val onSeleccionActual by rememberUpdatedState(onSeleccion)
    val onAreaActual by rememberUpdatedState(onAreaVisible)

    LaunchedEffect(garajes, seleccionadoId, estiloListo) {
        if (!estiloListo) return@LaunchedEffect
        mapa?.style?.getSourceAs<GeoJsonSource>(FUENTE_GARAJES)?.setGeoJson(garajesAGeoJson(garajes, seleccionadoId))
    }
    LaunchedEffect(ubicacionUsuario, estiloListo) {
        if (!estiloListo) return@LaunchedEffect
        val fc = ubicacionUsuario?.let { (lat, lng) -> FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(lng, lat))) }
            ?: FeatureCollection.fromFeatures(emptyList())
        mapa?.style?.getSourceAs<GeoJsonSource>(FUENTE_USUARIO)?.setGeoJson(fc)
    }

    // El MapView de MapLibre necesita los eventos del ciclo de vida.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val mv = mapView
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mv?.onStart()
                Lifecycle.Event.ON_RESUME -> mv?.onResume()
                Lifecycle.Event.ON_PAUSE -> mv?.onPause()
                Lifecycle.Event.ON_STOP -> mv?.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).also { mv ->
                mv.onCreate(null)
                mapView = mv
                mv.getMapAsync { m ->
                    mapa = m
                    onMapaListo(m)
                    m.uiSettings.isCompassEnabled = false
                    m.uiSettings.isAttributionEnabled = true
                    m.uiSettings.isLogoEnabled = false
                    m.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(ubicacionUsuario?.let { LatLng(it.first, it.second) } ?: BOGOTA)
                                .zoom(if (ubicacionUsuario != null) 14.5 else 12.0)
                                .build()
                        )
                    )
                    m.setStyle(ESTILO_MAPA) { style ->
                        style.addSource(GeoJsonSource(FUENTE_GARAJES, garajesAGeoJson(garajesActuales, null)))
                        style.addLayer(
                            CircleLayer(CAPA_GARAJES, FUENTE_GARAJES).withProperties(
                                PropertyFactory.circleColor(Expression.get("color")),
                                PropertyFactory.circleRadius(
                                    Expression.switchCase(Expression.get("sel"), Expression.literal(19f), Expression.literal(15f))
                                ),
                                PropertyFactory.circleStrokeWidth(3f),
                                PropertyFactory.circleStrokeColor(
                                    Expression.switchCase(
                                        Expression.get("sel"),
                                        Expression.literal("#0B2D5B"),
                                        Expression.literal("#FFFFFF")
                                    )
                                )
                            )
                        )
                        // Número de cupos libres dentro del marcador
                        style.addLayer(
                            SymbolLayer(CAPA_CUPOS, FUENTE_GARAJES).withProperties(
                                PropertyFactory.textField(Expression.get("cupos")),
                                PropertyFactory.textSize(12f),
                                PropertyFactory.textColor(Expression.get("texto")),
                                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                                PropertyFactory.textAllowOverlap(true),
                                PropertyFactory.textIgnorePlacement(true)
                            )
                        )
                        style.addSource(GeoJsonSource(FUENTE_USUARIO, FeatureCollection.fromFeatures(emptyList())))
                        style.addLayer(
                            CircleLayer("usuario-halo", FUENTE_USUARIO).withProperties(
                                PropertyFactory.circleRadius(18f),
                                PropertyFactory.circleColor("#2563EB"),
                                PropertyFactory.circleOpacity(0.18f)
                            )
                        )
                        style.addLayer(
                            CircleLayer("usuario-punto", FUENTE_USUARIO).withProperties(
                                PropertyFactory.circleRadius(7f),
                                PropertyFactory.circleColor("#2563EB"),
                                PropertyFactory.circleStrokeWidth(3f),
                                PropertyFactory.circleStrokeColor("#FFFFFF")
                            )
                        )

                        m.addOnMapClickListener { punto ->
                            val pixel = m.projection.toScreenLocation(punto)
                            val id = m.queryRenderedFeatures(pixel, CAPA_GARAJES, CAPA_CUPOS)
                                .firstOrNull()?.getStringProperty("id")
                            onSeleccionActual(id?.let { i -> garajesActuales.firstOrNull { it.id == i } })
                            id != null
                        }
                        // Lazy loading: al terminar de mover el mapa se piden los marcadores del área visible.
                        m.addOnCameraIdleListener {
                            val b = m.projection.visibleRegion.latLngBounds
                            onAreaActual(AreaMapa(b.latitudeSouth, b.longitudeWest, b.latitudeNorth, b.longitudeEast))
                        }
                        estiloListo = true
                        val b = m.projection.visibleRegion.latLngBounds
                        onAreaActual(AreaMapa(b.latitudeSouth, b.longitudeWest, b.latitudeNorth, b.longitudeEast))
                    }
                }
            }
        },
        onRelease = { it.onDestroy() }
    )
}

private fun garajesAGeoJson(garajes: List<Garaje>, seleccionadoId: String?): FeatureCollection =
    FeatureCollection.fromFeatures(
        garajes.filter { it.tieneUbicacion }.map { g ->
            val efectiva = if (g.abiertoAhora == false) Disponibilidad.CLOSED else g.disponibilidad
            val color = colorDe(efectiva)
            Feature.fromGeometry(Point.fromLngLat(g.lng!!, g.lat!!)).apply {
                addStringProperty("id", g.id)
                addStringProperty("color", String.format("#%06X", 0xFFFFFF and color.toArgb()))
                addStringProperty("texto", if (efectiva == Disponibilidad.AVAILABLE) "#0B2D5B" else "#FFFFFF")
                addStringProperty("cupos", if (g.espaciosDisponibles > 99) "99+" else g.espaciosDisponibles.toString())
                addBooleanProperty("sel", g.id == seleccionadoId)
            }
        }
    )

// ─────────────────────────── Búsqueda y filtros ───────────────────────────

@Composable
private fun BarraBusqueda(
    texto: String,
    filtrosActivos: Int,
    onTexto: (String) -> Unit,
    onBuscar: () -> Unit,
    onFiltros: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextLight)
            TextField(
                value = texto,
                onValueChange = onTexto,
                modifier = Modifier.weight(1f),
                placeholder = { Text("¿A dónde vas? Barrio, dirección o garaje", fontSize = 14.sp, color = TextLight, maxLines = 1) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onBuscar() }),
                trailingIcon = if (texto.isNotEmpty()) {
                    { IconButton(onClick = { onTexto("") }) { Icon(Icons.Filled.Close, "Borrar búsqueda", tint = TextLight) } }
                } else null,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )
            BadgedBox(badge = {
                if (filtrosActivos > 0) Badge(containerColor = GreenAccent, contentColor = BluePrimary) { Text("$filtrosActivos") }
            }) {
                IconButton(
                    onClick = onFiltros,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Blue50)
                ) { Icon(Icons.Filled.FilterList, contentDescription = "Filtros", tint = BluePrimary) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosRapidos(filtros: FiltrosBusqueda, enVivo: Boolean, onCambio: (FiltrosBusqueda) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (enVivo) {
            Row(
                modifier = Modifier.shadow(2.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50))
                    .background(BluePrimary).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(GreenAccent))
                Spacer(Modifier.width(6.dp))
                Text("En vivo", color = SurfaceWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        ChipRapido("Con cupos", filtros.soloDisponibles) { onCambio(filtros.copy(soloDisponibles = it)) }
        ChipRapido("Abierto ahora", filtros.abiertoAhora) { onCambio(filtros.copy(abiertoAhora = it)) }
        ChipRapido("Moto", filtros.tipoVehiculo == TipoVehiculo.MOTORCYCLE) {
            onCambio(filtros.copy(tipoVehiculo = if (it) TipoVehiculo.MOTORCYCLE else null))
        }
        ChipRapido("Eléctrico", filtros.tipoVehiculo == TipoVehiculo.ELECTRIC) {
            onCambio(filtros.copy(tipoVehiculo = if (it) TipoVehiculo.ELECTRIC else null))
        }
    }
}

@Composable
private fun ChipRapido(texto: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    FilterChip(
        selected = activo,
        onClick = { onCambio(!activo) },
        label = { Text(texto, fontSize = 13.sp) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceWhite,
            labelColor = TextDark,
            selectedContainerColor = BluePrimary,
            selectedLabelColor = SurfaceWhite
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = activo, borderColor = BorderGray),
        elevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp)
    )
}

@Composable
private fun AvisoFlotante(texto: String, esError: Boolean = false, onCerrar: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        color = if (esError) RedError else BluePrimary.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Text(texto, color = SurfaceWhite, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
            if (onCerrar != null) {
                IconButton(onClick = onCerrar) { Icon(Icons.Filled.Close, "Cerrar aviso", tint = SurfaceWhite) }
            } else Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun BotonCircular(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(52.dp).shadow(6.dp, CircleShape).clickable(onClick = onClick),
        shape = CircleShape,
        color = SurfaceWhite
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icono, descripcion, tint = BluePrimary) }
    }
}

// ─────────────────────────── Tarjetas y lista ───────────────────────────

@Composable
private fun TarjetaSeleccion(
    garaje: Garaje,
    distanciaKm: Double?,
    onCerrar: () -> Unit,
    onDetalle: () -> Unit,
    onReservar: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(22.dp),
        color = SurfaceWhite,
        shadowElevation = 12.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(garaje.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    PildoraDisponibilidad(garaje.disponibilidad, garaje.abiertoAhora)
                }
                IconButton(onClick = onCerrar, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = TextLight)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Dato("Precio", "${Formatos.pesos(garaje.precioPorHora)}/h", Modifier.weight(1f))
                Dato("Cupos libres", "${garaje.espaciosDisponibles} de ${garaje.espaciosTotales}", Modifier.weight(1f))
                distanciaKm?.let { Dato("Distancia", Formatos.distancia(it), Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDetalle,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Ver detalle", color = BluePrimary, fontWeight = FontWeight.SemiBold) }
                BotonPrimario(
                    texto = if (garaje.reservable) "Reservar" else "Sin cupos",
                    onClick = onReservar,
                    enabled = garaje.reservable,
                    acento = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(BackgroundGray).padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(etiqueta, fontSize = 11.sp, color = TextMedium)
        Spacer(Modifier.height(2.dp))
        Text(valor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark, maxLines = 1)
    }
}

@Composable
private fun ListaResultados(
    resultados: List<Garaje>,
    cargando: Boolean,
    orden: OrdenBusqueda,
    hayUbicacion: Boolean,
    onOrden: (OrdenBusqueda) -> Unit,
    onClick: (Garaje) -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ordenar:", color = TextMedium, fontSize = 13.sp)
            OrdenBusqueda.entries
                .filter { it != OrdenBusqueda.DISTANCE || hayUbicacion }
                .forEach { o -> ChipRapido(o.etiqueta, o == orden) { if (it) onOrden(o) } }
        }
        when {
            resultados.isEmpty() && !cargando -> EstadoVacio(
                icono = Icons.Filled.SearchOff,
                titulo = "No hay garajes con esos filtros",
                texto = "Prueba ampliando el radio de búsqueda o quitando filtros.",
                accion = "Quitar filtros",
                onAccion = onLimpiar
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("${resultados.size} garaje(s) encontrados", color = TextMedium, fontSize = 13.sp)
                }
                items(resultados, key = { it.id }) { g -> TarjetaGarajeLista(g) { onClick(g) } }
            }
        }
    }
}

@Composable
private fun TarjetaGarajeLista(g: Garaje, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Indicador de cupos con el color de disponibilidad
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(colorDe(if (g.abiertoAhora == false) Disponibilidad.CLOSED else g.disponibilidad).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${g.espaciosDisponibles}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BluePrimary)
                    Text("libres", fontSize = 10.sp, color = TextMedium)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(g.nombre, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(g.direccion, g.ciudad).filter { it.isNotBlank() }.joinToString(", "),
                    fontSize = 12.sp, color = TextMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PildoraDisponibilidad(g.disponibilidad, g.abiertoAhora)
                    g.distanciaKm?.let { Text(Formatos.distancia(it), fontSize = 12.sp, color = TextMedium) }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(Formatos.pesos(g.precioPorHora), fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 15.sp)
                Text("por hora", fontSize = 11.sp, color = TextMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FiltrosBottomSheet(
    filtros: FiltrosBusqueda,
    onAplicar: (FiltrosBusqueda) -> Unit,
    onDismiss: () -> Unit
) {
    var f by remember { mutableStateOf(filtros) }
    var precioTexto by remember { mutableStateOf(filtros.precioMax?.toString().orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text("Filtros de búsqueda", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(20.dp))

            Text("Radio de búsqueda: ${f.radioKm.toInt()} km", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
            Slider(
                value = f.radioKm.toFloat(),
                onValueChange = { f = f.copy(radioKm = it.toInt().toDouble().coerceAtLeast(1.0)) },
                valueRange = 1f..20f,
                colors = SliderDefaults.colors(thumbColor = BluePrimary, activeTrackColor = GreenAccent)
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = precioTexto,
                onValueChange = { precioTexto = it.filter(Char::isDigit).take(7) },
                label = { Text("Precio máximo por hora (COP)") },
                placeholder = { Text("Ej: 5000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = camposColores(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Tipo de vehículo", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipRapido("Todos", f.tipoVehiculo == null) { f = f.copy(tipoVehiculo = null) }
                TipoVehiculo.entries.forEach { t ->
                    ChipRapido(t.etiqueta, f.tipoVehiculo == t) { sel -> f = f.copy(tipoVehiculo = if (sel) t else null) }
                }
            }

            Spacer(Modifier.height(12.dp))
            FilaSwitch("Solo con cupos libres", f.soloDisponibles) { f = f.copy(soloDisponibles = it) }
            FilaSwitch("Solo abiertos ahora", f.abiertoAhora) { f = f.copy(abiertoAhora = it) }

            Spacer(Modifier.height(12.dp))
            Text("Ordenar por", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OrdenBusqueda.entries.forEach { o -> ChipRapido(o.etiqueta, f.orden == o) { if (it) f = f.copy(orden = o) } }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        f = FiltrosBusqueda(texto = filtros.texto)
                        precioTexto = ""
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Limpiar", color = TextMedium) }
                BotonPrimario(
                    "Aplicar",
                    onClick = { onAplicar(f.copy(precioMax = precioTexto.toIntOrNull()?.takeIf { it > 0 })) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilaSwitch(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCambio(!valor) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, fontSize = 14.sp, color = TextDark, modifier = Modifier.weight(1f))
        Switch(
            checked = valor,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(checkedTrackColor = GreenAccentDark, checkedThumbColor = SurfaceWhite)
        )
    }
}
