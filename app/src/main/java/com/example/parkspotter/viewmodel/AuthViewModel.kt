package com.example.parkspotter.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parkspotter.data.remote.ApiClient
import com.example.parkspotter.data.repository.AuthRepository
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.model.Usuario
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje

    private val _perfil = MutableStateFlow<Usuario?>(null)
    val perfil: StateFlow<Usuario?> = _perfil

    fun login(email: String, password: String, onResult: (RolUsuario) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.login(email, password)) {
                is Resource.Success -> {
                    val rol = repository.sincronizarRol()
                    _loading.value = false
                    onResult(rol)
                }
                is Resource.Error -> {
                    _loading.value = false
                    _error.value = result.message
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun register(
        email: String,
        password: String,
        rol: RolUsuario,
        nombre: String,
        telefono: String?,
        ciudad: String?,
        onResult: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.register(email, password, rol, nombre, telefono, ciudad)) {
                is Resource.Success -> {
                    _loading.value = false
                    onResult()
                }
                is Resource.Error -> {
                    _loading.value = false
                    _error.value = result.message
                }
                Resource.Loading -> Unit
            }
        }
    }

    // ── Sprint 1: gestionar perfil ──
    fun cargarPerfil() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val r = repository.obtenerPerfil()) {
                is Resource.Success -> _perfil.value = r.data
                is Resource.Error -> _error.value = r.message
                Resource.Loading -> Unit
            }
            _loading.value = false
        }
    }

    fun guardarPerfil(nombre: String, telefono: String, ciudad: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _mensaje.value = null
            when (val r = repository.actualizarPerfil(nombre, telefono.ifBlank { null }, ciudad.ifBlank { null })) {
                is Resource.Success -> {
                    _perfil.value = r.data
                    _mensaje.value = "Perfil actualizado"
                }
                is Resource.Error -> _error.value = r.message
                Resource.Loading -> Unit
            }
            _loading.value = false
        }
    }

    fun cambiarPassword(actual: String, nueva: String, onOk: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _mensaje.value = null
            when (val r = repository.cambiarPassword(actual, nueva)) {
                is Resource.Success -> {
                    _mensaje.value = "Contraseña actualizada"
                    onOk()
                }
                is Resource.Error -> _error.value = r.message
                Resource.Loading -> Unit
            }
            _loading.value = false
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onDone()
        }
    }

    fun limpiarError() { _error.value = null }
    fun mostrarError(msg: String) { _error.value = msg }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val session = SessionManager(appContext)
                    val api = ApiClient.createAuthApi(appContext)
                    return AuthViewModel(AuthRepository(api, session)) as T
                }
            }
    }
}
