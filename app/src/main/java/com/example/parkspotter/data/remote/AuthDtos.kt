package com.example.parkspotter.data.remote

// Contrato de /api/v1/auth (backend rama develop). El registro solo recibe correo y contraseña:
// nombre, teléfono, ciudad y modo de uso se guardan en el dispositivo (PerfilLocalStore).

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String
)

data class TokenPairResponse(
    val userId: String?,
    val email: String?,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String?,
    val expiresInSeconds: Long?
)

data class RefreshRequest(val refreshToken: String)

data class LogoutRequest(val refreshToken: String)

data class UserResponse(
    val id: String,
    val email: String,
    val roles: List<String>?,
    val enabled: Boolean?
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
