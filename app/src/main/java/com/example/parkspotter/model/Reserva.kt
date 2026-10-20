package com.example.parkspotter.model

import java.time.Duration
import java.time.ZonedDateTime

/** Estados definidos para el Sprint 4: Pendiente, Confirmada, Cancelada, Completada. */
enum class EstadoReserva(val etiqueta: String) {
    PENDING("Pendiente"),
    CONFIRMED("Confirmada"),
    CANCELLED("Cancelada"),
    COMPLETED("Completada");

    companion object {
        fun de(valor: String?): EstadoReserva = entries.firstOrNull { it.name == valor } ?: PENDING
    }
}

data class Reserva(
    val id: String,
    val codigo: String,
    val garajeId: String,
    val garajeNombre: String,
    val garajeDireccion: String,
    val plazaCodigo: String?,
    val tipoVehiculo: TipoVehiculo,
    val placa: String?,
    val inicio: ZonedDateTime,
    val fin: ZonedDateTime,
    val estado: EstadoReserva,
    val precioPorHora: Int,
    val total: Int,
    val creadaEn: ZonedDateTime?,
    val canceladaEn: ZonedDateTime?,
    val estadoPago: EstadoPago? = null
) {
    val duracion: Duration get() = Duration.between(inicio, fin)

    /** Activa = aún puede usarse (pendiente o confirmada y no ha terminado). */
    fun esActiva(ahora: ZonedDateTime): Boolean =
        (estado == EstadoReserva.PENDING || estado == EstadoReserva.CONFIRMED) && fin.isAfter(ahora)

    fun enCurso(ahora: ZonedDateTime): Boolean = esActiva(ahora) && !inicio.isAfter(ahora)

    /** Se puede pagar en la app mientras esté vigente y no tenga un pago aprobado o en proceso. */
    fun pagable(ahora: ZonedDateTime): Boolean =
        esActiva(ahora) && estadoPago != EstadoPago.APPROVED && estadoPago != EstadoPago.PENDING

    /** Solo se cancela antes de que empiece. */
    fun cancelable(ahora: ZonedDateTime): Boolean = esActiva(ahora) && inicio.isAfter(ahora)

    /** Cambio automático de estado: una reserva confirmada cuyo horario ya terminó se ve como completada. */
    fun estadoVisible(ahora: ZonedDateTime): EstadoReserva =
        if (estado == EstadoReserva.CONFIRMED && !fin.isAfter(ahora))
            EstadoReserva.COMPLETED
        else estado
}

/** Lo que el conductor eligió en el flujo de reserva, antes de confirmar. */
data class SolicitudReserva(
    val garaje: Garaje,
    val tipoVehiculo: TipoVehiculo,
    val placa: String,
    val inicio: ZonedDateTime,
    val fin: ZonedDateTime
)
