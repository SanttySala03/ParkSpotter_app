package com.example.parkspotter.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GarajeApi {

    // Búsqueda de garajes activos — fuente real del mapa (Sprint 3).
    // Todos los parámetros son opcionales: sin ellos trae todos los activos.
    @GET("garajes")
    suspend fun buscar(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radioKm") radioKm: Double? = null,
        @Query("precioMax") precioMax: Int? = null,
        @Query("soloDisponibles") soloDisponibles: Boolean? = null,
        @Query("q") texto: String? = null
    ): Response<List<GarajeResponse>>

    @GET("garajes/{id}")
    suspend fun obtener(@Path("id") id: String): Response<GarajeResponse>

    // Garajes del propietario autenticado — fuente de MisGarajesScreen (Sprint 2).
    @GET("garajes/mios")
    suspend fun listarMios(): Response<List<GarajeResponse>>

    @POST("garajes")
    suspend fun crear(@Body request: GarajeRequest): Response<GarajeResponse>

    @PUT("garajes/{id}")
    suspend fun actualizar(@Path("id") id: String, @Body request: GarajeRequest): Response<GarajeResponse>

    @PATCH("garajes/{id}/disponibilidad")
    suspend fun actualizarDisponibilidad(
        @Path("id") id: String,
        @Body request: DisponibilidadRequest
    ): Response<GarajeResponse>

    @DELETE("garajes/{id}")
    suspend fun eliminar(@Path("id") id: String): Response<Unit>
}
