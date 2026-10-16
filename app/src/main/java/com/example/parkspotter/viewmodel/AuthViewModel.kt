package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.repository.AuthRepository
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
                    _loading.value = false
                    onResult(repository.rolGuardado())
                }
                is Resource.Error -> {
                    _loading.value = false
                    _error.value = if (result.codigo == 401) "Correo o contraseña incorrectos" else result.message
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
                    _error.value = if (result.codigo == 409) "Ese correo ya tiene una cuenta. Inicia sesión." else result.message
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

    /** Nombre, teléfono y ciudad se guardan en el dispositivo (el backend aún no los tiene). */
    fun guardarPerfil(nombre: String, telefono: String, ciudad: String) {
        _error.value = null
        repository.guardarDatosPerfil(nombre, telefono, ciudad)?.let {
            _perfil.value = it.copy(esAdmin = _perfil.value?.esAdmin == true)
            _mensaje.value = "Perfil actualizado"
        } ?: run { _error.value = "Inicia sesión de nuevo para editar tu perfil" }
    }

    fun cambiarRol(rol: RolUsuario) {
        repository.cambiarRol(rol)
        _perfil.value = _perfil.value?.copy(rol = rol)
        _mensaje.value = if (rol == RolUsuario.PROPIETARIO) "Modo propietario activado" else "Modo conductor activado"
    }

    fun rolActual(): RolUsuario = repository.rolGuardado()

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
    fun limpiarMensaje() { _mensaje.value = null }
    fun mostrarError(msg: String) { _error.value = msg }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(AppContainer.authRepository) }
        }
    }
}
