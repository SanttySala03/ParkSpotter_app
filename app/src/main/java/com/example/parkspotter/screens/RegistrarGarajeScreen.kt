package com.example.parkspotter.screens

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
import com.example.parkspotter.viewmodel.GarajeViewModel

/**
 * Formulario para que un propietario publique un garaje en desuso.
 * onGarajeGuardado se dispara solo después de que el backend confirma
 * la creación (respuesta 2xx de POST /garajes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarGarajeScreen(
    onGarajeGuardado: (Garaje) -> Unit,
    onCancelar: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GarajeViewModel = viewModel(factory = GarajeViewModel.factory(context))
    val isLoading by viewModel.loading.collectAsState()
    val backendError by viewModel.error.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var espacios by remember { mutableStateOf("1") }
    var fotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var localError by remember { mutableStateOf<String?>(null) }
    val errorMessage = localError ?: backendError

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> fotos = (fotos + uris).take(6) }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Publicar garaje", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
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
                onValueChange = { nombre = it },
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
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                placeholder = { Text("Calle, barrio, Bogotá") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = garajeFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
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
                            val garaje = Garaje(
                                nombre = nombre,
                                direccion = direccion,
                                descripcion = descripcion,
                                precioPorHora = precioInt,
                                espaciosTotales = espaciosInt,
                                espaciosDisponibles = espaciosInt,
                                // Nota: son URIs locales del celular, aún no se suben a
                                // ningún storage (Firebase/S3) — pendiente fuera de Sprint 2.
                                fotos = fotos.map { it.toString() },
                                activo = true
                            )
                            viewModel.publicarGaraje(garaje) { exito ->
                                if (exito) onGarajeGuardado(garaje)
                            }
                            null
                        }
                    }
                },
                enabled = !isLoading,
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
                    Text("Publicar garaje", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
