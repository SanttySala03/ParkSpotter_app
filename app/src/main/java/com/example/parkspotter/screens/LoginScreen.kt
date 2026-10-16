package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.ui.components.LogoParkSpotter
import com.example.parkspotter.ui.components.MarcaParkSpotter
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.esEmailValido
import com.example.parkspotter.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (RolUsuario) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val isLoading by viewModel.loading.collectAsState()
    val backendError by viewModel.error.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var mostrarRecuperar by remember { mutableStateOf(false) }
    val errorMessage = localError ?: backendError

    IconosBarraEstado(fondoOscuro = false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo de marca: P + vehículo + plaza de parqueo
            LogoParkSpotter(modifier = Modifier.size(88.dp))
            Spacer(modifier = Modifier.height(12.dp))
            MarcaParkSpotter(tamano = 30.sp, eslogan = true)
            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Bienvenido de nuevo",
                color = TextDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Inicia sesión para continuar",
                color = TextMedium,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 28.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; localError = null; viewModel.limpiarError() },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TextLight) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = parkSpotterFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; localError = null; viewModel.limpiarError() },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = TextLight) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = TextLight
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = parkSpotterFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = RedError,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(top = 8.dp)
                )
            }

            TextButton(
                onClick = { mostrarRecuperar = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("¿Olvidaste tu contraseña?", color = BluePrimary, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() ->
                            localError = "Completa correo y contraseña"
                        !esEmailValido(email) ->
                            localError = "Ingresa un correo válido, ej. nombre@dominio.com"
                        else -> {
                            localError = null
                            viewModel.login(email, password) { rol -> onLoginSuccess(rol) }
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    contentColor = SurfaceWhite
                ),
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
                    Text("Iniciar sesión", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes cuenta? ", color = TextMedium, fontSize = 14.sp)
                Text(
                    text = "Regístrate",
                    color = GreenAccentDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (mostrarRecuperar) {
        AlertDialog(
            onDismissRequest = { mostrarRecuperar = false },
            title = { Text("Recuperar contraseña") },
            text = {
                Text(
                    "La recuperación por correo estará disponible cuando el backend publique el endpoint " +
                        "(pendiente en el backlog). Mientras tanto, si recuerdas tu contraseña puedes cambiarla desde tu perfil."
                )
            },
            confirmButton = { TextButton(onClick = { mostrarRecuperar = false }) { Text("Entendido") } }
        )
    }
}

@Composable
private fun parkSpotterFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BluePrimary,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = BluePrimary,
    unfocusedLabelColor = TextMedium,
    cursorColor = BluePrimary,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite
)
