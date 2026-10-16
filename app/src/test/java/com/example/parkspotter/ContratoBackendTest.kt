package com.example.parkspotter

import com.example.parkspotter.data.remote.AuthApi
import com.example.parkspotter.data.remote.DisponibilidadEnVivo
import com.example.parkspotter.data.remote.GarageUpdateRequest
import com.example.parkspotter.data.remote.GarajeApi
import com.example.parkspotter.data.remote.LoginRequest
import com.example.parkspotter.data.remote.RegisterRequest
import com.example.parkspotter.data.remote.ReservaApi
import com.example.parkspotter.data.remote.ReservationRequest
import com.example.parkspotter.data.remote.aIso
import com.example.parkspotter.data.remote.aModelo
import com.example.parkspotter.data.remote.aRequest
import com.example.parkspotter.model.AreaMapa
import com.example.parkspotter.model.EstadoPlaza
import com.example.parkspotter.model.EstadoReserva
import com.example.parkspotter.model.FiltrosBusqueda
import com.example.parkspotter.model.Garaje
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Prueba de CONTRATO entre esta app y el backend real (rama develop): usa las mismas interfaces
 * Retrofit, DTOs y mapeos de la app. Se salta si no se define la variable de entorno:
 *
 *     PARKSPOTTER_API=http://localhost:8080/api/v1/ ./gradlew :app:testDebugUnitTest
 *
 * Crea un usuario y un garaje de prueba con nombres únicos y al final borra el garaje.
 * Correr contra un backend local (H2), nunca contra la base de datos compartida del equipo.
 */
class ContratoBackendTest {

    private val baseUrl: String? = System.getenv("PARKSPOTTER_API")
    private var token: String? = null

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val r = chain.request().newBuilder().header("X-Client-Id", "android-test")
                token?.let { r.header("Authorization", "Bearer $it") }
                chain.proceed(r.build())
            }
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
    private val retrofit by lazy {
        Retrofit.Builder().baseUrl(baseUrl!!).client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }

    @Test
    fun flujoCompletoContraBackend() = runBlocking {
        assumeTrue("Define PARKSPOTTER_API para correr la prueba de contrato", !baseUrl.isNullOrBlank())
        val auth = retrofit.create(AuthApi::class.java)
        val garajes = retrofit.create(GarajeApi::class.java)
        val reservas = retrofit.create(ReservaApi::class.java)
        val unico = System.currentTimeMillis()

        // ── Sprint 1: registro, login y perfil ──
        val email = "android-$unico@parkspotter.co"
        val registro = auth.register(RegisterRequest(email, "ParkSpotter2026"))
        assertEquals("registro", 200, registro.code())
        val login = auth.login(LoginRequest(email, "ParkSpotter2026"))
        assertEquals("login", 200, login.code())
        token = login.body()!!.accessToken
        val me = auth.me()
        assertEquals(email, me.body()!!.email)

        // ── Sprint 2: alta de garaje con el request que arma el formulario ──
        val borrador = Garaje(
            nombre = "Garaje Contrato $unico", direccion = "Cra 7 # 60-15", descripcion = "Prueba de contrato",
            telefono = "+57 300 123 4567", precioPorHora = 4200, espaciosTotales = 4,
            abierto24h = false, horaApertura = LocalTime.of(0, 30), horaCierre = LocalTime.of(23, 59),
            lat = 4.6486, lng = -74.0628
        )
        val creado = garajes.crear(borrador.aRequest())
        assertEquals("crear garaje: ${creado.errorBody()?.string()}", 200, creado.code())
        val garaje = creado.body()!!.aModelo()
        assertEquals(4, garaje.espaciosTotales)
        assertEquals(4200, garaje.precioPorHora)
        assertTrue(garajes.listarMios().body()!!.any { it.id == garaje.id })

        // ── Sprint 3: búsqueda, mapa, detalle ──
        val busqueda = garajes.buscar(FiltrosBusqueda(texto = "Contrato $unico").aParametrosBusqueda(4.6486, -74.0628))
        assertEquals("buscar: ${busqueda.errorBody()?.string()}", 200, busqueda.code())
        assertTrue("el garaje aparece en /search", busqueda.body()!!.content!!.any { it.id == garaje.id })

        val area = AreaMapa(4.60, -74.10, 4.70, -74.00)
        val mapa = garajes.marcadores(area.aParametros() + FiltrosBusqueda().aParametros())
        assertEquals("mapa: ${mapa.errorBody()?.string()}", 200, mapa.code())
        assertTrue("el garaje aparece en /map", mapa.body()!!.markers!!.any { it.id == garaje.id })

        val detalle = garajes.detalle(garaje.id).body()!!.aModelo()
        assertEquals(4, detalle.plazas.size)
        assertTrue(detalle.plazas.all { it.estado == EstadoPlaza.AVAILABLE })

        // ── Sprint 3: tiempo real (SSE) — ocupar una plaza llega como evento ──
        val enVivo = DisponibilidadEnVivo(client.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build(), baseUrl!!)
        val eventos = async { withTimeout(15_000) { enVivo.garaje(garaje.id).take(2).toList() } }
        delay(1_500) // dar tiempo a que el stream se suscriba antes del cambio
        val plaza = detalle.plazas.first()
        val cambio = garajes.cambiarEstadoPlaza(garaje.id, plaza.id, EstadoPlaza.OCCUPIED.name)
        assertEquals(200, cambio.code())
        val recibidos = eventos.await()
        assertEquals("primer evento = estado actual", 4, recibidos[0].disponibles)
        assertEquals("segundo evento = cambio en vivo", 3, recibidos[1].disponibles)

        // ── Sprint 2: publicar / ocultar y editar ──
        assertEquals("INACTIVE", garajes.cambiarEstado(garaje.id, "INACTIVE").body()!!.status)
        assertEquals("ACTIVE", garajes.cambiarEstado(garaje.id, "ACTIVE").body()!!.status)
        val editado = garajes.actualizar(garaje.id, GarageUpdateRequest(pricePerHour = 4500.0))
        assertEquals(4500, editado.body()!!.aModelo().precioPorHora)

        // ── Sprint 4: reservar, ver en el historial y cancelar (backend rama Sprint-04-Reservas) ──
        val inicio = ZonedDateTime.now(ZoneId.of("America/Bogota")).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
        val creada = reservas.crear(ReservationRequest(garaje.id, "CAR", "KDZ482", inicio.aIso(), inicio.plusHours(2).aIso()))
        assertEquals("crear reserva: ${creada.errorBody()?.string()}", 201, creada.code())
        val reserva = creada.body()!!.aModelo()!!
        assertEquals(EstadoReserva.CONFIRMED, reserva.estado)
        assertEquals(9000, reserva.total) // 4500 × 2 h
        assertEquals(inicio.toInstant(), reserva.inicio.toInstant())
        assertNotNull(reserva.plazaCodigo)
        assertTrue(reservas.mias().body()!!.content!!.any { it.id == reserva.id })
        assertEquals("CANCELLED", reservas.cancelar(reserva.id).body()!!.status)

        // Limpieza
        assertEquals(204, garajes.eliminar(garaje.id).code())
    }
}
