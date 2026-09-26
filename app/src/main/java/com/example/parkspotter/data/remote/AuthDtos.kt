package com.example.parkspotter.data.remote

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val rol: String
)

data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val roles: List<String>
)
