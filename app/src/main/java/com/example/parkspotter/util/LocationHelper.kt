package com.example.parkspotter.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Ubicación actual del dispositivo. Primero intenta la última conocida (rápido);
     * si no existe (frecuente en el emulador) pide una lectura nueva al GPS.
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): Pair<Double, Double>? {
        if (!tienePermiso()) return null
        return ultimaConocida() ?: lecturaActual()
    }

    @SuppressLint("MissingPermission")
    private suspend fun ultimaConocida(): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                cont.resume(location?.let { Pair(it.latitude, it.longitude) })
            }
            .addOnFailureListener { cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun lecturaActual(): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                cont.resume(location?.let { Pair(it.latitude, it.longitude) })
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /**
     * Convierte una dirección escrita ("Cra 7 # 60-15, Bogotá") en coordenadas.
     * Devuelve null si el dispositivo no tiene servicio de geocodificación o no la encuentra.
     */
    @Suppress("DEPRECATION")
    suspend fun geocodificar(direccion: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            val consulta = if (direccion.contains("bogot", ignoreCase = true)) direccion
                           else "$direccion, Bogotá, Colombia"
            Geocoder(context, Locale.forLanguageTag("es-CO"))
                .getFromLocationName(consulta, 1)
                ?.firstOrNull()
                ?.let { Pair(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
