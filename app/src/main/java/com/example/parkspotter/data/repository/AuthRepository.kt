package com.example.parkspotter.data.repository

import com.example.parkspotter.data.remote.AuthApi
import com.example.parkspotter.data.remote.ChangePasswordRequest
import com.example.parkspotter.data.remote.LoginRequest
import com.example.parkspotter.data.remote.LogoutRequest
import com.example.parkspotter.data.remote.RegisterRequest
import com.example.parkspotter.data.remote.TokenPairResponse
import com.example.parkspotter.data.remote.UpdateProfileRequest
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.model.Usuario
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.safeApiCall

class AuthRepository(private val api: AuthApi, private val session: SessionManager) {

    suspend fun login(email: String, password: String): Resource<TokenPairResponse> {
        val result = safeApiCall { api.login(LoginRequest(email.trim(), password)) }
        if (result is Resource.Success) saveTokens(result.data)
        return result
    }

    suspend fun register(
        email: String,
        password: String,
        rol: RolUsuario,
        nombre: String,
        telefono: String?,
        ciudad: String?
    ): Resource<TokenPairResponse> {
        val result = safeApiCall {
            api.register(RegisterRequest(email.trim(), password, rol.name, nombre.trim(), telefono, ciudad))
        }
        if (result is Resource.Success) {
            saveTokens(result.data)
            session.rolLocal = rol
        }
        return result
    }

    /** Confirma contra el backend qué rol tiene realmente el usuario (tras login). */
    suspend fun sincronizarRol(): RolUsuario {
        val result = safeApiCall { api.me() }
        val rol = if (result is Resource.Success) result.data.toModel().rol else session.rolLocal
        session.rolLocal = rol
        return rol
    }

    // ── Sprint 1: gestionar perfil ──
    suspend fun obtenerPerfil(): Resource<Usuario> = when (val r = safeApiCall { api.me() }) {
        is Resource.Success -> Resource.Success(r.data.toModel())
        is Resource.Error -> Resource.Error(r.message)
        Resource.Loading -> Resource.Loading
    }

    suspend fun actualizarPerfil(nombre: String, telefono: String?, ciudad: String?): Resource<Usuario> =
        when (val r = safeApiCall {
            api.actualizarPerfil(UpdateProfileRequest(nombre.trim(), telefono?.trim(), ciudad?.trim()))
        }) {
            is Resource.Success -> Resource.Success(r.data.toModel())
            is Resource.Error -> Resource.Error(r.message)
            Resource.Loading -> Resource.Loading
        }

    suspend fun cambiarPassword(actual: String, nueva: String): Resource<Unit> =
        safeApiCall { api.cambiarPassword(ChangePasswordRequest(actual, nueva)) }

    /** Revoca el refresh token en el backend (si hay red) y borra la sesión local. */
    suspend fun logout() {
        session.refreshToken?.let { token -> safeApiCall { api.logout(LogoutRequest(token)) } }
        session.clear()
    }

    private fun saveTokens(tokens: TokenPairResponse) {
        session.accessToken = tokens.accessToken
        session.refreshToken = tokens.refreshToken
        tokens.email?.let { session.email = it }
    }

    private fun com.example.parkspotter.data.remote.UserResponse.toModel() = Usuario(
        id = id,
        nombre = nombre ?: "",
        email = email,
        telefono = telefono ?: "",
        ciudad = ciudad ?: "",
        rol = if (roles.contains("ROLE_PROPIETARIO")) RolUsuario.PROPIETARIO else RolUsuario.CONDUCTOR
    )

    fun isLoggedIn(): Boolean = session.isLoggedIn
    fun rolGuardado(): RolUsuario = session.rolLocal
}
