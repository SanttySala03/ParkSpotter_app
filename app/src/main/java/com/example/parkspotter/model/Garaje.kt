package com.example.parkspotter.model

/**
 * Representa un garaje publicado por un propietario.
 * Es el modelo "fuente" que luego se traduce a ParkingSpot para mostrarse
 * en el mapa (MainActivity.kt), una vez el garaje está activo.
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
    val lat: Double = 4.6782,
    val lng: Double = -74.0582,
    val activo: Boolean = true
)
