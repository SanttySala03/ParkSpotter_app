package com.example.parkspotter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.repository.GarajeRepository
import com.example.parkspotter.model.AreaMapa
import com.example.parkspotter.model.FiltrosBusqueda
import com.example.parkspotter.model.Garaje
import com.example.parkspotter.model.con
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Respaldo si el stream en vivo no está disponible: recargar el área visible cada 20 s. */
private const val INTERVALO_RESPALDO_MS = 20_000L
/** Espera tras mover el mapa antes de pedir marcadores (evita una petición por cada frame). */
private const val ESPERA_MOVIMIENTO_MS = 350L

data class EstadoBusqueda(
    /** Marcadores del área visible (GET /garages/map). */
    val marcadores: List<Garaje> = emptyList(),
    /** Resultados de la lista (GET /garages/search), ordenados según el filtro. */
    val resultados: List<Garaje> = emptyList(),
    val filtros: FiltrosBusqueda = FiltrosBusqueda(),
    val ubicacionUsuario: Pair<Double, Double>? = null,
    val cargandoMapa: Boolean = false,
    val cargandoLista: Boolean = false,
    /** El área tiene más garajes de los que se pintaron: sugerir acercar el zoom. */
    val truncado: Boolean = false,
    val enVivo: Boolean = false,
    val error: String? = null
)

/**
 * Sprint 3 — Búsqueda y mapa en tiempo real para el conductor.
 * - Mapa: solo se cargan los marcadores del área visible (lazy loading).
 * - Lista: búsqueda por cercanía con filtros y orden.
 * - Tiempo real: los cambios de cupos llegan por SSE y se aplican sin recargar.
 */
class BusquedaViewModel(private val repository: GarajeRepository) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoBusqueda())
    val estado: StateFlow<EstadoBusqueda> = _estado.asStateFlow()

    /** Pide a la UI mover la cámara (p. ej. al buscar un garaje por nombre). */
    private val _moverCamara = MutableSharedFlow<Pair<Double, Double>>(extraBufferCapacity = 1)
    val moverCamara: SharedFlow<Pair<Double, Double>> = _moverCamara.asSharedFlow()

    private var area: AreaMapa? = null
    private var jobMapa: Job? = null
    private var jobLista: Job? = null
    private var jobEnVivo: Job? = null

    fun onAreaVisible(nueva: AreaMapa) {
        area = nueva
        cargarMarcadores(conEspera = true)
    }

    fun actualizarUbicacion(lat: Double, lng: Double) {
        _estado.update { it.copy(ubicacionUsuario = lat to lng) }
        cargarLista()
    }

    fun actualizarTexto(texto: String) {
        _estado.update { it.copy(filtros = it.filtros.copy(texto = texto)) }
        if (texto.isBlank()) refrescar()
    }

    fun aplicarFiltros(nuevos: FiltrosBusqueda) {
        _estado.update { it.copy(filtros = nuevos) }
        refrescar()
    }

    fun limpiarFiltros() = aplicarFiltros(FiltrosBusqueda(texto = _estado.value.filtros.texto))

    /** Búsqueda por texto: recarga todo y centra el mapa en el resultado más relevante. */
    fun buscarTexto() {
        cargarMarcadores(conEspera = false)
        cargarLista { resultados ->
            resultados.firstOrNull { it.tieneUbicacion }?.let { _moverCamara.tryEmit(it.lat!! to it.lng!!) }
            if (resultados.isEmpty()) _estado.update { it.copy(error = "No encontramos garajes con \"${it.filtros.texto}\"") }
        }
    }

    fun refrescar() {
        cargarMarcadores(conEspera = false)
        cargarLista()
    }

    private fun cargarMarcadores(conEspera: Boolean) {
        val zona = area ?: return
        jobMapa?.cancel()
        jobMapa = viewModelScope.launch {
            if (conEspera) delay(ESPERA_MOVIMIENTO_MS)
            _estado.update { it.copy(cargandoMapa = true) }
            when (val r = repository.marcadores(zona, _estado.value.filtros)) {
                is Resource.Success -> _estado.update {
                    it.copy(marcadores = r.data.garajes, truncado = r.data.truncado, error = null)
                }
                is Resource.Error -> _estado.update { it.copy(error = r.message) }
                Resource.Loading -> Unit
            }
            _estado.update { it.copy(cargandoMapa = false) }
        }
    }

    /** La lista se ordena desde la ubicación del usuario o, si no la dio, desde el centro del mapa. */
    private fun cargarLista(alTerminar: (List<Garaje>) -> Unit = {}) {
        val s = _estado.value
        val (lat, lng) = s.ubicacionUsuario ?: area?.let { it.centroLat to it.centroLng } ?: (null to null)
        jobLista?.cancel()
        jobLista = viewModelScope.launch {
            _estado.update { it.copy(cargandoLista = true) }
            when (val r = repository.buscar(s.filtros, lat, lng)) {
                is Resource.Success -> {
                    _estado.update { it.copy(resultados = r.data, error = null) }
                    alTerminar(r.data)
                }
                is Resource.Error -> _estado.update { it.copy(error = r.message) }
                Resource.Loading -> Unit
            }
            _estado.update { it.copy(cargandoLista = false) }
        }
    }

    /** Se llama cuando la pantalla del mapa queda visible (ON_START). */
    fun iniciarEnVivo() {
        if (jobEnVivo?.isActive == true) return
        val stream = repository.disponibilidadEnVivo()
        jobEnVivo = viewModelScope.launch {
            if (stream == null) {
                while (isActive) {
                    delay(INTERVALO_RESPALDO_MS)
                    cargarMarcadores(conEspera = false)
                }
            } else {
                // Al (re)conectar se recarga el área: lo que cambió mientras no había conexión no llega por el stream.
                refrescar()
                _estado.update { it.copy(enVivo = true) }
                stream.collect { evento ->
                    _estado.update { s ->
                        s.copy(
                            marcadores = s.marcadores.map { if (it.id == evento.garajeId) it.con(evento) else it },
                            resultados = s.resultados.map { if (it.id == evento.garajeId) it.con(evento) else it }
                        )
                    }
                }
            }
        }
    }

    /** ON_STOP: se cierra el stream para no gastar batería ni conexiones del servidor. */
    fun detenerEnVivo() {
        jobEnVivo?.cancel()
        jobEnVivo = null
        _estado.update { it.copy(enVivo = false) }
    }

    fun limpiarError() = _estado.update { it.copy(error = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { BusquedaViewModel(AppContainer.garajeRepository) }
        }
    }
}
