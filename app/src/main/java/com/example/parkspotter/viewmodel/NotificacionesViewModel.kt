package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.AvisosMonitor
import com.example.parkspotter.data.repository.NotificacionRepository
import com.example.parkspotter.model.Notificacion
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class EstadoNotificaciones(
    val lista: List<Notificacion> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

/** Sprint 5 — Bandeja de avisos (pagos, reembolsos y recordatorios de reserva). */
class NotificacionesViewModel(
    private val repository: NotificacionRepository,
    private val avisos: AvisosMonitor,
    private val reloj: () -> ZonedDateTime
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoNotificaciones())
    val estado: StateFlow<EstadoNotificaciones> = _estado.asStateFlow()

    fun ahora(): ZonedDateTime = reloj()

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val r = repository.listar()) {
                is Resource.Success -> _estado.update { it.copy(lista = r.data) }
                is Resource.Error -> _estado.update { it.copy(error = r.message) }
                Resource.Loading -> Unit
            }
            _estado.update { it.copy(cargando = false) }
        }
    }

    fun marcarLeida(n: Notificacion) {
        if (n.leida) return
        _estado.update { s -> s.copy(lista = s.lista.map { if (it.id == n.id) it.copy(leida = true) else it }) }
        avisos.descontar()
        viewModelScope.launch { repository.marcarLeida(n.id) }
    }

    fun marcarTodas() {
        _estado.update { s -> s.copy(lista = s.lista.map { it.copy(leida = true) }) }
        avisos.ponerEnCero()
        viewModelScope.launch { repository.marcarTodas() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { NotificacionesViewModel(AppContainer.notificacionRepository, AppContainer.avisos, AppContainer.reloj) }
        }
    }
}
