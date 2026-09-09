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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.parkspotter.ui.theme.*
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

// ── Modelo de datos ───────────────────────────────────────────────────────────
data class ParkingSpot(
    val id: Int,
    val name: String,
    val address: String,
    val price: String,
    val status: ParkingStatus,
    val lat: Double,
    val lng: Double,
    val availableSpots: Int,
    val totalSpots: Int
)

enum class ParkingStatus { AVAILABLE, RESERVED, FULL }

// ── Datos de ejemplo en Bogotá ────────────────────────────────────────────────
val sampleParkings = listOf(
    ParkingSpot(1, "Garaje Chapinero", "Calle 57 #13-20, Chapinero", "$3.000/hora",
        ParkingStatus.AVAILABLE, 4.6486, -74.0632, 3, 5),
    ParkingSpot(2, "Parqueadero Usaquén", "Cra 7 #119-45, Usaquén", "$4.500/hora",
        ParkingStatus.AVAILABLE, 4.6951, -74.0317, 1, 3),
    ParkingSpot(3, "Garaje Teusaquillo", "Calle 34 #17-12, Teusaquillo", "$2.500/hora",
        ParkingStatus.RESERVED, 4.6436, -74.0849, 0, 2),
    ParkingSpot(4, "Parqueadero Zona Rosa", "Cra 15 #88-10, Zona Rosa", "$5.000/hora",
        ParkingStatus.AVAILABLE, 4.6667, -74.0536, 2, 4),
    ParkingSpot(5, "Garaje Kennedy", "Calle 40 Sur #78-30, Kennedy", "$2.000/hora",
        ParkingStatus.FULL, 4.6275, -74.1460, 0, 3),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContent {
            ParkSpotterTheme {
                ParkSpotterApp()
            }
        }
    }
}

@Composable
fun ParkSpotterApp() {
    val context = LocalContext.current
    var selectedParking by remember { mutableStateOf<ParkingSpot?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Mapa ──────────────────────────────────────────────────────────────
        ParkSpotterMap(
            parkings = sampleParkings,
            onParkingSelected = { parking ->
                selectedParking = parking
                showBottomSheet = true
            }
        )

        // ── Navbar superior ───────────────────────────────────────────────────
        TopNavBar()

        // ── Botón de ubicación ────────────────────────────────────────────────
        FloatingLocationButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (showBottomSheet) 320.dp else 24.dp),
            onClick = {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                    onReserve = { /* TODO: implementar reserva */ }
                )
            }
        }
    }
}

@Composable
fun ParkSpotterMap(
    parkings: List<ParkingSpot>,
    onParkingSelected: (ParkingSpot) -> Unit
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            MapView(context).also { mv ->
                mv.getMapAsync { map ->
                    map.setStyle(
                        "https://tiles.openfreemap.org/styles/liberty"
                    ) { style ->

                        // Centrar en Bogotá
                        map.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(4.6782, -74.0582))
                                    .zoom(12.0)
                                    .build()
                            )
                        )

                        // Agregar marcadores como círculos GeoJSON
                        val features = parkings.joinToString(",") { parking ->
                            val color = when (parking.status) {
                                ParkingStatus.AVAILABLE -> "#059669"
                                ParkingStatus.RESERVED  -> "#F59E0B"
                                ParkingStatus.FULL      -> "#EF4444"
                            }
                            """
            {
                "type": "Feature",
                "properties": {
                    "id": ${parking.id},
                    "color": "$color",
                    "name": "${parking.name}"
                },
                "geometry": {
                    "type": "Point",
                    "coordinates": [${parking.lng}, ${parking.lat}]
                }
            }
            """
                        }

                        val geojson = """
            {
                "type": "FeatureCollection",
                "features": [$features]
            }
        """

                        // Agregar fuente GeoJSON
                        style.addSource(
                            org.maplibre.android.style.sources.GeoJsonSource("parkings-source", geojson)
                        )

                        // Capa de círculos
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

                        // Detectar clic en marcador
                        map.addOnMapClickListener { point ->
                            val pixel = map.projection.toScreenLocation(point)
                            val features = map.queryRenderedFeatures(pixel, "parkings-layer")
                            if (features.isNotEmpty()) {
                                val id = features[0].getNumberProperty("id")?.toInt()
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

    // Overlay de marcadores sobre el mapa
    Box(modifier = Modifier.fillMaxSize()) {
        // Nota: en producción usar SymbolManager de MapLibre
        // Por ahora mostramos indicadores visuales
    }
}

@Composable
fun TopNavBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(top = 32.dp)
    ) {
        // Barra de búsqueda
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceWhite
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🅿", fontSize = 20.sp)
                Text(
                    text = "Buscar parqueaderos...",
                    color = TextLight,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Blue50),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⚙", fontSize = 14.sp)
                }
            }
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
    parking: ParkingSpot,
    onDismiss: () -> Unit,
    onReserve: () -> Unit
) {
    val statusColor = when (parking.status) {
        ParkingStatus.AVAILABLE -> GreenSuccess
        ParkingStatus.RESERVED  -> YellowWarning
        ParkingStatus.FULL      -> RedError
    }
    val statusText = when (parking.status) {
        ParkingStatus.AVAILABLE -> "Disponible"
        ParkingStatus.RESERVED  -> "Reservado"
        ParkingStatus.FULL      -> "Ocupado"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = SurfaceWhite,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderGray)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parking.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parking.address,
                        fontSize = 13.sp,
                        color = TextMedium
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = "💰",
                    label = "Precio",
                    value = parking.price
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = "🅿",
                    label = "Espacios",
                    value = "${parking.availableSpots}/${parking.totalSpots}"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMedium)
                ) {
                    Text("Cerrar", fontSize = 14.sp)
                }
                Button(
                    onClick = onReserve,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (parking.status == ParkingStatus.FULL)
                            BorderGray else BluePrimary,
                        contentColor = SurfaceWhite
                    ),
                    enabled = parking.status != ParkingStatus.FULL
                ) {
                    Text(
                        text = if (parking.status == ParkingStatus.FULL) "Sin espacios" else "Reservar",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = BackgroundGray
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = TextLight)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        }
    }
}