package com.example.parkspotter.util

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            val body = response.body() ?: (Unit as T) // 204 No Content => Unit
            Resource.Success(body)
        } else {
            Resource.Error(mensajeDeError(response.code(), response.errorBody()?.string()), response.code())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Resource.Error("No se pudo conectar al servidor. Verifica que el backend esté encendido y tu conexión.")
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Error inesperado")
    }
}

/** Nombres de campo del backend -> etiqueta que ve el usuario en el formulario. */
private val CAMPOS = mapOf(
    "email" to "Correo", "password" to "Contraseña", "newPassword" to "Nueva contraseña",
    "currentPassword" to "Contraseña actual", "name" to "Nombre", "description" to "Descripción",
    "phone" to "Teléfono", "addressLine" to "Dirección", "city" to "Ciudad", "state" to "Departamento",
    "country" to "País", "postalCode" to "Código postal", "latitude" to "Ubicación",
    "longitude" to "Ubicación", "totalSpots" to "Espacios", "pricePerHour" to "Precio por hora",
    "openingTime" to "Hora de apertura", "closingTime" to "Hora de cierre",
    "startTime" to "Inicio", "endTime" to "Fin", "vehiclePlate" to "Placa", "vehicleType" to "Tipo de vehículo"
)

/**
 * El backend responde siempre ApiError: {"status", "message", "details": {campo: mensaje}}.
 * En errores de validación se muestra el primer campo inválido con su etiqueta en español.
 */
internal fun mensajeDeError(code: Int, raw: String?): String {
    val json = raw?.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
    val mensaje = json?.optString("message")?.takeIf { it.isNotBlank() }
    val detalle = json?.optJSONObject("details")?.let { d ->
        d.keys().asSequence().firstOrNull()?.let { campo ->
            val texto = d.optString(campo)
            val etiqueta = CAMPOS[campo]
            when {
                texto.isBlank() -> null
                // Reglas de varios campos (p. ej. "scheduleValid"): el mensaje ya es autoexplicativo.
                etiqueta == null -> texto.replaceFirstChar { it.uppercase() }
                else -> "$etiqueta: $texto"
            }
        }
    }
    return detalle ?: mensaje ?: when (code) {
        400 -> "Datos inválidos"
        401 -> "Tu sesión expiró, inicia sesión de nuevo"
        403 -> "No tienes permiso para realizar esta acción"
        404 -> "No encontrado"
        409 -> "Conflicto con los datos existentes"
        429 -> "Demasiados intentos, espera unos minutos"
        503 -> "Servicio ocupado, intenta en unos segundos"
        in 500..599 -> "Error del servidor ($code)"
        else -> "Error $code"
    }
}
