package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.repository.GarajeRepository
import com.example.parkspotter.model.EstadoPlaza
import com.example.parkspotter.model.GarajeDetalle
import com.example.parkspotter.model.Plaza
import com.example.parkspotter.model.con
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoDetalle(
    val detalle: GarajeDetalle? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    /** Plaza a la que el propietario le está cambiando el estado. */
    val plazaEnCambio: String? = null
)

/**
 * Sprint 3 — Vista de detalle del garaje con disponibilidad por tipo de vehículo y mapa de
 * plazas, actualizado en vivo por el stream SSE del garaje. El propietario además puede
 * cambiar el estado de cada plaza (libre / ocupada / fuera de servicio).
 */
class DetalleGarajeViewModel(private val repository: GarajeRepository) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoDetalle())
    val estado: StateFlow<EstadoDetalle> = _estado.asStateFlow()

    private var garajeId: String? = null
    private var jobEnVivo: Job? = null
    private var jobRecarga: Job? = null

    fun cargar(id: String) {
        garajeId = id
        viewModelScope.launch {
            _estado.update { it.copy(cargando = it.detalle == null, error = null) }
            when (val r = repository.detalle(id)) {
                is Resource.Success -> _estado.update { it.copy(detalle = r.data, cargando = false) }
                is Resource.Error -> _estado.update {
                    it.copy(cargando = false, error = if (r.codigo == 404) "Este garaje ya no existe" else r.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun iniciarEnVivo() {
        val id = garajeId ?: return
        if (jobEnVivo?.isActive == true) return
        val stream = repository.disponibilidadEnVivo(id) ?: return
        jobEnVivo = viewModelScope.launch {
            stream.collect { evento ->
                _estado.update { s -> s.copy(detalle = s.detalle?.let { d -> d.copy(garaje = d.garaje.con(evento)) }) }
                // El evento trae contadores, no el estado de cada plaza: se recarga el detalle (agrupado).
                recargarPlazas()
            }
        }
    }

    fun detenerEnVivo() {
        jobEnVivo?.cancel()
        jobEnVivo = null
    }

    private fun recargarPlazas() {
        val id = garajeId ?: return
        jobRecarga?.cancel()
        jobRecarga = viewModelScope.launch {
            delay(600)
            (repository.detalle(id) as? Resource.Success)?.let { r -> _estado.update { it.copy(detalle = r.data) } }
        }
    }

    /** Propietario: cambia el estado de una plaza (el backend avisa a todos los conductores). */
    fun cambiarEstadoPlaza(plaza: Plaza, nuevo: EstadoPlaza) {
        val id = garajeId ?: return
        if (plaza.estado == nuevo || _estado.value.plazaEnCambio != null) return
        viewModelScope.launch {
            _estado.update { it.copy(plazaEnCambio = plaza.id) }
            when (val r = repository.cambiarEstadoPlaza(id, plaza.id, nuevo)) {
                is Resource.Success -> {
                    _estado.update { it.copy(mensaje = "Plaza ${plaza.codigo}: ${nuevo.etiqueta.lowercase()}") }
                    (repository.detalle(id) as? Resource.Success)?.let { d -> _estado.update { it.copy(detalle = d.data) } }
                }
                is Resource.Error -> _estado.update { it.copy(error = r.message) }
                Resource.Loading -> Unit
            }
            _estado.update { it.copy(plazaEnCambio = null) }
        }
    }

    fun limpiarMensajes() = _estado.update { it.copy(error = null, mensaje = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { DetalleGarajeViewModel(AppContainer.garajeRepository) }
        }
    }
}
