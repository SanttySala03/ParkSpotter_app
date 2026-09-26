package com.example.parkspotter.util

import retrofit2.Response
import java.io.IOException

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) Resource.Success(body)
            else Resource.Error("Respuesta vacía del servidor")
        } else {
            val errorMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                ?: "Error ${response.code()}"
            Resource.Error(errorMsg)
        }
    } catch (e: IOException) {
        Resource.Error("No se pudo conectar al servidor. Verifica tu conexión.")
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Error inesperado")
    }
}