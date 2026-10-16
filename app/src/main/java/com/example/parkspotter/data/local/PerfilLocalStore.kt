package com.example.parkspotter.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.parkspotter.model.RolUsuario

/**
 * Datos de perfil que el backend todavía no guarda (nombre, teléfono, ciudad y modo de uso).
 * Se guardan por correo, así sobreviven al cierre de sesión y a cambiar de cuenta en el mismo
 * celular. Cuando el backend agregue /auth/me con esos campos, AuthRepository debe preferirlos.
 */
class PerfilLocalStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("parkspotter_perfiles", Context.MODE_PRIVATE)

    private fun clave(email: String, campo: String) = "${email.trim().lowercase()}::$campo"

    fun rol(email: String): RolUsuario =
        runCatching { RolUsuario.valueOf(prefs.getString(clave(email, "rol"), null)!!) }
            .getOrDefault(RolUsuario.CONDUCTOR)

    fun guardarRol(email: String, rol: RolUsuario) =
        prefs.edit().putString(clave(email, "rol"), rol.name).apply()

    fun nombre(email: String): String = prefs.getString(clave(email, "nombre"), "").orEmpty()
    fun telefono(email: String): String = prefs.getString(clave(email, "telefono"), "").orEmpty()
    fun ciudad(email: String): String = prefs.getString(clave(email, "ciudad"), "").orEmpty()

    fun guardarDatos(email: String, nombre: String, telefono: String, ciudad: String) {
        prefs.edit()
            .putString(clave(email, "nombre"), nombre.trim())
            .putString(clave(email, "telefono"), telefono.trim())
            .putString(clave(email, "ciudad"), ciudad.trim())
            .apply()
    }

    /** Placa usada en la última reserva: se sugiere en la siguiente. */
    fun ultimaPlaca(email: String): String = prefs.getString(clave(email, "placa"), "").orEmpty()
    fun guardarPlaca(email: String, placa: String) =
        prefs.edit().putString(clave(email, "placa"), placa.trim().uppercase()).apply()
}
