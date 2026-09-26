package com.example.parkspotter.data.repository

import com.example.parkspotter.data.remote.DisponibilidadRequest
import com.example.parkspotter.data.remote.GarajeApi
import com.example.parkspotter.data.remote.GarajeRequest
import com.example.parkspotter.data.remote.GarajeResponse
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.safeApiCall
import retrofit2.Response

class GarajeRepository(private val api: GarajeApi) {

    suspend fun buscar(
        lat: Double? = null,
        lng: Double? = null,
        radioKm: Double? = null,
        precioMax: Int? = null,
        soloDisponibles: Boolean? = null,
        texto: String? = null
    ): Resource<List<Garaje>> = mapList {
        api.buscar(lat, lng, radioKm, precioMax, soloDisponibles, texto?.takeIf { it.isNotBlank() })
    }

    suspend fun listarMios(): Resource<List<Garaje>> = mapList { api.listarMios() }

    suspend fun crear(garaje: Garaje): Resource<Garaje> = mapOne { api.crear(garaje.toRequest()) }

    suspend fun actualizar(id: String, garaje: Garaje): Resource<Garaje> =
        mapOne { api.actualizar(id, garaje.toRequest()) }

    suspend fun actualizarDisponibilidad(id: String, activo: Boolean): Resource<Garaje> =
        mapOne { api.actualizarDisponibilidad(id, DisponibilidadRequest(activo)) }

    suspend fun eliminar(id: String): Resource<Unit> = safeApiCall { api.eliminar(id) }

    private suspend fun mapList(
        call: suspend () -> Response<List<GarajeResponse>>
    ): Resource<List<Garaje>> {
        return when (val result = safeApiCall(call)) {
            is Resource.Success -> Resource.Success(result.data.map { it.toModel() })
            is Resource.Error -> Resource.Error(result.message)
            Resource.Loading -> Resource.Loading
        }
    }

    private suspend fun mapOne(
        call: suspend () -> Response<GarajeResponse>
    ): Resource<Garaje> {
        return when (val result = safeApiCall(call)) {
            is Resource.Success -> Resource.Success(result.data.toModel())
            is Resource.Error -> Resource.Error(result.message)
            Resource.Loading -> Resource.Loading
        }
    }

    private fun Garaje.toRequest() = GarajeRequest(
        nombre = nombre,
        direccion = direccion,
        descripcion = descripcion,
        precioPorHora = precioPorHora,
        espaciosTotales = espaciosTotales,
        fotos = fotos,
        lat = lat,
        lng = lng
    )

    private fun GarajeResponse.toModel() = Garaje(
        id = id,
        propietarioId = propietarioId,
        nombre = nombre,
        direccion = direccion,
        descripcion = descripcion ?: "",
        precioPorHora = precioPorHora,
        espaciosTotales = espaciosTotales,
        espaciosDisponibles = espaciosDisponibles,
        fotos = fotos,
        lat = lat,
        lng = lng,
        activo = activo,
        distanciaKm = distanciaKm
    )
}
