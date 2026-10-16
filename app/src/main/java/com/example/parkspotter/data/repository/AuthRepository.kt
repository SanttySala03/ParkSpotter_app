package com.example.parkspotter.data.repository

import com.example.parkspotter.data.local.PerfilLocalStore
import com.example.parkspotter.data.remote.AuthApi
import com.example.parkspotter.data.remote.ChangePasswordRequest
import com.example.parkspotter.data.remote.LoginRequest
import com.example.parkspotter.data.remote.LogoutRequest
import com.example.parkspotter.data.remote.RegisterRequest
import com.example.parkspotter.data.remote.TokenPairResponse
import com.example.parkspotter.data.remote.UserResponse
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.model.Usuario
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.safeApiCall

class AuthRepository(
    private val api: AuthApi,
    private val session: SessionManager,
    private val perfiles: PerfilLocalStore
) {

    suspend fun login(email: String, password: String): Resource<TokenPairResponse> {
        val result = safeApiCall { api.login(LoginRequest(email.trim(), password)) }
        if (result is Resource.Success) saveTokens(result.data, email)
        return result
    }

    /**
     * El backend solo registra correo y contraseña. Nombre, teléfono, ciudad y modo de uso
     * quedan en el dispositivo (PerfilLocalStore) hasta que la API los soporte.
     */
    suspend fun register(
        email: String,
        password: String,
        rol: RolUsuario,
        nombre: String,
        telefono: String?,
        ciudad: String?
    ): Resource<TokenPairResponse> {
        val correo = email.trim()
        val result = safeApiCall { api.register(RegisterRequest(correo, password)) }
        if (result is Resource.Success) {
            saveTokens(result.data, correo)
            perfiles.guardarRol(correo, rol)
            perfiles.guardarDatos(correo, nombre, telefono.orEmpty(), ciudad.orEmpty())
        }
        return result
    }

    /** Modo de uso guardado para el usuario de la sesión (CONDUCTOR si no eligió). */
    fun rolGuardado(): RolUsuario = session.email?.let { perfiles.rol(it) } ?: RolUsuario.CONDUCTOR

    fun cambiarRol(rol: RolUsuario) {
        session.email?.let { perfiles.guardarRol(it, rol) }
    }

    suspend fun obtenerPerfil(): Resource<Usuario> = when (val r = safeApiCall { api.me() }) {
        is Resource.Success -> {
            session.email = r.data.email
            Resource.Success(r.data.aUsuario())
        }
        is Resource.Error -> r
        Resource.Loading -> Resource.Loading
    }

    fun guardarDatosPerfil(nombre: String, telefono: String, ciudad: String): Usuario? {
        val email = session.email ?: return null
        perfiles.guardarDatos(email, nombre, telefono, ciudad)
        return Usuario(
            id = session.userId.orEmpty(), email = email, nombre = nombre.trim(),
            telefono = telefono.trim(), ciudad = ciudad.trim(), rol = perfiles.rol(email)
        )
    }

    suspend fun cambiarPassword(actual: String, nueva: String): Resource<Unit> =
        safeApiCall { api.cambiarPassword(ChangePasswordRequest(actual, nueva)) }

    /** Revoca el refresh token en el backend (si hay red) y borra la sesión local. */
    suspend fun logout() {
        session.refreshToken?.let { token -> safeApiCall { api.logout(LogoutRequest(token)) } }
        session.clear()
    }

    private fun saveTokens(tokens: TokenPairResponse, emailIngresado: String) {
        session.accessToken = tokens.accessToken
        session.refreshToken = tokens.refreshToken
        session.userId = tokens.userId
        session.email = tokens.email ?: emailIngresado.trim()
    }

    private fun UserResponse.aUsuario() = Usuario(
        id = id,
        email = email,
        nombre = perfiles.nombre(email),
        telefono = perfiles.telefono(email),
        ciudad = perfiles.ciudad(email),
        rol = perfiles.rol(email),
        esAdmin = roles.orEmpty().contains("ROLE_ADMIN")
    )

    fun isLoggedIn(): Boolean = session.isLoggedIn
}
