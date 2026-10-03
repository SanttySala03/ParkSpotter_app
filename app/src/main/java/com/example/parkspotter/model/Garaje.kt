package com.example.parkspotter.model

/**
 * Garaje publicado por un propietario. Se muestra en el mapa (MainActivity.kt)
 * cuando está activo. lat/lng son null mientras el propietario no haya
 * definido la ubicación en el formulario.
 */
data class Garaje(
    val id: String = "",
    val propietarioId: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val descripcion: String = "",
    val precioPorHora: Int = 0,
    val espaciosTotales: Int = 1,
    val espaciosDisponibles: Int = 1,
    val fotos: List<String> = emptyList(),
    val lat: Double? = null,
    val lng: Double? = null,
    val activo: Boolean = true,
    val distanciaKm: Double? = null
)
