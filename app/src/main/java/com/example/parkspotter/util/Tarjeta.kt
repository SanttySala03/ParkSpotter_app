package com.example.parkspotter.util

import java.time.YearMonth

/**
 * Validación de la tarjeta en el celular y tokenización (Sprint 5).
 *
 * shortcut: la tokenización es simulada (el token se arma aquí). Con Wompi, el número se envía directo
 * a su API de tokenización y la app recibe el token; el backend nunca ve el número en ningún caso.
 */
object Tarjeta {

    /** Tarjetas de prueba de la pasarela simulada. */
    const val PRUEBA_APROBADA = "4242424242424242"
    const val PRUEBA_RECHAZADA = "4000000000000002"

    fun digitos(numero: String): String = numero.filter(Char::isDigit)

    fun franquicia(numero: String): String? {
        val n = digitos(numero)
        val dos = n.take(2).toIntOrNull() ?: return null
        val cuatro = n.take(4).toIntOrNull() ?: 0
        return when {
            n.startsWith("4") -> "VISA"
            dos in 51..55 || cuatro in 2221..2720 -> "MASTERCARD"
            dos == 34 || dos == 37 -> "AMEX"
            else -> null
        }
    }

    /** Dígito de control de Luhn: descarta errores de digitación. */
    fun luhnValido(numero: String): Boolean {
        val n = digitos(numero)
        if (n.length !in 13..19) return false
        val suma = n.reversed().mapIndexed { i, c ->
            val d = c - '0'
            if (i % 2 == 1) (d * 2).let { if (it > 9) it - 9 else it } else d
        }.sum()
        return suma % 10 == 0
    }

    /** Devuelve el primer error del formulario o null si se puede pagar. [vence] en formato MM/AA. */
    fun error(numero: String, vence: String, cvv: String, titular: String, hoy: YearMonth): String? {
        val marca = franquicia(numero)
        val partes = vence.split("/")
        val mes = partes.getOrNull(0)?.toIntOrNull()
        val anio = partes.getOrNull(1)?.toIntOrNull()
        val vencimiento = if (mes != null && mes in 1..12 && anio != null) YearMonth.of(2000 + anio, mes) else null
        return when {
            titular.isBlank() -> "Escribe el nombre como aparece en la tarjeta"
            marca == null -> "Solo aceptamos Visa, Mastercard y American Express"
            !luhnValido(numero) -> "El número de la tarjeta no es válido"
            vencimiento == null -> "Escribe el vencimiento como MM/AA"
            vencimiento.isBefore(hoy) -> "La tarjeta está vencida"
            cvv.length != (if (marca == "AMEX") 4 else 3) || cvv.any { !it.isDigit() } ->
                "El código de seguridad tiene ${if (marca == "AMEX") 4 else 3} dígitos"
            else -> null
        }
    }

    fun tokenizar(numero: String): String {
        val n = digitos(numero)
        return (if (n == PRUEBA_RECHAZADA) "tok_test_declined_" else "tok_test_approved_") + n.takeLast(4)
    }
}
