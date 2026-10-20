package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.AvisosMonitor
import com.example.parkspotter.data.repository.PagoRepository
import com.example.parkspotter.data.repository.ReservaRepository
import com.example.parkspotter.model.EstadoPago
import com.example.parkspotter.model.Pago
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.Tarjeta
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.UUID

data class EstadoPantallaPago(
    val reserva: Reserva? = null,
    val pago: Pago? = null,
    val cargando: Boolean = true,
    val numero: String = "",
    val vence: String = "",
    val cvv: String = "",
    val titular: String = "",
    val procesando: Boolean = false,
    val error: String? = null
)

/**
 * Sprint 5 — Pago de una reserva con tarjeta y su comprobante. El cobro es asíncrono: se crea el pago
 * (PENDING) y se consulta cada segundo hasta que la pasarela lo apruebe o rechace.
 */
class PagoViewModel(
    private val pagos: PagoRepository,
    private val reservas: ReservaRepository,
    private val avisos: AvisosMonitor,
    private val reloj: () -> ZonedDateTime
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoPantallaPago())
    val estado: StateFlow<EstadoPantallaPago> = _estado.asStateFlow()

    /** Se conserva entre reintentos por falla de red (no cobra dos veces); se renueva tras un rechazo. */
    private var claveIdempotencia: String = UUID.randomUUID().toString()

    fun ahora(): ZonedDateTime = reloj()

    fun cargarReserva(id: String) {
        if (_estado.value.reserva?.id == id) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val r = reservas.obtener(id)) {
                is Resource.Success -> _estado.update { it.copy(reserva = r.data, cargando = false) }
                is Resource.Error -> _estado.update { it.copy(cargando = false, error = r.message) }
                Resource.Loading -> Unit
            }
        }
    }

    /** Comprobante: último pago de la reserva. */
    fun cargarComprobante(reservaId: String) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val r = pagos.ultimoDeReserva(reservaId)) {
                is Resource.Success -> _estado.update { it.copy(pago = r.data, cargando = false) }
                is Resource.Error -> _estado.update {
                    it.copy(cargando = false, error = if (r.codigo == 404) "Esta reserva todavía no tiene pagos" else r.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun cambiarNumero(v: String) = _estado.update { it.copy(numero = Tarjeta.digitos(v).take(19), error = null) }
    fun cambiarVence(v: String) = _estado.update { it.copy(vence = v.filter { c -> c.isDigit() || c == '/' }.take(5), error = null) }
    fun cambiarCvv(v: String) = _estado.update { it.copy(cvv = Tarjeta.digitos(v).take(4), error = null) }
    fun cambiarTitular(v: String) = _estado.update { it.copy(titular = v.take(60), error = null) }

    fun pagar() {
        val s = _estado.value
        val reserva = s.reserva ?: return
        if (s.procesando) return
        Tarjeta.error(s.numero, s.vence, s.cvv, s.titular, YearMonth.from(reloj()))?.let { e ->
            _estado.update { it.copy(error = e) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(procesando = true, error = null) }
            when (val r = pagos.pagar(reserva.id, s.numero, claveIdempotencia)) {
                is Resource.Success -> esperarResultado(r.data)
                is Resource.Error -> _estado.update {
                    it.copy(procesando = false, error = if (r.codigo == 404) "Los pagos en la app no están disponibles en este servidor" else r.message)
                }
                Resource.Loading -> Unit
            }
        }
    }

    private suspend fun esperarResultado(creado: Pago) {
        var pago = creado
        repeat(INTENTOS) {
            if (pago.estado != EstadoPago.PENDING) return@repeat
            delay(1_000)
            (pagos.obtener(pago.id) as? Resource.Success)?.let { pago = it.data }
        }
        when (pago.estado) {
            EstadoPago.APPROVED, EstadoPago.REFUNDED -> _estado.update { it.copy(procesando = false, pago = pago) }
            EstadoPago.DECLINED -> {
                claveIdempotencia = UUID.randomUUID().toString()
                _estado.update {
                    it.copy(procesando = false, error = (pago.motivoRechazo ?: "El pago fue rechazado") + ". Intenta con otra tarjeta.")
                }
            }
            EstadoPago.PENDING -> _estado.update {
                it.copy(procesando = false, error = "El pago sigue en proceso. Revisa Mis reservas en unos minutos.")
            }
        }
        avisos.revisar()
    }

    companion object {
        private const val INTENTOS = 30

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PagoViewModel(AppContainer.pagoRepository, AppContainer.reservaRepository, AppContainer.avisos, AppContainer.reloj)
            }
        }
    }
}
