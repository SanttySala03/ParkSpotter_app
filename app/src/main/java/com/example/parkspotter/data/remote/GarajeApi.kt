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
import retrofit2.http.QueryMap

/**
 * Endpoints de garajes del backend (rama develop). Base: /api/v1/garages.
 * Guía completa: ParkSpotter-BackEnd/docs/API-Sprint-03-Busqueda-Garajes.md
 */
interface GarajeApi {

    // ── Sprint 3: búsqueda, mapa, detalle y disponibilidad (públicos) ──

    /** Búsqueda paginada con filtros. Los filtros van en [filtros] (ver FiltrosBusqueda.toQueryMap). */
    @GET("garages/search")
    suspend fun buscar(@QueryMap filtros: Map<String, String>): Response<PageResponse<GarageSummaryResponse>>

    /** Marcadores del área visible: minLat/minLng/maxLat/maxLng + los mismos filtros de /search. */
    @GET("garages/map")
    suspend fun marcadores(@QueryMap parametros: Map<String, String>): Response<GarageMapResponse>

    @GET("garages/{id}/detail")
    suspend fun detalle(
        @Path("id") id: String,
        @Query("includeSpots") incluirPlazas: Boolean = true
    ): Response<GarageDetailResponse>

    @GET("garages/{id}/availability")
    suspend fun disponibilidad(@Path("id") id: String): Response<GarageAvailabilityResponse>

    // ── Sprint 2: gestión del propietario (requieren token) ──

    @GET("garages/{id}")
    suspend fun obtener(@Path("id") id: String): Response<GarageResponse>

    @GET("garages/mine")
    suspend fun listarMios(): Response<List<GarageResponse>>

    @POST("garages")
    suspend fun crear(@Body request: GarageRequest): Response<GarageResponse>

    @PUT("garages/{id}")
    suspend fun actualizar(@Path("id") id: String, @Body request: GarageUpdateRequest): Response<GarageResponse>

    /** status = ACTIVE | INACTIVE (SUSPENDED es solo de administradores). */
    @PATCH("garages/{id}/status")
    suspend fun cambiarEstado(@Path("id") id: String, @Query("status") estado: String): Response<GarageResponse>

    @DELETE("garages/{id}")
    suspend fun eliminar(@Path("id") id: String): Response<Unit>

    // ── Sprint 3: plazas del garaje ──

    @GET("garages/{garageId}/spots")
    suspend fun plazas(@Path("garageId") garajeId: String): Response<List<ParkingSpotResponse>>

    /** status = AVAILABLE | OCCUPIED | RESERVED | OUT_OF_SERVICE. Solo dueño/admin. */
    @PATCH("garages/{garageId}/spots/{spotId}/status")
    suspend fun cambiarEstadoPlaza(
        @Path("garageId") garajeId: String,
        @Path("spotId") plazaId: String,
        @Query("status") estado: String
    ): Response<ParkingSpotResponse>
}
