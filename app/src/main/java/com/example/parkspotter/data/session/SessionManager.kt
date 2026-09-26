package com.example.parkspotter.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.parkspotter.model.RolUsuario

/** Guarda el token JWT y datos locales de sesión en SharedPreferences. */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("parkspotter_session", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    var rolLocal: RolUsuario
        get() = RolUsuario.valueOf(prefs.getString("rol_local", RolUsuario.CONDUCTOR.name)!!)
        set(value) = prefs.edit().putString("rol_local", value.name).apply()

    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()

    fun clear() = prefs.edit().clear().apply()
}