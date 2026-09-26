package com.example.parkspotter.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parkspotter.data.remote.ApiClient
import com.example.parkspotter.data.repository.GarajeRepository
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GarajeViewModel(private val repository: GarajeRepository) : ViewModel() {

    private val _garajes = MutableStateFlow<List<Garaje>>(emptyList())
    val garajes: StateFlow<List<Garaje>> = _garajes

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun cargarMisGarajes() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.listarMios()) {
                is Resource.Success -> _garajes.value = result.data
                is Resource.Error -> _error.value = result.message
                Resource.Loading -> Unit
            }
            _loading.value = false
        }
    }

    fun publicarGaraje(garaje: Garaje, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.crear(garaje)) {
                is Resource.Success -> {
                    _garajes.value = _garajes.value + result.data
                    _loading.value = false
                    onResult(true)
                }
                is Resource.Error -> {
                    _error.value = result.message
                    _loading.value = false
                    onResult(false)
                }
                Resource.Loading -> Unit
            }
        }
    }

    /** Actualiza el switch al instante (optimista) y revierte si el backend falla. */
    fun cambiarDisponibilidad(id: String, activo: Boolean) {
        val anterior = _garajes.value
        _garajes.value = anterior.map { if (it.id == id) it.copy(activo = activo) else it }
        viewModelScope.launch {
            val result = repository.actualizarDisponibilidad(id, activo)
            if (result is Resource.Error) {
                _garajes.value = anterior
                _error.value = result.message
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val api = ApiClient.createGarajeApi(context.applicationContext)
                    return GarajeViewModel(GarajeRepository(api)) as T
                }
            }
    }
}
