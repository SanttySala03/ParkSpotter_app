package com.example.parkspotter.util

import com.example.parkspotter.model.Garaje
import com.example.parkspotter.model.Reserva
import com.example.parkspotter.model.SolicitudReserva
import com.example.parkspotter.model.TipoVehiculo
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlin.math.ceil

/**
 * Reglas de negocio del flujo de reserva (Sprint 4), validadas en la app antes de enviar
 * para dar feedback inmediato. El backend debe volver a validarlas (fuente de verdad).
 */
object ReglasReserva {

    val DURACION_MINIMA: Duration = Duration.ofHours(1)
    val DURACION_MAXIMA: Duration = Duration.ofHours(12)
    val ANTICIPACION_MAXIMA: Duration = Duration.ofDays(7)
    /** Margen para "reservar ahora" mientras el usuario completa el formulario. */
    private val TOLERANCIA_PASADO: Duration = Duration.ofMinutes(10)

    /** Placas colombianas: carro ABC123, moto ABC12D. */
    private val REGEX_PLACA = Regex("^[A-Z]{3}[0-9]{2}[0-9A-Z]$")

    fun normalizarPlaca(placa: String): String = placa.uppercase().filter { it.isLetterOrDigit() }

    fun placaObligatoria(tipo: TipoVehiculo): Boolean = tipo != TipoVehiculo.BICYCLE

    fun errorPlaca(placa: String, tipo: TipoVehiculo): String? {
        val p = normalizarPlaca(placa)
        return when {
            !placaObligatoria(tipo) && p.isEmpty() -> null
            p.isEmpty() -> "Ingresa la placa del vehículo"
            !REGEX_PLACA.matches(p) -> "Placa inválida. Ej: ABC123 (carro) o ABC12D (moto)"
            else -> null
        }
    }

    /** Horas cobradas: cada fracción de hora se cobra completa. */
    fun horasCobradas(inicio: ZonedDateTime, fin: ZonedDateTime): Int =
        ceil(Duration.between(inicio, fin).toMinutes() / 60.0).toInt().coerceAtLeast(0)

    fun costo(precioPorHora: Int, inicio: ZonedDateTime, fin: ZonedDateTime): Int =
        precioPorHora * horasCobradas(inicio, fin)

    /** ¿El intervalo cabe completo dentro del horario del garaje? Soporta horarios nocturnos (22:00–06:00). */
    fun dentroDeHorario(garaje: Garaje, inicio: ZonedDateTime, fin: ZonedDateTime): Boolean {
        if (garaje.abierto24h) return true
        val abre: LocalTime = garaje.horaApertura ?: return true
        val cierra: LocalTime = garaje.horaCierre ?: return true
        val dia = inicio.toLocalDate()
        return listOf(dia.minusDays(1), dia).any { d ->
            val desde = d.atTime(abre).atZone(inicio.zone)
            val hasta = (if (cierra.isAfter(abre)) d else d.plusDays(1)).atTime(cierra).atZone(inicio.zone)
            !inicio.isBefore(desde) && !fin.isAfter(hasta)
        }
    }

    /**
     * Hora de inicio sugerida: la próxima media hora; si el garaje está cerrado a esa hora,
     * su siguiente apertura (hoy o mañana).
     */
    fun primerInicioPosible(garaje: Garaje, ahora: ZonedDateTime): ZonedDateTime {
        val base = ahora.withSecond(0).withNano(0)
        val redondeado = base.plusMinutes(((30 - base.minute % 30) % 30).toLong())
        if (dentroDeHorario(garaje, redondeado, redondeado.plus(DURACION_MINIMA))) return redondeado
        val abre = garaje.horaApertura ?: return redondeado
        val hoy = redondeado.toLocalDate().atTime(abre).atZone(redondeado.zone)
        return if (hoy.isAfter(redondeado)) hoy else hoy.plusDays(1)
    }

    fun seSolapan(aIni: ZonedDateTime, aFin: ZonedDateTime, bIni: ZonedDateTime, bFin: ZonedDateTime): Boolean =
        aIni.isBefore(bFin) && bIni.isBefore(aFin)

    /**
     * Devuelve el primer error encontrado o null si la solicitud es válida.
     * [cuposDelTipo] = plazas libres del tipo elegido en este momento (null si se desconoce).
     */
    fun validar(
        solicitud: SolicitudReserva,
        ahora: ZonedDateTime,
        reservasDelUsuario: List<Reserva>,
        cuposDelTipo: Int?
    ): String? {
        val (garaje, tipo, placa, inicio, fin) = solicitud
        val duracion = Duration.between(inicio, fin)
        return when {
            !garaje.activo -> "Este garaje no está recibiendo reservas en este momento"
            inicio.isBefore(ahora.minus(TOLERANCIA_PASADO)) -> "La hora de inicio ya pasó, elige una hora futura"
            inicio.isAfter(ahora.plus(ANTICIPACION_MAXIMA)) -> "Solo puedes reservar con hasta 7 días de anticipación"
            duracion < DURACION_MINIMA -> "La reserva debe durar al menos 1 hora"
            duracion > DURACION_MAXIMA -> "La reserva puede durar máximo 12 horas"
            !dentroDeHorario(garaje, inicio, fin) ->
                "El garaje atiende de ${Formatos.hora(garaje.horaApertura)} a ${Formatos.hora(garaje.horaCierre)}: ajusta el horario"
            cuposDelTipo != null && cuposDelTipo <= 0 -> "No hay plazas libres para ${tipo.etiqueta.lowercase()} en este garaje"
            errorPlaca(placa, tipo) != null -> errorPlaca(placa, tipo)
            reservasDelUsuario.any { it.esActiva(ahora) && seSolapan(inicio, fin, it.inicio, it.fin) } ->
                "Ya tienes una reserva activa que se cruza con ese horario"
            else -> null
        }
    }
}
