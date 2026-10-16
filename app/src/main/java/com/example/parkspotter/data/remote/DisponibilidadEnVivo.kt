package com.example.parkspotter.data.remote

import com.example.parkspotter.model.DisponibilidadGaraje
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException

/**
 * Disponibilidad en tiempo real por Server-Sent Events (Sprint 3).
 *
 * - [todos]: GET /garages/availability/stream — cambios de todos los garajes (mapa y lista).
 * - [garaje]: GET /garages/{id}/availability/stream — primer evento = estado actual, luego cada cambio.
 *
 * Si la conexión se cae (sin red, backend reiniciado, 503 por saturación) se reintenta con
 * espera creciente hasta 30 s. Cada evento "availability" trae el JSON de GarageAvailabilityResponse.
 */
class DisponibilidadEnVivo(
    private val client: OkHttpClient,
    private val baseUrl: String = Config.BASE_URL,
    private val gson: Gson = Gson()
) {

    fun todos(): Flow<DisponibilidadGaraje> = stream("garages/availability/stream")

    fun garaje(id: String): Flow<DisponibilidadGaraje> = stream("garages/$id/availability/stream")

    private fun stream(ruta: String): Flow<DisponibilidadGaraje> = callbackFlow {
        val request = Request.Builder()
            .url(baseUrl + ruta)
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type != null && type != EVENTO) return
                runCatching { gson.fromJson(data, GarageAvailabilityResponse::class.java) }
                    .getOrNull()
                    ?.takeIf { it.garageId.isNotBlank() }
                    ?.let { trySend(it.aModelo()) }
            }

            override fun onClosed(eventSource: EventSource) {
                // El servidor cerró (timeout de 30 min): se fuerza la reconexión.
                close(IOException("Stream cerrado por el servidor"))
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val causa = t ?: IOException("Stream SSE falló (HTTP ${response?.code})")
                close(causa)
            }
        }

        val fuente = EventSources.createFactory(client).newEventSource(request, listener)
        awaitClose { fuente.cancel() }
    }.retryWhen { _, intento ->
        delay((2_000L shl intento.toInt().coerceAtMost(4)).coerceAtMost(30_000L))
        true
    }

    private companion object {
        const val EVENTO = "availability"
    }
}
