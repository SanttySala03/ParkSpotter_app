package com.example.parkspotter.model

enum class RolUsuario { CONDUCTOR, PROPIETARIO }

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val ciudad: String = "",
    val rol: RolUsuario = RolUsuario.CONDUCTOR,
    val fotoUrl: String? = null
)
