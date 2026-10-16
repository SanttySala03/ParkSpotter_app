package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.ui.components.BarraInferior
import com.example.parkspotter.ui.components.BotonPrimario
import com.example.parkspotter.ui.components.Pestana
import com.example.parkspotter.ui.components.camposColores
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.errorPassword
import com.example.parkspotter.util.esTelefonoValido
import com.example.parkspotter.viewmodel.AuthViewModel

/**
 * Sprint 1 — Gestión del perfil: datos, modo de uso (conductor / propietario), cambio de
 * contraseña (PATCH /auth/password) y cierre de sesión (POST /auth/logout).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onPestana: (Pestana) -> Unit,
    onRolCambiado: (RolUsuario) -> Unit,
    onSesionCerrada: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val perfil by viewModel.perfil.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }
    var passActual by remember { mutableStateOf("") }
    var passNueva by remember { mutableStateOf("") }
    var mostrarCambioPass by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }
    val rol = perfil?.rol ?: viewModel.rolActual()

    LaunchedEffect(Unit) { viewModel.cargarPerfil() }
    LaunchedEffect(perfil?.email) {
        perfil?.let {
            nombre = it.nombre
            telefono = it.telefono
            ciudad = it.ciudad
        }
    }
    LaunchedEffect(mensaje) {
        mensaje?.let { snackbar.showSnackbar(it); viewModel.limpiarMensaje() }
    }

    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        bottomBar = { BarraInferior(Pestana.PERFIL, rol == RolUsuario.PROPIETARIO, onPestana) }
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(BluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (perfil?.nombre?.takeIf { it.isNotBlank() } ?: perfil?.email ?: "?").take(1).uppercase(),
                        color = GreenAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(perfil?.nombre?.takeIf { it.isNotBlank() } ?: "Tu perfil", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(perfil?.email ?: "…", color = TextMedium, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Seccion("¿Cómo usas ParkSpotter?")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OpcionRol("Conductor", "Busco y reservo", Icons.Filled.DirectionsCar, rol == RolUsuario.CONDUCTOR, Modifier.weight(1f)) {
                    if (rol != RolUsuario.CONDUCTOR) { viewModel.cambiarRol(RolUsuario.CONDUCTOR); onRolCambiado(RolUsuario.CONDUCTOR) }
                }
                OpcionRol("Propietario", "Publico mi garaje", Icons.Filled.Home, rol == RolUsuario.PROPIETARIO, Modifier.weight(1f)) {
                    if (rol != RolUsuario.PROPIETARIO) { viewModel.cambiarRol(RolUsuario.PROPIETARIO); onRolCambiado(RolUsuario.PROPIETARIO) }
                }
            }
            Text(
                "Como propietario también puedes buscar y reservar otros garajes.",
                fontSize = 12.sp, color = TextMedium, modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Seccion("Datos personales")
            CampoPerfil("Nombre completo", nombre) { nombre = it; viewModel.limpiarError() }
            CampoPerfil("Teléfono", telefono, KeyboardType.Phone) { telefono = it; viewModel.limpiarError() }
            CampoPerfil("Ciudad", ciudad) { ciudad = it; viewModel.limpiarError() }
            Text(
                "Estos datos se guardan en este celular hasta que el backend permita editarlos.",
                fontSize = 12.sp, color = TextMedium
            )

            error?.let { Text(it, color = RedError, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }

            Spacer(modifier = Modifier.height(12.dp))
            BotonPrimario(
                "Guardar cambios",
                onClick = {
                    when {
                        nombre.isBlank() -> viewModel.mostrarError("El nombre es obligatorio")
                        telefono.isNotBlank() && !esTelefonoValido(telefono) ->
                            viewModel.mostrarError("Teléfono inválido (7 a 20 dígitos, puede iniciar con +)")
                        else -> viewModel.guardarPerfil(nombre, telefono, ciudad)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BorderGray)
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { mostrarCambioPass = !mostrarCambioPass }) {
                Text(if (mostrarCambioPass) "Ocultar cambio de contraseña" else "Cambiar contraseña", color = BluePrimary)
            }
            if (mostrarCambioPass) {
                CampoPerfil("Contraseña actual", passActual, KeyboardType.Password, esPassword = true) { passActual = it }
                CampoPerfil("Nueva contraseña", passNueva, KeyboardType.Password, esPassword = true) { passNueva = it }
                Text("Mínimo 12 caracteres, con al menos una letra y un número.", fontSize = 12.sp, color = TextMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val errorNueva = errorPassword(passNueva)
                        when {
                            passActual.isBlank() -> viewModel.mostrarError("Ingresa tu contraseña actual")
                            errorNueva != null -> viewModel.mostrarError(errorNueva)
                            passNueva == passActual -> viewModel.mostrarError("La nueva contraseña debe ser distinta de la actual")
                            else -> viewModel.cambiarPassword(passActual, passNueva) {
                                passActual = ""
                                passNueva = ""
                                mostrarCambioPass = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Actualizar contraseña", color = BluePrimary) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { confirmarSalida = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que ingresar tu correo y contraseña de nuevo.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; viewModel.logout(onSesionCerrada) }) {
                    Text("Cerrar sesión", color = RedError)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(texto, color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
}

@Composable
private fun OpcionRol(
    titulo: String,
    subtitulo: String,
    icono: ImageVector,
    seleccionado: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (seleccionado) Blue50 else SurfaceWhite)
            .border(if (seleccionado) 2.dp else 1.dp, if (seleccionado) BluePrimary else BorderGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icono, contentDescription = null, tint = if (seleccionado) BluePrimary else TextLight)
        Spacer(Modifier.height(6.dp))
        Text(titulo, fontWeight = FontWeight.SemiBold, color = TextDark, fontSize = 14.sp)
        Text(subtitulo, color = TextMedium, fontSize = 11.sp)
    }
}

@Composable
private fun CampoPerfil(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    esPassword: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = camposColores(),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
}
