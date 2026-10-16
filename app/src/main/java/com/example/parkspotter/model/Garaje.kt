package com.example.parkspotter.model

import java.time.LocalTime

/** Color del marcador en el mapa (backend: GarageAvailability). */
enum class Disponibilidad(val etiqueta: String) {
    AVAILABLE("Disponible"),
    FEW_SPOTS("Pocos cupos"),
    FULL("Lleno"),
    CLOSED("Cerrado");

    companion object {
        fun de(valor: String?): Disponibilidad = entries.firstOrNull { it.name == valor } ?: CLOSED
    }
}

/** Estado administrativo del garaje (backend: GarageStatus). */
enum class EstadoGaraje {
    ACTIVE, INACTIVE, SUSPENDED;

    companion object {
        fun de(valor: String?): EstadoGaraje = entries.firstOrNull { it.name == valor } ?: INACTIVE
    }
}

/** Tipos de plaza (backend: VehicleType). */
enum class TipoVehiculo(val etiqueta: String) {
    CAR("Carro"),
    MOTORCYCLE("Moto"),
    BICYCLE("Bicicleta"),
    TRUCK("Camión"),
    ELECTRIC("Eléctrico"),
    DISABLED("Movilidad reducida");

    companion object {
        fun de(valor: String?): TipoVehiculo = entries.firstOrNull { it.name == valor } ?: CAR
    }
}

/** Estado de una plaza física (backend: SpotStatus). */
enum class EstadoPlaza(val etiqueta: String) {
    AVAILABLE("Libre"),
    OCCUPIED("Ocupada"),
    RESERVED("Reservada"),
    OUT_OF_SERVICE("Fuera de servicio");

    companion object {
        fun de(valor: String?): EstadoPlaza = entries.firstOrNull { it.name == valor } ?: OUT_OF_SERVICE
    }
}

/**
 * Garaje tal como lo usa la app. Se arma desde distintas respuestas del backend
 * (búsqueda, marcador del mapa, detalle o "mis garajes"), por eso varios campos son opcionales:
 * un marcador, por ejemplo, no trae dirección.
 */
data class Garaje(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val ciudad: String = "Bogotá",
    val departamento: String = "Bogotá D.C.",
    val pais: String = "Colombia",
    val codigoPostal: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val precioPorHora: Int = 0,
    val espaciosTotales: Int = 1,
    val espaciosDisponibles: Int = 1,
    val espaciosOcupados: Int = 0,
    val espaciosReservados: Int = 0,
    val abierto24h: Boolean = true,
    val horaApertura: LocalTime? = null,
    val horaCierre: LocalTime? = null,
    val estado: EstadoGaraje = EstadoGaraje.ACTIVE,
    val disponibilidad: Disponibilidad = Disponibilidad.AVAILABLE,
    val abiertoAhora: Boolean? = null,
    val distanciaKm: Double? = null,
    /** URIs locales de la vista previa: el backend aún no almacena fotos. */
    val fotos: List<String> = emptyList()
) {
    val activo: Boolean get() = estado == EstadoGaraje.ACTIVE
    val tieneUbicacion: Boolean get() = lat != null && lng != null
    val reservable: Boolean
        get() = activo && abiertoAhora != false && espaciosDisponibles > 0 && disponibilidad != Disponibilidad.CLOSED
}

data class Plaza(
    val id: String,
    val codigo: String,
    val piso: Int,
    val tipo: TipoVehiculo,
    val estado: EstadoPlaza
)

data class DisponibilidadPorTipo(
    val tipo: TipoVehiculo,
    val total: Int,
    val disponibles: Int,
    val ocupadas: Int,
    val reservadas: Int
)

/** Vista de detalle (GET /garages/{id}/detail?includeSpots=true). */
data class GarajeDetalle(
    val garaje: Garaje,
    val porTipo: List<DisponibilidadPorTipo> = emptyList(),
    val pisos: List<Int> = emptyList(),
    val plazas: List<Plaza> = emptyList(),
    val fueraDeServicio: Int = 0
)

/** Evento de disponibilidad en tiempo real (SSE) o foto puntual. */
data class DisponibilidadGaraje(
    val garajeId: String,
    val estado: EstadoGaraje,
    val disponibilidad: Disponibilidad,
    val abiertoAhora: Boolean,
    val total: Int,
    val disponibles: Int,
    val ocupadas: Int,
    val reservadas: Int,
    val fueraDeServicio: Int
)

/** Aplica un evento en vivo sobre el garaje (mapa, lista y detalle se actualizan sin recargar). */
fun Garaje.con(evento: DisponibilidadGaraje): Garaje = copy(
    estado = evento.estado,
    disponibilidad = evento.disponibilidad,
    abiertoAhora = evento.abiertoAhora,
    espaciosTotales = evento.total,
    espaciosDisponibles = evento.disponibles,
    espaciosOcupados = evento.ocupadas,
    espaciosReservados = evento.reservadas
)
