package com.example.parkspotter.data.repository

import com.example.parkspotter.data.remote.DisponibilidadEnVivo
import com.example.parkspotter.data.remote.GarajeApi
import com.example.parkspotter.data.remote.aModelo
import com.example.parkspotter.data.remote.aRequest
import com.example.parkspotter.data.remote.aUpdateRequest
import com.example.parkspotter.model.AreaMapa
import com.example.parkspotter.model.DisponibilidadGaraje
import com.example.parkspotter.model.EstadoGaraje
import com.example.parkspotter.model.EstadoPlaza
import com.example.parkspotter.model.FiltrosBusqueda
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.model.GarajeDetalle
import com.example.parkspotter.model.Plaza
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.map
import com.example.parkspotter.util.safeApiCall
import kotlinx.coroutines.flow.Flow

/** Resultado de los marcadores del mapa: truncado = hay más garajes de los que se pintaron. */
data class MarcadoresMapa(val garajes: List<Garaje>, val truncado: Boolean)

class GarajeRepository(
    private val api: GarajeApi,
    private val enVivo: DisponibilidadEnVivo? = null
) {

    // ── Sprint 3: conductor ──

    suspend fun buscar(filtros: FiltrosBusqueda, lat: Double?, lng: Double?): Resource<List<Garaje>> =
        safeApiCall { api.buscar(filtros.aParametrosBusqueda(lat, lng)) }
            .map { pagina -> pagina.content.orEmpty().map { it.aModelo() } }

    suspend fun marcadores(area: AreaMapa, filtros: FiltrosBusqueda): Resource<MarcadoresMapa> =
        safeApiCall { api.marcadores(area.aParametros() + filtros.aParametros()) }
            .map { r -> MarcadoresMapa(r.markers.orEmpty().map { it.aModelo() }, r.truncated == true) }

    suspend fun detalle(id: String): Resource<GarajeDetalle> =
        safeApiCall { api.detalle(id, incluirPlazas = true) }.map { it.aModelo() }

    suspend fun disponibilidad(id: String): Resource<DisponibilidadGaraje> =
        safeApiCall { api.disponibilidad(id) }.map { it.aModelo() }

    /** Cambios en vivo de todos los garajes (SSE). Vacío si no se configuró el stream. */
    fun disponibilidadEnVivo(): Flow<DisponibilidadGaraje>? = enVivo?.todos()

    fun disponibilidadEnVivo(id: String): Flow<DisponibilidadGaraje>? = enVivo?.garaje(id)

    // ── Sprint 2: propietario ──

    suspend fun obtener(id: String): Resource<Garaje> = safeApiCall { api.obtener(id) }.map { it.aModelo() }

    suspend fun listarMios(): Resource<List<Garaje>> =
        safeApiCall { api.listarMios() }.map { lista -> lista.map { it.aModelo() } }

    suspend fun crear(garaje: Garaje): Resource<Garaje> =
        safeApiCall { api.crear(garaje.aRequest()) }.map { it.aModelo() }

    suspend fun actualizar(id: String, garaje: Garaje): Resource<Garaje> =
        safeApiCall { api.actualizar(id, garaje.aUpdateRequest()) }.map { it.aModelo() }

    /** Publicar (ACTIVE) u ocultar (INACTIVE) el garaje del mapa. */
    suspend fun cambiarActivo(id: String, activo: Boolean): Resource<Garaje> =
        safeApiCall { api.cambiarEstado(id, if (activo) EstadoGaraje.ACTIVE.name else EstadoGaraje.INACTIVE.name) }
            .map { it.aModelo() }

    suspend fun eliminar(id: String): Resource<Unit> = safeApiCall { api.eliminar(id) }

    // ── Sprint 3: estado de las plazas (propietario) ──

    suspend fun plazas(garajeId: String): Resource<List<Plaza>> =
        safeApiCall { api.plazas(garajeId) }.map { lista -> lista.map { it.aModelo() } }

    suspend fun cambiarEstadoPlaza(garajeId: String, plazaId: String, estado: EstadoPlaza): Resource<Plaza> =
        safeApiCall { api.cambiarEstadoPlaza(garajeId, plazaId, estado.name) }.map { it.aModelo() }

    /**
     * Atajo de "Mis garajes": ocupa (−1 cupo) o libera (+1 cupo) una plaza. Desde el Sprint 3 los
     * cupos se derivan del estado de las plazas, así que se cambia la primera plaza que aplique.
     */
    suspend fun ajustarCupo(garajeId: String, liberar: Boolean): Resource<Unit> {
        val desde = if (liberar) EstadoPlaza.OCCUPIED else EstadoPlaza.AVAILABLE
        val hacia = if (liberar) EstadoPlaza.AVAILABLE else EstadoPlaza.OCCUPIED
        return when (val r = plazas(garajeId)) {
            is Resource.Success -> {
                val plaza = r.data.firstOrNull { it.estado == desde }
                    ?: return Resource.Error(
                        if (liberar) "No hay plazas ocupadas para liberar" else "No hay plazas libres para ocupar"
                    )
                cambiarEstadoPlaza(garajeId, plaza.id, hacia).map { }
            }
            is Resource.Error -> r
            Resource.Loading -> Resource.Loading
        }
    }
}
