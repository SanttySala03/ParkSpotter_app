package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.repository.GarajeRepository
import com.example.parkspotter.data.repository.ReservaRepository
import com.example.parkspotter.model.GarajeDetalle
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.model.SolicitudReserva
import com.example.parkspotter.model.TipoVehiculo
import com.example.parkspotter.util.ReglasReserva
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Pasos del flujo (RNF: máximo 4 pasos desde el mapa: detalle → horario → confirmar → listo). */
enum class PasoReserva { HORARIO, CONFIRMAR, LISTO }

data class EstadoReservar(
    val detalle: GarajeDetalle? = null,
    val cargando: Boolean = true,
    val paso: PasoReserva = PasoReserva.HORARIO,
    val tipoVehiculo: TipoVehiculo = TipoVehiculo.CAR,
    val placa: String = "",
    val fecha: LocalDate? = null,
    val horaInicio: LocalTime? = null,
    val duracionHoras: Int = 2,
    val enviando: Boolean = false,
    val error: String? = null,
    val reserva: Reserva? = null
) {
    fun inicio(ahora: ZonedDateTime): ZonedDateTime? =
        if (fecha != null && horaInicio != null) fecha.atTime(horaInicio).atZone(ahora.zone) else null

    fun fin(ahora: ZonedDateTime): ZonedDateTime? = inicio(ahora)?.plusHours(duracionHoras.toLong())

    /** Tipos de vehículo que el garaje tiene (si no hay datos, al menos carro). */
    val tiposDisponibles: List<TipoVehiculo>
        get() = detalle?.porTipo?.filter { it.total > 0 }?.map { it.tipo }?.ifEmpty { null } ?: listOf(TipoVehiculo.CAR)

    val cuposDelTipo: Int?
        get() = detalle?.porTipo?.firstOrNull { it.tipo == tipoVehiculo }?.disponibles
}

/**
 * Sprint 4 — Flujo completo de reserva:
 * 1) elegir vehículo, fecha, hora y duración; 2) confirmar resumen y precio; 3) reserva lista.
 */
class ReservarViewModel(
    private val garajes: GarajeRepository,
    private val reservas: ReservaRepository,
    private val reloj: () -> ZonedDateTime
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoReservar())
    val estado: StateFlow<EstadoReservar> = _estado.asStateFlow()

    /** Reservas del usuario para validar solapamientos antes de enviar. */
    private var misReservas: List<Reserva> = emptyList()

    fun ahora(): ZonedDateTime = reloj()

    fun cargar(garajeId: String) {
        if (_estado.value.detalle?.garaje?.id == garajeId) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, placa = reservas.ultimaPlaca()) }
            (reservas.misReservas() as? Resource.Success)?.let { misReservas = it.data }
            when (val r = garajes.detalle(garajeId)) {
                is Resource.Success -> {
                    val sugerido = ReglasReserva.primerInicioPosible(r.data.garaje, reloj())
                    _estado.update { s ->
                        val tipos = r.data.porTipo.filter { it.total > 0 }.map { it.tipo }
                        s.copy(
                            detalle = r.data,
                            cargando = false,
                            tipoVehiculo = if (s.tipoVehiculo in tipos || tipos.isEmpty()) s.tipoVehiculo else tipos.first(),
                            fecha = sugerido.toLocalDate(),
                            horaInicio = sugerido.toLocalTime()
                        )
                    }
                }
                is Resource.Error -> _estado.update { it.copy(cargando = false, error = r.message) }
                Resource.Loading -> Unit
            }
        }
    }

    fun elegirTipo(tipo: TipoVehiculo) = _estado.update { it.copy(tipoVehiculo = tipo, error = null) }
    fun cambiarPlaca(placa: String) =
        _estado.update { it.copy(placa = ReglasReserva.normalizarPlaca(placa).take(6), error = null) }
    fun elegirFecha(fecha: LocalDate) = _estado.update { it.copy(fecha = fecha, error = null) }
    fun elegirHora(hora: LocalTime) = _estado.update { it.copy(horaInicio = hora, error = null) }
    fun elegirDuracion(horas: Int) = _estado.update { it.copy(duracionHoras = horas, error = null) }

    /** "Ahora": la próxima media hora disponible. */
    fun usarAhora() {
        val garaje = _estado.value.detalle?.garaje ?: return
        val sugerido = ReglasReserva.primerInicioPosible(garaje, reloj())
        _estado.update { it.copy(fecha = sugerido.toLocalDate(), horaInicio = sugerido.toLocalTime(), error = null) }
    }

    private fun solicitud(): SolicitudReserva? {
        val s = _estado.value
        val garaje = s.detalle?.garaje ?: return null
        val ahora = reloj()
        val inicio = s.inicio(ahora) ?: return null
        val fin = s.fin(ahora) ?: return null
        return SolicitudReserva(garaje, s.tipoVehiculo, s.placa, inicio, fin)
    }

    /** Paso 1 → 2: valida en la app y muestra el resumen para confirmar. */
    fun continuar() {
        val s = solicitud() ?: run { _estado.update { it.copy(error = "Elige fecha y hora de inicio") }; return }
        val error = ReglasReserva.validar(s, reloj(), misReservas, _estado.value.cuposDelTipo)
        _estado.update { if (error != null) it.copy(error = error) else it.copy(paso = PasoReserva.CONFIRMAR, error = null) }
    }

    fun volver() = _estado.update { it.copy(paso = PasoReserva.HORARIO, error = null) }

    /** Paso 2 → 3: crea la reserva (backend o modo local). */
    fun confirmar() {
        val s = solicitud() ?: return
        if (_estado.value.enviando) return
        viewModelScope.launch {
            _estado.update { it.copy(enviando = true, error = null) }
            // Revalida justo antes de enviar: los cupos pudieron cambiar mientras el usuario leía el resumen.
            (garajes.disponibilidad(s.garaje.id) as? Resource.Success)?.data?.let { d ->
                if (d.disponibles <= 0) {
                    _estado.update { it.copy(enviando = false, paso = PasoReserva.HORARIO,
                        error = "Se acaban de ocupar todas las plazas de este garaje") }
                    return@launch
                }
            }
            when (val r = reservas.crear(s)) {
                is Resource.Success -> _estado.update {
                    it.copy(enviando = false, reserva = r.data, paso = PasoReserva.LISTO)
                }
                is Resource.Error -> _estado.update {
                    it.copy(enviando = false, error = if (r.codigo == 409) "Ya no hay cupo en ese horario: elige otro" else r.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReservarViewModel(AppContainer.garajeRepository, AppContainer.reservaRepository, AppContainer.reloj)
            }
        }
    }
}
