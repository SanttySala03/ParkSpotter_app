package com.example.parkspotter.data.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tokens JWT de la sesión. Se guardan cifrados (EncryptedSharedPreferences, AES-256) como pide
 * la Definition of Done; si el Keystore del dispositivo falla se usa almacenamiento privado normal
 * para no bloquear el inicio de sesión.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = prefsCifradas(context.applicationContext)

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) = prefs.edit().putString("user_id", value).apply()

    var email: String?
        get() = prefs.getString("email", null)
        set(value) = prefs.edit().putString("email", value).apply()

    val isLoggedIn: Boolean get() = !refreshToken.isNullOrBlank()

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val ARCHIVO = "parkspotter_session_secure"

        @Volatile private var instancia: SharedPreferences? = null

        private fun prefsCifradas(context: Context): SharedPreferences =
            instancia ?: synchronized(this) {
                instancia ?: runCatching {
                    EncryptedSharedPreferences.create(
                        ARCHIVO,
                        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                }.getOrElse { e ->
                    Log.w("SessionManager", "Keystore no disponible, se usa almacenamiento privado", e)
                    context.getSharedPreferences(ARCHIVO + "_plain", Context.MODE_PRIVATE)
                }.also { instancia = it }
            }

        private val _sesionExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /** Emite cuando el refresh token ya no sirve: la UI debe volver al Login. */
        val sesionExpirada: SharedFlow<Unit> = _sesionExpirada.asSharedFlow()

        internal fun notificarSesionExpirada() {
            _sesionExpirada.tryEmit(Unit)
        }
    }
}
