package com.example.parkspotter.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parkspotter.data.remote.ApiClient
import com.example.parkspotter.data.repository.AuthRepository
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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

    fun register(email: String, password: String, rol: RolUsuario, onResult: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.register(email, password, rol)) {
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

    fun limpiarError() { _error.value = null }

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
