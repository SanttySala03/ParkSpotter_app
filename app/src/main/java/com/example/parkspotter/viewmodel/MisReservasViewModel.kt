package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.repository.ReservaRepository
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class EstadoMisReservas(
    val activas: List<Reserva> = emptyList(),
    val historial: List<Reserva> = emptyList(),
    val cargando: Boolean = false,
    val cancelando: String? = null,
    val error: String? = null,
    val mensaje: String? = null
)

/** Sprint 4 — Historial de reservas: próximas/en curso e historial, con cancelación. */
class MisReservasViewModel(
    private val repository: ReservaRepository,
    private val reloj: () -> ZonedDateTime
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoMisReservas())
    val estado: StateFlow<EstadoMisReservas> = _estado.asStateFlow()

    fun ahora(): ZonedDateTime = reloj()

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val r = repository.misReservas()) {
                is Resource.Success -> publicar(r.data)
                is Resource.Error -> _estado.update { it.copy(error = r.message) }
                Resource.Loading -> Unit
            }
            _estado.update { it.copy(cargando = false) }
        }
    }

    private fun publicar(todas: List<Reserva>) {
        val ahora = reloj()
        val (activas, pasadas) = todas.partition { it.esActiva(ahora) }
        _estado.update {
            it.copy(
                activas = activas.sortedBy { r -> r.inicio },
                historial = pasadas.sortedByDescending { r -> r.inicio }
            )
        }
    }

    fun cancelar(reserva: Reserva) {
        if (_estado.value.cancelando != null) return
        viewModelScope.launch {
            _estado.update { it.copy(cancelando = reserva.id, error = null) }
            when (val r = repository.cancelar(reserva.id)) {
                is Resource.Success -> {
                    _estado.update { it.copy(mensaje = "Reserva ${reserva.codigo} cancelada") }
                    (repository.misReservas() as? Resource.Success)?.let { publicar(it.data) }
                }
                is Resource.Error -> _estado.update { it.copy(error = r.message) }
                Resource.Loading -> Unit
            }
            _estado.update { it.copy(cancelando = null) }
        }
    }

    fun limpiarMensajes() = _estado.update { it.copy(error = null, mensaje = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { MisReservasViewModel(AppContainer.reservaRepository, AppContainer.reloj) }
        }
    }
}
