package com.example.parkspotter.data.repository

import com.example.parkspotter.data.remote.NotificacionApi
import com.example.parkspotter.data.remote.PagoApi
import com.example.parkspotter.data.remote.PaymentRequest
import com.example.parkspotter.data.remote.aModelo
import com.example.parkspotter.model.Notificacion
import com.example.parkspotter.model.Pago
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.Tarjeta
import com.example.parkspotter.util.map
import com.example.parkspotter.util.safeApiCall

/** Sprint 5 — Pagos con tarjeta tokenizada y comprobante. */
class PagoRepository(private val api: PagoApi) {

    suspend fun pagar(reservaId: String, numeroTarjeta: String, claveIdempotencia: String): Resource<Pago> {
        val numero = Tarjeta.digitos(numeroTarjeta)
        val request = PaymentRequest(
            reservationId = reservaId,
            cardToken = Tarjeta.tokenizar(numero),
            cardBrand = Tarjeta.franquicia(numero).orEmpty(),
            cardLast4 = numero.takeLast(4)
        )
        return safeApiCall { api.pagar(claveIdempotencia, request) }.map { it.aModelo() }
    }

    suspend fun obtener(id: String): Resource<Pago> = safeApiCall { api.obtener(id) }.map { it.aModelo() }

    suspend fun ultimoDeReserva(reservaId: String): Resource<Pago> =
        safeApiCall { api.ultimoDeReserva(reservaId) }.map { it.aModelo() }
}

/** Sprint 5 — Bandeja de notificaciones. */
class NotificacionRepository(private val api: NotificacionApi) {

    suspend fun listar(): Resource<List<Notificacion>> =
        safeApiCall { api.listar() }.map { pagina -> pagina.content.orEmpty().map { it.aModelo() } }

    suspend fun marcarLeida(id: String): Resource<Unit> = safeApiCall { api.marcarLeida(id) }.map { }

    suspend fun marcarTodas(): Resource<Unit> = safeApiCall { api.marcarTodas() }.map { }
}
