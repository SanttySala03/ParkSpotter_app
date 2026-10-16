package com.example.parkspotter.util

import com.example.parkspotter.model.Garaje

/* Mismas reglas que valida el backend (ValidationPatterns / PasswordPolicyValidator / GarageRequest). */

private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val REGEX_TELEFONO = Regex("^\\+?[0-9()\\- ]{7,20}$")
private val REGEX_LUGAR = Regex("^\\s*\\p{L}[\\p{L} .'-]*$")
private val REGEX_POSTAL = Regex("^[A-Za-z0-9 -]{3,10}$")

fun esEmailValido(email: String): Boolean = REGEX_EMAIL.matches(email.trim())

/** Política de contraseñas nuevas del backend. Devuelve el mensaje de error o null si es válida. */
fun errorPassword(password: String): String? = when {
    password.length < 12 -> "La contraseña debe tener mínimo 12 caracteres"
    password.toByteArray(Charsets.UTF_8).size > 72 -> "La contraseña admite máximo 72 caracteres"
    password != password.trim() -> "La contraseña no puede empezar ni terminar con espacios"
    password.none { it.isLetter() } || password.none { it.isDigit() } ->
        "La contraseña debe tener al menos una letra y un número"
    else -> null
}

fun esTelefonoValido(telefono: String): Boolean = REGEX_TELEFONO.matches(telefono.trim())

/** Reglas de GarageRequest (Sprint 2/3) para dar feedback antes de llamar al backend. */
object ValidacionGaraje {
    fun validar(g: Garaje): String? = when {
        g.nombre.trim().length !in 3..150 -> "El nombre debe tener entre 3 y 150 caracteres"
        g.direccion.trim().length !in 5..255 -> "Ingresa la dirección completa (mínimo 5 caracteres)"
        !REGEX_LUGAR.matches(g.ciudad) || g.ciudad.trim().length < 2 -> "Ingresa una ciudad válida (solo letras)"
        !REGEX_LUGAR.matches(g.departamento) || g.departamento.trim().length < 2 -> "Ingresa un departamento válido (solo letras)"
        !REGEX_LUGAR.matches(g.pais) || g.pais.trim().length < 2 -> "Ingresa un país válido (solo letras)"
        g.codigoPostal.isNotBlank() && !REGEX_POSTAL.matches(g.codigoPostal.trim()) ->
            "El código postal debe tener entre 3 y 10 letras o números"
        g.telefono.isNotBlank() && !esTelefonoValido(g.telefono) -> "Teléfono inválido (7 a 20 dígitos, puede iniciar con +)"
        g.precioPorHora !in 1..1_000_000 -> "Ingresa un precio por hora válido"
        g.espaciosTotales !in 1..5000 -> "Ingresa entre 1 y 5000 plazas"
        !g.abierto24h && (g.horaApertura == null || g.horaCierre == null) -> "Elige la hora de apertura y de cierre"
        !g.abierto24h && g.horaApertura == g.horaCierre -> "Apertura y cierre no pueden ser iguales (si abre todo el día, marca 24 horas)"
        else -> null
    }
}
