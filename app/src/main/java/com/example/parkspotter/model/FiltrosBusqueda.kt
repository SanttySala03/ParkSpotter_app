package com.example.parkspotter.model

/** Orden de resultados (backend: GarageSortOption). */
enum class OrdenBusqueda(val etiqueta: String) {
    DISTANCE("Más cercanos"),
    PRICE_ASC("Menor precio"),
    AVAILABILITY("Más cupos"),
    NAME("Nombre")
}

/**
 * Filtros de búsqueda del conductor (Sprint 3). Se traducen a los query params de
 * GET /garages/search y GET /garages/map.
 */
data class FiltrosBusqueda(
    val texto: String = "",
    val radioKm: Double = 5.0,
    val precioMax: Int? = null,
    val soloDisponibles: Boolean = false,
    val abiertoAhora: Boolean = false,
    val tipoVehiculo: TipoVehiculo? = null,
    val orden: OrdenBusqueda = OrdenBusqueda.DISTANCE
) {
    /** Cantidad de filtros distintos a los valores por defecto (para el badge del botón). */
    val activos: Int
        get() = listOf(
            radioKm != 5.0,
            precioMax != null,
            soloDisponibles,
            abiertoAhora,
            tipoVehiculo != null,
            orden != OrdenBusqueda.DISTANCE
        ).count { it }

    /** Filtros comunes a /search y /map (sin ubicación ni paginación). */
    fun aParametros(): Map<String, String> = buildMap {
        texto.trim().takeIf { it.isNotEmpty() }?.let { put("q", it.take(100)) }
        precioMax?.let { put("maxPrice", it.toString()) }
        if (soloDisponibles) put("onlyAvailable", "true")
        if (abiertoAhora) put("openNow", "true")
        tipoVehiculo?.let { put("vehicleType", it.name) }
    }

    /**
     * Parámetros de /search. Con ubicación se busca en el radio elegido; si además hay texto,
     * el radio se amplía al máximo (50 km) para no esconder un garaje que el usuario nombró.
     */
    fun aParametrosBusqueda(lat: Double?, lng: Double?, tamano: Int = 50): Map<String, String> =
        aParametros() + buildMap {
            if (lat != null && lng != null) {
                put("lat", lat.toString())
                put("lng", lng.toString())
                val radio = if (texto.isNotBlank()) 50.0 else radioKm.coerceIn(0.5, 50.0)
                put("radiusKm", radio.toString())
                put("sort", orden.name)
            } else {
                put("sort", if (orden == OrdenBusqueda.DISTANCE) OrdenBusqueda.NAME.name else orden.name)
            }
            put("page", "0")
            put("size", tamano.coerceIn(1, 100).toString())
        }
}

/** Área visible del mapa (para cargar solo esos marcadores: lazy loading). */
data class AreaMapa(
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double
) {
    val centroLat: Double get() = (minLat + maxLat) / 2
    val centroLng: Double get() = (minLng + maxLng) / 2

    fun aParametros(limite: Int = 200): Map<String, String> = mapOf(
        "minLat" to minLat.coerceIn(-90.0, 90.0).toString(),
        "minLng" to minLng.coerceIn(-180.0, 180.0).toString(),
        "maxLat" to maxLat.coerceIn(-90.0, 90.0).toString(),
        "maxLng" to maxLng.coerceIn(-180.0, 180.0).toString(),
        "limit" to limite.coerceIn(1, 500).toString()
    )
}
