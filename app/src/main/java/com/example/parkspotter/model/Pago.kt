package com.example.parkspotter.model

import java.time.ZonedDateTime

/** Estado del pago de una reserva (Sprint 5). */
enum class EstadoPago(val etiqueta: String) {
    PENDING("Pago en proceso"),
    APPROVED("Pagada"),
    DECLINED("Pago rechazado"),
    REFUNDED("Reembolsada");

    companion object {
        fun de(valor: String?): EstadoPago? = entries.firstOrNull { it.name == valor }
    }
}

/** Pago y, si fue aprobado, los datos de su comprobante. */
data class Pago(
    val id: String,
    val referencia: String,
    val reservaId: String,
    val codigoReserva: String,
    val garajeNombre: String,
    val garajeDireccion: String,
    val placa: String?,
    val inicio: ZonedDateTime?,
    val fin: ZonedDateTime?,
    val monto: Int,
    val estado: EstadoPago,
    val franquicia: String,
    val ultimos4: String,
    val transaccion: String?,
    val comprobante: String?,
    val motivoRechazo: String?,
    val creadoEn: ZonedDateTime?,
    val aprobadoEn: ZonedDateTime?,
    val reembolsadoEn: ZonedDateTime?
)

data class Notificacion(
    val id: String,
    val tipo: String,
    val titulo: String,
    val mensaje: String,
    val reservaId: String?,
    val leida: Boolean,
    val creadaEn: ZonedDateTime?
)
