package com.example.parkspotter.model

/**
 * Modo de uso de la app. El backend asigna a todos ROLE_USER y cualquier usuario puede publicar
 * garajes, así que el "rol" es una preferencia de la app (qué pantalla de inicio y pestañas ver),
 * guardada por correo en el dispositivo y editable desde el perfil.
 */
enum class RolUsuario { CONDUCTOR, PROPIETARIO }

data class Usuario(
    val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val ciudad: String = "",
    val rol: RolUsuario = RolUsuario.CONDUCTOR,
    val esAdmin: Boolean = false
)
