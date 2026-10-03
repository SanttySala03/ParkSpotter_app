package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.viewmodel.AuthViewModel

/**
 * Sprint 1 — Gestión del perfil: ver datos y rol, editar nombre/teléfono/ciudad,
 * cambiar contraseña y cerrar sesión. Todo contra el backend (/auth/me, /auth/password, /auth/logout).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onVolver: () -> Unit,
    onSesionCerrada: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(context))
    val perfil by viewModel.perfil.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("") }
    var passActual by remember { mutableStateOf("") }
    var passNueva by remember { mutableStateOf("") }
    var mostrarCambioPass by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargarPerfil() }
    LaunchedEffect(perfil) {
        perfil?.let {
            nombre = it.nombre
            telefono = it.telefono
            ciudad = it.ciudad
        }
    }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Blue50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = BluePrimary)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(perfil?.email ?: "…", color = TextDark, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = when (perfil?.rol) {
                            RolUsuario.PROPIETARIO -> "Propietario"
                            RolUsuario.CONDUCTOR -> "Conductor"
                            null -> ""
                        },
                        color = GreenAccentDark,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Datos personales", color = TextDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            CampoPerfil("Nombre completo", nombre) { nombre = it; viewModel.limpiarError() }
            CampoPerfil("Teléfono", telefono, KeyboardType.Phone) { telefono = it; viewModel.limpiarError() }
            CampoPerfil("Ciudad", ciudad) { ciudad = it; viewModel.limpiarError() }

            error?.let { Text(it, color = RedError, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
            mensaje?.let { Text(it, color = GreenSuccess, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (nombre.isBlank()) viewModel.mostrarError("El nombre es obligatorio")
                    else viewModel.guardarPerfil(nombre, telefono, ciudad)
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary, contentColor = SurfaceWhite),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = SurfaceWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Guardar cambios", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = BorderGray)
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { mostrarCambioPass = !mostrarCambioPass }) {
                Text(if (mostrarCambioPass) "Ocultar cambio de contraseña" else "Cambiar contraseña", color = BluePrimary)
            }
            if (mostrarCambioPass) {
                CampoPerfil("Contraseña actual", passActual, KeyboardType.Password, esPassword = true) { passActual = it }
                CampoPerfil("Nueva contraseña (mín. 8)", passNueva, KeyboardType.Password, esPassword = true) { passNueva = it }
                OutlinedButton(
                    onClick = {
                        when {
                            passActual.isBlank() -> viewModel.mostrarError("Ingresa tu contraseña actual")
                            passNueva.length < 8 -> viewModel.mostrarError("La nueva contraseña debe tener mínimo 8 caracteres")
                            else -> viewModel.cambiarPassword(passActual, passNueva) {
                                passActual = ""
                                passNueva = ""
                                mostrarCambioPass = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Actualizar contraseña") }
            }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { viewModel.logout(onSesionCerrada) },
                shape = RoundedCornerShape(12.dp),
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
        visualTransformation = if (esPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = BorderGray,
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}
