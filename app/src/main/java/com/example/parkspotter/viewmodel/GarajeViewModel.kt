package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.repository.GarajeRepository
import com.example.parkspotter.model.EstadoGaraje
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Sprint 2 — Gestión de garajes del propietario (Mis garajes, publicar, editar). */
class GarajeViewModel(private val repository: GarajeRepository) : ViewModel() {

    private val _garajes = MutableStateFlow<List<Garaje>>(emptyList())
    val garajes: StateFlow<List<Garaje>> = _garajes

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** Ids con una acción en curso (cupos, publicar): evita dobles toques. */
    private val _ocupados = MutableStateFlow<Set<String>>(emptySet())
    val ocupados: StateFlow<Set<String>> = _ocupados

    private val _garajeEditando = MutableStateFlow<Garaje?>(null)
    val garajeEditando: StateFlow<Garaje?> = _garajeEditando

    fun cargarMisGarajes() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.listarMios()) {
                is Resource.Success -> _garajes.value = result.data.sortedBy { it.nombre.lowercase() }
                is Resource.Error -> _error.value = result.message
                Resource.Loading -> Unit
            }
            _loading.value = false
        }
    }

    /** Carga un garaje para editarlo en RegistrarGarajeScreen. */
    fun cargarGaraje(id: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.obtener(id)) {
                is Resource.Success -> _garajeEditando.value = result.data
                is Resource.Error -> _error.value = result.message
                Resource.Loading -> Unit
            }
            _loading.value = false
        }
    }

    /** Crea (id vacío) o actualiza (id existente) un garaje. */
    fun guardarGaraje(garaje: Garaje, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val result = if (garaje.id.isBlank()) repository.crear(garaje)
                         else repository.actualizar(garaje.id, garaje)
            _loading.value = false
            when (result) {
                is Resource.Success -> onResult(true)
                is Resource.Error -> {
                    _error.value = result.message
                    onResult(false)
                }
                Resource.Loading -> Unit
            }
        }
    }

    /** Publica u oculta el garaje (optimista: revierte si el backend falla). */
    fun cambiarDisponibilidad(id: String, activo: Boolean) {
        val anterior = _garajes.value
        _garajes.value = anterior.map {
            if (it.id == id) it.copy(estado = if (activo) EstadoGaraje.ACTIVE
                                             else EstadoGaraje.INACTIVE)
            else it
        }
        conBloqueo(id) {
            when (val result = repository.cambiarActivo(id, activo)) {
                is Resource.Success -> reemplazar(result.data)
                is Resource.Error -> {
                    _garajes.value = anterior
                    _error.value = result.message
                }
                Resource.Loading -> Unit
            }
        }
    }

    /**
     * Ocupa (−1) o libera (+1) una plaza. Los conductores lo ven al instante en el mapa
     * porque el backend publica el cambio por el stream en tiempo real.
     */
    fun ajustarCupo(id: String, liberar: Boolean) {
        conBloqueo(id) {
            when (val r = repository.ajustarCupo(id, liberar)) {
                is Resource.Success -> repository.obtener(id).let { if (it is Resource.Success) reemplazar(it.data) }
                is Resource.Error -> _error.value = r.message
                Resource.Loading -> Unit
            }
        }
    }

    fun eliminarGaraje(id: String) {
        val anterior = _garajes.value
        _garajes.value = anterior.filterNot { it.id == id }
        viewModelScope.launch {
            val result = repository.eliminar(id)
            if (result is Resource.Error) {
                _garajes.value = anterior
                _error.value = result.message
            }
        }
    }

    private fun reemplazar(garaje: Garaje) {
        _garajes.value = _garajes.value.map { if (it.id == garaje.id) garaje.copy(fotos = it.fotos) else it }
    }

    private fun conBloqueo(id: String, accion: suspend () -> Unit) {
        if (id in _ocupados.value) return
        _ocupados.value = _ocupados.value + id
        viewModelScope.launch {
            try { accion() } finally { _ocupados.value = _ocupados.value - id }
        }
    }

    fun limpiarError() { _error.value = null }
    fun mostrarError(msg: String) { _error.value = msg }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { GarajeViewModel(AppContainer.garajeRepository) }
        }
    }
}
