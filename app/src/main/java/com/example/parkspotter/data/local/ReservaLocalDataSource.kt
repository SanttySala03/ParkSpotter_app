package com.example.parkspotter.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.parkspotter.data.remote.ReservationResponse
import com.example.parkspotter.data.remote.aIso
import com.example.parkspotter.model.EstadoReserva
import com.example.parkspotter.model.SolicitudReserva
import com.example.parkspotter.util.ReglasReserva
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Respaldo local del Sprint 4 mientras el backend no publique /api/v1/reservations.
 * Guarda las reservas del usuario en el dispositivo con el MISMO formato JSON del contrato
 * propuesto (ReservationResponse), así el resto de la app no distingue entre modo local y remoto.
 *
 * Limitación conocida: en modo local la reserva no descuenta cupos del garaje en el backend.
 */
class ReservaLocalDataSource(context: Context, private val gson: Gson = Gson()) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("parkspotter_reservas_local", Context.MODE_PRIVATE)

    private val tipoLista = object : TypeToken<List<ReservationResponse>>() {}.type

    private fun clave(email: String) = "reservas::${email.trim().lowercase()}"

    @Synchronized
    fun listar(email: String): List<ReservationResponse> =
        prefs.getString(clave(email), null)
            ?.let { runCatching { gson.fromJson<List<ReservationResponse>>(it, tipoLista) }.getOrNull() }
            .orEmpty()

    @Synchronized
    private fun guardar(email: String, reservas: List<ReservationResponse>) {
        prefs.edit().putString(clave(email), gson.toJson(reservas)).apply()
    }

    /** En modo local la reserva se confirma de inmediato (no hay operador que la apruebe). */
    @Synchronized
    fun crear(email: String, s: SolicitudReserva, ahora: ZonedDateTime): ReservationResponse {
        val nueva = ReservationResponse(
            id = UUID.randomUUID().toString(),
            code = "PS-" + UUID.randomUUID().toString().take(6).uppercase(),
            garageId = s.garaje.id,
            garageName = s.garaje.nombre,
            garageAddress = s.garaje.direccion,
            spotId = null,
            spotCode = null,
            vehicleType = s.tipoVehiculo.name,
            vehiclePlate = ReglasReserva.normalizarPlaca(s.placa).ifEmpty { null },
            startTime = s.inicio.aIso(),
            endTime = s.fin.aIso(),
            status = EstadoReserva.CONFIRMED.name,
            pricePerHour = s.garaje.precioPorHora.toDouble(),
            totalPrice = ReglasReserva.costo(s.garaje.precioPorHora, s.inicio, s.fin).toDouble(),
            createdAt = ahora.aIso(),
            cancelledAt = null
        )
        guardar(email, listar(email) + nueva)
        return nueva
    }

    @Synchronized
    fun cancelar(email: String, id: String, ahora: ZonedDateTime): ReservationResponse? {
        var cancelada: ReservationResponse? = null
        val actualizadas = listar(email).map {
            if (it.id == id) it.copy(status = EstadoReserva.CANCELLED.name, cancelledAt = ahora.aIso())
                .also { c -> cancelada = c }
            else it
        }
        guardar(email, actualizadas)
        return cancelada
    }
}
