package com.example.parkspotter.util

import java.text.NumberFormat
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatos de presentación en español de Colombia. */
object Formatos {

    private val LOCALE = Locale.forLanguageTag("es-CO")
    private val HORA = DateTimeFormatter.ofPattern("h:mm a", LOCALE)
    private val NBSP = 0x00A0.toChar()   // espacio duro
    private val NNBSP = 0x202F.toChar()  // espacio duro angosto (Android 13+ en "a. m.")
    private val FECHA_CORTA = DateTimeFormatter.ofPattern("EEE d 'de' MMM", LOCALE)

    /** 3500 -> "$3.500" */
    fun pesos(valor: Int): String =
        "$" + NumberFormat.getIntegerInstance(LOCALE).format(valor)

    fun hora(hora: LocalTime?): String = hora?.format(HORA)?.normalizarAmPm() ?: "--"

    fun hora(fecha: ZonedDateTime): String = fecha.format(HORA).normalizarAmPm()

    /** "vie 17 de oct" */
    fun fecha(fecha: ZonedDateTime): String = fecha.format(FECHA_CORTA).replace(".", "")

    /** "Hoy", "Mañana" o la fecha corta. */
    fun dia(fecha: ZonedDateTime, ahora: ZonedDateTime): String = when (fecha.toLocalDate()) {
        ahora.toLocalDate() -> "Hoy"
        ahora.toLocalDate().plusDays(1) -> "Mañana"
        else -> fecha(fecha).replaceFirstChar { it.uppercase() }
    }

    /** "Hoy, 8:00 a. m. – 10:00 a. m." */
    fun intervalo(inicio: ZonedDateTime, fin: ZonedDateTime, ahora: ZonedDateTime): String {
        val mismoDia = inicio.toLocalDate() == fin.toLocalDate()
        return if (mismoDia) "${dia(inicio, ahora)}, ${hora(inicio)} – ${hora(fin)}"
        else "${dia(inicio, ahora)} ${hora(inicio)} – ${dia(fin, ahora)} ${hora(fin)}"
    }

    /** 90 min -> "1 h 30 min" */
    fun duracion(d: Duration): String {
        val h = d.toHours()
        val m = d.toMinutes() % 60
        return when {
            h == 0L -> "$m min"
            m == 0L -> "$h h"
            else -> "$h h $m min"
        }
    }

    /** 1 -> "1 libre", 3 -> "3 libres" */
    fun libres(n: Int): String = if (n == 1) "1 libre" else "$n libres"

    /** 0.35 -> "350 m", 2.4 -> "2,4 km" */
    fun distancia(km: Double): String =
        if (km < 1) "${(km * 1000).toInt()} m" else String.format(LOCALE, "%.1f km", km)

    /** Unifica "a. m." / "a.m." / "AM" (varía según la versión de Android) y quita espacios duros. */
    private fun String.normalizarAmPm(): String =
        replace(NBSP, ' ').replace(NNBSP, ' ')
            .replace("a.m.", "a. m.").replace("p.m.", "p. m.")
            .replace("AM", "a. m.").replace("PM", "p. m.")
}
