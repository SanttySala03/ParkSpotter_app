package com.example.parkspotter.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Sprint 4 — Reservas. Contrato PROPUESTO al backend (aún no existe en la rama develop):
 * ver docs/API-Sprint-04-Reservas-propuesta.md. Mientras el backend no lo publique,
 * ReservaRepository detecta el 404 y trabaja en modo local (ReservaLocalDataSource).
 */
interface ReservaApi {

    @POST("reservations")
    suspend fun crear(@Body request: ReservationRequest): Response<ReservationResponse>

    @GET("reservations/mine")
    suspend fun mias(
        @Query("status") estado: String? = null,
        @Query("page") pagina: Int = 0,
        @Query("size") tamano: Int = 100
    ): Response<PageResponse<ReservationResponse>>

    @GET("reservations/{id}")
    suspend fun obtener(@Path("id") id: String): Response<ReservationResponse>

    @PATCH("reservations/{id}/cancel")
    suspend fun cancelar(@Path("id") id: String): Response<ReservationResponse>
}

/** startTime / endTime en ISO-8601 con zona, p. ej. "2026-10-17T08:00:00-05:00". */
data class ReservationRequest(
    val garageId: String,
    val vehicleType: String,
    val vehiclePlate: String?,
    val startTime: String,
    val endTime: String
)

/** status = PENDING | CONFIRMED | CANCELLED | COMPLETED */
data class ReservationResponse(
    val id: String,
    val code: String?,
    val garageId: String,
    val garageName: String?,
    val garageAddress: String?,
    val spotId: String?,
    val spotCode: String?,
    val vehicleType: String?,
    val vehiclePlate: String?,
    val startTime: String,
    val endTime: String,
    val status: String?,
    val pricePerHour: Double?,
    val totalPrice: Double?,
    val createdAt: String?,
    val cancelledAt: String?
)
