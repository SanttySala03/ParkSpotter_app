package com.example.parkspotter.data.repository

import com.example.parkspotter.data.remote.AuthApi
import com.example.parkspotter.data.remote.LoginRequest
import com.example.parkspotter.data.remote.RegisterRequest
import com.example.parkspotter.data.remote.TokenPairResponse
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.safeApiCall

class AuthRepository(private val api: AuthApi, private val session: SessionManager) {

    suspend fun login(email: String, password: String): Resource<TokenPairResponse> {
        val result = safeApiCall { api.login(LoginRequest(email, password)) }
        if (result is Resource.Success) saveTokens(result.data)
        return result
    }

    suspend fun register(email: String, password: String, rol: RolUsuario): Resource<TokenPairResponse> {
        val result = safeApiCall { api.register(RegisterRequest(email, password, rol.name)) }
        if (result is Resource.Success) {
            saveTokens(result.data)
            session.rolLocal = rol
        }
        return result
    }

    /** Confirma contra el backend qué rol tiene realmente el usuario (tras login). */
    suspend fun sincronizarRol(): RolUsuario {
        val result = safeApiCall { api.me() }
        val rol = if (result is Resource.Success && result.data.roles.contains("ROLE_PROPIETARIO")) {
            RolUsuario.PROPIETARIO
        } else {
            RolUsuario.CONDUCTOR
        }
        session.rolLocal = rol
        return rol
    }

    private fun saveTokens(tokens: TokenPairResponse) {
        session.accessToken = tokens.accessToken
        session.refreshToken = tokens.refreshToken
    }

    fun isLoggedIn(): Boolean = session.isLoggedIn
    fun logoutLocal() = session.clear()
}
