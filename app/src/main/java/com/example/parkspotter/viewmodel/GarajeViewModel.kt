package com.example.parkspotter.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parkspotter.data.remote.ApiClient
import com.example.parkspotter.data.repository.GarajeRepository
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Intervalo de refresco del mapa en tiempo real (Sprint 3). */
private const val INTERVALO_ACTUALIZACION_MS = 15_000L

data class FiltrosBusqueda(
    val texto: String = "",
    val radioKm: Double? = 5.0,
    val precioMax: Int? = null,
    val soloDisponibles: Boolean = false
)

class GarajeViewModel(private val repository: GarajeRepository) : ViewModel() {

    private val _garajes = MutableStateFlow<List<Garaje>>(emptyList())
    val garajes: StateFlow<List<Garaje>> = _garajes

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _filtros = MutableStateFlow(FiltrosBusqueda())
    val filtros: StateFlow<FiltrosBusqueda> = _filtros

    private var ubicacionActual: Pair<Double, Double>? = null
    private var pollingJob: Job? = null

    /** Búsqueda puntual de garajes activos cerca de [lat]/[lng] con los filtros vigentes. */
    fun buscarGarajes(lat: Double? = null, lng: Double? = null, mostrarCargando: Boolean = true) {
        if (lat != null && lng != null) ubicacionActual = lat to lng
        val (ubicLat, ubicLng) = ubicacionActual ?: (lat to lng)
        val f = _filtros.value
        viewModelScope.launch {
            if (mostrarCargando) _loading.value = true
            _error.value = null
            val result = repository.buscar(
                lat = ubicLat,
                lng = ubicLng,
                radioKm = if (ubicLat != null) f.radioKm else null,
                precioMax = f.precioMax,
                soloDisponibles = f.soloDisponibles.takeIf { it },
                texto = f.texto
            )
            when (result) {
                is Resource.Success -> _garajes.value = result.data
                is Resource.Error -> _error.value = result.message
                Resource.Loading -> Unit
            }
            if (mostrarCargando) _loading.value = false
        }
    }

    fun actualizarFiltros(nuevos: FiltrosBusqueda) {
        _filtros.value = nuevos
        buscarGarajes(mostrarCargando = false)
    }

    /** Sprint 3 — mapa "en tiempo real": refresca la búsqueda cada 15s mientras la pantalla esté visible. */
    fun iniciarActualizacionEnVivo() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(INTERVALO_ACTUALIZACION_MS)
                buscarGarajes(mostrarCargando = false)
            }
        }
    }

    fun detenerActualizacionEnVivo() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        detenerActualizacionEnVivo()
        super.onCleared()
    }

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
