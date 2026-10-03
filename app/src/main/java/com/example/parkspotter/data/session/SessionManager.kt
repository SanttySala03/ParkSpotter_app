package com.example.parkspotter.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.parkspotter.model.RolUsuario
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Guarda los tokens JWT y datos locales de sesión en SharedPreferences. */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("parkspotter_session", Context.MODE_PRIVATE)

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    var rolLocal: RolUsuario
        get() = runCatching {
            RolUsuario.valueOf(prefs.getString("rol_local", RolUsuario.CONDUCTOR.name)!!)
        }.getOrDefault(RolUsuario.CONDUCTOR)
        set(value) = prefs.edit().putString("rol_local", value.name).apply()

    var email: String?
        get() = prefs.getString("email", null)
        set(value) = prefs.edit().putString("email", value).apply()

    val isLoggedIn: Boolean get() = !refreshToken.isNullOrBlank()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private val _sesionExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /** Emite cuando el refresh token ya no sirve: la UI debe volver al Login. */
        val sesionExpirada: SharedFlow<Unit> = _sesionExpirada.asSharedFlow()

        internal fun notificarSesionExpirada() {
            _sesionExpirada.tryEmit(Unit)
        }
    }
}
