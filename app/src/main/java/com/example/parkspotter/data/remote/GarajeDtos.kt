package com.example.parkspotter.data.remote

data class GarajeResponse(
    val id: String,
    val propietarioId: String,
    val nombre: String,
    val direccion: String,
    val descripcion: String?,
    val precioPorHora: Int,
    val espaciosTotales: Int,
    val espaciosDisponibles: Int,
    val fotos: List<String>?,
    val lat: Double,
    val lng: Double,
    val activo: Boolean,
    val createdAt: String?,
    val distanciaKm: Double? = null
)

data class GarajeRequest(
    val nombre: String,
    val direccion: String,
    val descripcion: String?,
    val precioPorHora: Int,
    val espaciosTotales: Int,
    val fotos: List<String> = emptyList(),
    val lat: Double,
    val lng: Double
)

/** Ambos opcionales: Gson omite los null, así se puede cambiar solo uno. */
data class DisponibilidadRequest(
    val activo: Boolean? = null,
    val espaciosDisponibles: Int? = null
)
