package com.example.parkspotter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (RolUsuario) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(context))
    val isLoading by viewModel.loading.collectAsState()
    val backendError by viewModel.error.collectAsState()

    var rolSeleccionado by remember { mutableStateOf(RolUsuario.CONDUCTOR) }

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Campos adicionales exclusivos del flujo Propietario
    var telefono by remember { mutableStateOf("") }
    var ciudad by remember { mutableStateOf("Bogotá") }

    var localError by remember { mutableStateOf<String?>(null) }
    val errorMessage = localError ?: backendError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Crea tu cuenta",
                color = TextDark,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Elige cómo quieres usar ParkSpotter",
                color = TextMedium,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // ── Selector de rol: Conductor / Propietario ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RolCard(
                    titulo = "Conductor",
                    subtitulo = "Busco parqueadero",
                    icono = Icons.Filled.DirectionsCar,
                    seleccionado = rolSeleccionado == RolUsuario.CONDUCTOR,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) { rolSeleccionado = RolUsuario.CONDUCTOR }

                RolCard(
                    titulo = "Propietario",
                    subtitulo = "Ofrezco mi garaje",
                    icono = Icons.Filled.Home,
                    seleccionado = rolSeleccionado == RolUsuario.PROPIETARIO,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) { rolSeleccionado = RolUsuario.PROPIETARIO }
            }

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre completo") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = registerFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = registerFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Campos exclusivos del flujo Propietario
            if (rolSeleccionado == RolUsuario.PROPIETARIO) {
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono de contacto") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    colors = registerFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = ciudad,
                    onValueChange = { ciudad = it },
                    label = { Text("Ciudad") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = registerFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = registerFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp),
                colors = registerFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = RedError,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    localError = when {
                        nombre.isBlank() || email.isBlank() || password.isBlank() ->
                            "Completa todos los campos obligatorios"
                        !email.contains("@") -> "Ingresa un correo válido"
                        password.length < 6 -> "La contraseña debe tener mínimo 6 caracteres"
                        password != confirmPassword -> "Las contraseñas no coinciden"
                        rolSeleccionado == RolUsuario.PROPIETARIO && telefono.isBlank() ->
                            "Ingresa un teléfono de contacto"
                        else -> {
                            viewModel.register(email, password, rolSeleccionado) {
                                onRegisterSuccess(rolSeleccionado)
                            }
                            null
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
                    Text(
                        text = if (rolSeleccionado == RolUsuario.CONDUCTOR)
                            "Registrarme como conductor"
                        else "Registrarme como propietario",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿Ya tienes cuenta? ", color = TextMedium, fontSize = 14.sp)
                Text(
                    text = "Inicia sesión",
                    color = GreenAccentDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RolCard(
    titulo: String,
    subtitulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (seleccionado) BluePrimary else BorderGray
    val bgColor = if (seleccionado) Blue50 else SurfaceWhite
    val iconTint = if (seleccionado) BluePrimary else TextLight

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (seleccionado) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icono, contentDescription = titulo, tint = iconTint, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(titulo, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitulo, color = TextMedium, fontSize = 11.sp)
    }
}

@Composable
private fun registerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BluePrimary,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = BluePrimary,
    unfocusedLabelColor = TextMedium,
    cursorColor = BluePrimary,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite
)
