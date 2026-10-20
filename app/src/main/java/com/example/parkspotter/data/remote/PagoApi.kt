package com.example.parkspotter.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Sprint 5 — Pagos (backend: docs/API-Sprint-05-Pagos.md). */
interface PagoApi {

    /** Con la misma [claveIdempotencia] el backend devuelve el mismo pago: reintentar no cobra dos veces. */
    @POST("payments")
    suspend fun pagar(
        @Header("Idempotency-Key") claveIdempotencia: String,
        @Body request: PaymentRequest
    ): Response<PaymentResponse>

    @GET("payments/{id}")
    suspend fun obtener(@Path("id") id: String): Response<PaymentResponse>

    @GET("payments/reservation/{reservationId}")
    suspend fun ultimoDeReserva(@Path("reservationId") reservaId: String): Response<PaymentResponse>
}

/** Sprint 5 — Bandeja de notificaciones. */
interface NotificacionApi {

    @GET("notifications")
    suspend fun listar(@Query("page") pagina: Int = 0, @Query("size") tamano: Int = 50): Response<PageResponse<NotificationResponse>>

    @PATCH("notifications/{id}/read")
    suspend fun marcarLeida(@Path("id") id: String): Response<NotificationResponse>

    @PATCH("notifications/read-all")
    suspend fun marcarTodas(): Response<Map<String, Int>>
}

/** Solo el token de la pasarela, la franquicia y los últimos 4 dígitos: el número nunca sale del celular. */
data class PaymentRequest(
    val reservationId: String,
    val cardToken: String,
    val cardBrand: String,
    val cardLast4: String
)

/** status = PENDING | APPROVED | DECLINED | REFUNDED */
data class PaymentResponse(
    val id: String,
    val reference: String?,
    val reservationId: String,
    val reservationCode: String?,
    val garageName: String?,
    val garageAddress: String?,
    val vehiclePlate: String?,
    val startTime: String?,
    val endTime: String?,
    val amount: Double?,
    val status: String?,
    val cardBrand: String?,
    val cardLast4: String?,
    val gatewayTransactionId: String?,
    val receiptNumber: String?,
    val declineReason: String?,
    val createdAt: String?,
    val approvedAt: String?,
    val refundedAt: String?
)

data class NotificationResponse(
    val id: String,
    val type: String?,
    val title: String?,
    val message: String?,
    val reservationId: String?,
    val read: Boolean?,
    val createdAt: String?
)
