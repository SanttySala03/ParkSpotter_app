package com.example.parkspotter.data.remote

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val rol: String,
    val nombre: String? = null,
    val telefono: String? = null,
    val ciudad: String? = null
)

data class TokenPairResponse(
    val userId: String?,
    val email: String?,
    val accessToken: String,
    val refreshToken: String
)

data class RefreshRequest(val refreshToken: String)

data class LogoutRequest(val refreshToken: String)

data class UserResponse(
    val id: String,
    val email: String,
    val nombre: String?,
    val telefono: String?,
    val ciudad: String?,
    val roles: List<String>
)

data class UpdateProfileRequest(
    val nombre: String,
    val telefono: String?,
    val ciudad: String?
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
