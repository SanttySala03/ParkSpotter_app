package com.example.parkspotter.util

import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            val body = response.body() ?: (Unit as T) // 204 No Content => Unit
            Resource.Success(body)
        } else {
            Resource.Error(mensajeDeError(response.code(), response.errorBody()?.string()))
        }
    } catch (e: IOException) {
        Resource.Error("No se pudo conectar al servidor. Verifica que el backend esté encendido y tu conexión.")
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Error inesperado")
    }
}

/** El backend responde {"status":..,"message":"..","details":{campo: msg}}; mostramos "message". */
private fun mensajeDeError(code: Int, raw: String?): String {
    val desdeBackend = raw?.takeIf { it.isNotBlank() }?.let {
        runCatching { JSONObject(it).optString("message").takeIf { m -> m.isNotBlank() } }.getOrNull()
    }
    return desdeBackend ?: when (code) {
        400 -> "Datos inválidos"
        401 -> "Tu sesión expiró, inicia sesión de nuevo"
        403 -> "No tienes permiso para realizar esta acción"
        404 -> "No encontrado"
        409 -> "Conflicto con los datos existentes"
        429 -> "Demasiados intentos, espera unos minutos"
        in 500..599 -> "Error del servidor ($code)"
        else -> "Error $code"
    }
}
