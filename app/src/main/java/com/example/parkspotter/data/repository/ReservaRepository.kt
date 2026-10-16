package com.example.parkspotter.data.repository

import com.example.parkspotter.data.local.PerfilLocalStore
import com.example.parkspotter.data.local.ReservaLocalDataSource
import com.example.parkspotter.data.remote.ReservaApi
import com.example.parkspotter.data.remote.ReservationRequest
import com.example.parkspotter.data.remote.aIso
import com.example.parkspotter.data.remote.aModelo
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.model.SolicitudReserva
import com.example.parkspotter.util.ReglasReserva
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.safeApiCall
import java.time.ZonedDateTime

enum class ModoReservas {
    /** Aún no se sabe si el backend tiene el módulo de reservas. */
    DESCONOCIDO,
    /** El backend respondió /reservations: es la fuente de verdad. */
    REMOTO,
    /** El backend no tiene /reservations (404): se guardan en el dispositivo. */
    LOCAL
}

/**
 * Sprint 4 — Reservas. Usa el backend si publica /api/v1/reservations (contrato en
 * docs/API-Sprint-04-Reservas-propuesta.md) y, si responde 404, cae a ReservaLocalDataSource
 * para que el flujo completo (reservar, confirmar, cancelar, historial) funcione en la demo.
 */
class ReservaRepository(
    private val api: ReservaApi,
    private val local: ReservaLocalDataSource,
    private val session: SessionManager,
    private val perfiles: PerfilLocalStore,
    private val reloj: () -> ZonedDateTime
) {

    var modo: ModoReservas = modoDetectado
        private set(value) {
            field = value
            modoDetectado = value
        }

    private val email: String get() = session.email.orEmpty()

    suspend fun misReservas(): Resource<List<Reserva>> {
        if (modo != ModoReservas.LOCAL) {
            when (val r = safeApiCall { api.mias() }) {
                is Resource.Success -> {
                    modo = ModoReservas.REMOTO
                    return Resource.Success(r.data.content.orEmpty().mapNotNull { it.aModelo() }.ordenadas())
                }
                is Resource.Error -> if (r.codigo == 404 || r.codigo == 405) modo = ModoReservas.LOCAL else return r
                Resource.Loading -> return Resource.Loading
            }
        }
        return Resource.Success(local.listar(email).mapNotNull { it.aModelo() }.ordenadas())
    }

    suspend fun crear(s: SolicitudReserva): Resource<Reserva> {
        if (modo == ModoReservas.DESCONOCIDO) misReservas()
        perfiles.guardarPlaca(email, s.placa)
        if (modo == ModoReservas.REMOTO) {
            val request = ReservationRequest(
                garageId = s.garaje.id,
                vehicleType = s.tipoVehiculo.name,
                vehiclePlate = ReglasReserva.normalizarPlaca(s.placa).ifEmpty { null },
                startTime = s.inicio.aIso(),
                endTime = s.fin.aIso()
            )
            return when (val r = safeApiCall { api.crear(request) }) {
                is Resource.Success -> r.data.aModelo()?.let { Resource.Success(it) }
                    ?: Resource.Error("El servidor devolvió una reserva incompleta")
                is Resource.Error -> r
                Resource.Loading -> Resource.Loading
            }
        }
        val creada = local.crear(email, s, reloj()).aModelo()
            ?: return Resource.Error("No se pudo guardar la reserva")
        return Resource.Success(creada)
    }

    suspend fun cancelar(id: String): Resource<Reserva> {
        if (modo == ModoReservas.REMOTO) {
            return when (val r = safeApiCall { api.cancelar(id) }) {
                is Resource.Success -> r.data.aModelo()?.let { Resource.Success(it) }
                    ?: Resource.Error("El servidor devolvió una reserva incompleta")
                is Resource.Error -> r
                Resource.Loading -> Resource.Loading
            }
        }
        return local.cancelar(email, id, reloj())?.aModelo()?.let { Resource.Success(it) }
            ?: Resource.Error("No encontramos esa reserva")
    }

    fun ultimaPlaca(): String = perfiles.ultimaPlaca(email)

    /** Próximas primero (por inicio ascendente); el historial se ordena en la UI. */
    private fun List<Reserva>.ordenadas() = sortedBy { it.inicio }

    private companion object {
        /** Se recuerda mientras viva el proceso para no volver a sondear en cada pantalla. */
        @Volatile var modoDetectado: ModoReservas = ModoReservas.DESCONOCIDO
    }
}
