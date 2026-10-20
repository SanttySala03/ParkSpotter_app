package com.example.parkspotter

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.model.EstadoPago
import com.example.parkspotter.model.FiltrosBusqueda
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.model.SolicitudReserva
import com.example.parkspotter.model.TipoVehiculo
import com.example.parkspotter.screens.ComprobanteScreen
import com.example.parkspotter.screens.DetalleGarajeScreen
import com.example.parkspotter.screens.NotificacionesScreen
import com.example.parkspotter.screens.PagoScreen
import com.example.parkspotter.screens.LoginScreen
import com.example.parkspotter.screens.MisGarajesScreen
import com.example.parkspotter.screens.MisReservasScreen
import com.example.parkspotter.screens.PerfilScreen
import com.example.parkspotter.screens.RegisterScreen
import com.example.parkspotter.screens.RegistrarGarajeScreen
import com.example.parkspotter.screens.ReservarScreen
import com.example.parkspotter.screens.SplashScreen
import com.example.parkspotter.ui.theme.ParkSpotterTheme
import com.example.parkspotter.util.Resource
import com.example.parkspotter.util.Tarjeta
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit
import java.io.File

/**
 * Capturas de las pantallas reales renderizadas en la JVM (Robolectric) contra un backend local,
 * para revisar la UI sin emulador. Se salta si faltan las variables de entorno:
 *
 *     PARKSPOTTER_API=http://localhost:8080/api/v1/  PARKSPOTTER_SHOTS=<carpeta>  ./gradlew :app:testDebugUnitTest
 *
 * Requiere los datos de demostración (propietario.demo / conductor.demo y garajes en Chapinero).
 * El mapa (MapLibre, código nativo) no se puede renderizar aquí.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h860dp-xhdpi")
class CapturasPantallasTest {

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val carpeta: String? = System.getProperty("parkspotter.shots")

    @Before
    fun preparar() {
        assumeTrue("Define PARKSPOTTER_API y PARKSPOTTER_SHOTS", !carpeta.isNullOrBlank())
        AppContainer.init(ApplicationProvider.getApplicationContext())
    }

    private fun entrar(email: String, rol: RolUsuario = RolUsuario.CONDUCTOR) = runBlocking {
        val r = AppContainer.authRepository.login(email, "ParkSpotter2026")
        check(r is Resource.Success) { "No se pudo iniciar sesión con $email: $r" }
        AppContainer.authRepository.cambiarRol(rol)
    }

    private fun garajeId(nombre: String): String = runBlocking {
        val r = AppContainer.garajeRepository.buscar(FiltrosBusqueda(texto = nombre), 4.6486, -74.0628)
        (r as Resource.Success).data.first { it.nombre == nombre }.id
    }

    private fun mostrar(contenido: @Composable () -> Unit) = compose.setContent { ParkSpotterTheme { contenido() } }

    private fun esperarTexto(texto: String, timeout: Long = 15_000) =
        compose.waitUntil(timeout) { compose.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun capturar(nombre: String) {
        compose.waitForIdle()
        // Se dibuja la ventana completa (captureToImage no funciona bajo Robolectric).
        val vista = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(vista.width, vista.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { vista.draw(Canvas(bmp)) }
        File(carpeta!!).apply { mkdirs() }.resolve("$nombre.png").outputStream().use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun splash() {
        mostrar { SplashScreen(onFinished = {}) }
        compose.mainClock.advanceTimeBy(1_200)
        capturar("01_splash")
    }

    @Test fun login() {
        mostrar { LoginScreen(onLoginSuccess = {}, onNavigateToRegister = {}) }
        capturar("02_login")
    }

    @Test fun registro() {
        mostrar { RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {}) }
        capturar("03_registro")
    }

    @Test fun detalleConductor() {
        entrar("conductor.demo@parkspotter.co")
        val id = garajeId("Parqueadero Chapinero 60")
        mostrar { DetalleGarajeScreen(garajeId = id, modoPropietario = false, onVolver = {}, onReservar = {}) }
        esperarTexto("Cupos por tipo")
        capturar("05_detalle_garaje")
    }

    @Test fun flujoReserva() {
        entrar("conductor.demo@parkspotter.co")
        val id = garajeId("Parqueadero Chapinero 60")
        mostrar { ReservarScreen(garajeId = id, onVolver = {}, onVerReservas = {}, onIrAlMapa = {}, onPagar = {}) }
        esperarTexto("¿Por cuánto tiempo?")
        compose.onNodeWithText("Placa").performTextInput("KDZ482")
        capturar("06_reserva_paso1_horario")
        compose.onNodeWithText("Continuar").performClick()
        esperarTexto("Revisa tu reserva")
        capturar("07_reserva_paso2_confirmar")
        compose.onNodeWithText("Confirmar reserva").performClick()
        esperarTexto("¡Reserva confirmada!")
        capturar("08_reserva_paso3_listo")
    }

    @Test fun misReservas() {
        entrar("conductor.demo@parkspotter.co")
        val id = garajeId("Garaje Zona G")
        runBlocking {
            val garaje = (AppContainer.garajeRepository.detalle(id) as Resource.Success).data.garaje
            val inicio = AppContainer.reloj().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0)
            AppContainer.reservaRepository.crear(SolicitudReserva(garaje, TipoVehiculo.CAR, "KDZ482", inicio, inicio.plusHours(3)))
            val otro = AppContainer.reservaRepository.crear(
                SolicitudReserva(garaje, TipoVehiculo.CAR, "KDZ482", inicio.plusDays(1), inicio.plusDays(1).plusHours(2))
            )
            AppContainer.reservaRepository.cancelar((otro as Resource.Success).data.id)
        }
        mostrar { MisReservasScreen(esPropietario = false, onPestana = {}, onVerGaraje = {}, onPagar = {}, onComprobante = {}, onAvisos = {}) }
        esperarTexto("Garaje Zona G")
        capturar("09_mis_reservas")
    }

    @Test fun misGarajesPropietario() {
        entrar("propietario.demo@parkspotter.co", RolUsuario.PROPIETARIO)
        mostrar { MisGarajesScreen(onAgregarGaraje = {}, onEditarGaraje = {}, onGestionarPlazas = {}, onPestana = {}) }
        try { esperarTexto("Garaje Javeriana") } finally { capturar("10_mis_garajes") }
    }

    @Test fun plazasPropietario() {
        entrar("propietario.demo@parkspotter.co", RolUsuario.PROPIETARIO)
        val id = garajeId("Parqueadero Chapinero 60")
        mostrar { DetalleGarajeScreen(garajeId = id, modoPropietario = true, onVolver = {}, onReservar = {}) }
        esperarTexto("toca una para cambiar")
        capturar("11_gestion_plazas")
    }

    @Test fun formularioGaraje() {
        entrar("propietario.demo@parkspotter.co", RolUsuario.PROPIETARIO)
        mostrar { RegistrarGarajeScreen(garajeId = null, onGarajeGuardado = {}, onCancelar = {}) }
        capturar("12_publicar_garaje")
    }

    @Test fun formularioGarajeAmbos() {
        entrar("propietario.demo@parkspotter.co", RolUsuario.PROPIETARIO)
        mostrar { RegistrarGarajeScreen(garajeId = null, onGarajeGuardado = {}, onCancelar = {}) }
        compose.onNodeWithText("Ambos").performScrollTo().performClick()
        compose.onNodeWithText("Cupos moto").performScrollTo()
        capturar("14_publicar_garaje_ambos")
    }

    @Test fun reservaMoto() {
        entrar("conductor.demo@parkspotter.co")
        val id = garajeId("Parqueadero Rosales Motos")
        mostrar { ReservarScreen(garajeId = id, onVolver = {}, onVerReservas = {}, onIrAlMapa = {}, onPagar = {}) }
        esperarTexto("¿Por cuánto tiempo?")
        compose.onNodeWithText("Placa").performTextInput("AGH430")
        capturar("15_reserva_moto_placa_carro")
    }

    @Test fun pagoYComprobante() {
        entrar("conductor.demo@parkspotter.co")
        val id = garajeId("Garaje Javeriana")
        val reserva = runBlocking {
            val garaje = (AppContainer.garajeRepository.detalle(id) as Resource.Success).data.garaje
            val inicio = AppContainer.reloj().plusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0)
            (AppContainer.reservaRepository.crear(SolicitudReserva(garaje, TipoVehiculo.CAR, "AGH430", inicio, inicio.plusHours(2)))
                as Resource.Success).data
        }
        var aprobado: String? = null
        mostrar { PagoScreen(reservaId = reserva.id, onVolver = {}, onAprobado = { aprobado = it }) }
        esperarTexto("Total a pagar")
        compose.onNodeWithText("Número de la tarjeta").performTextInput("4242424242424242")
        compose.onNodeWithText("Vence (MM/AA)").performTextInput("12/29")
        compose.onNodeWithText("CVV").performTextInput("123")
        compose.onNodeWithText("Nombre en la tarjeta").performTextInput("ANA PEREZ")
        capturar("16_pago_tarjeta")
        compose.onNodeWithText("Pagar $", substring = true).performClick()
        // En Robolectric el reloj del hilo principal no avanza solo: se avanza para que corran las consultas de cada segundo.
        repeat(40) {
            if (aprobado == null) {
                ShadowLooper.idleMainLooper(500, TimeUnit.MILLISECONDS)
                compose.waitForIdle()
                Thread.sleep(250)
            }
        }
        check(aprobado != null) { "La app no vio el pago aprobado" }
    }

    @Test fun comprobante() {
        entrar("conductor.demo@parkspotter.co")
        val reservaId = reservaPagada(diasAdelante = 3)
        mostrar { ComprobanteScreen(reservaId = reservaId, onVolver = {}) }
        esperarTexto("Pago aprobado")
        capturar("17_comprobante")
    }

    @Test fun avisos() {
        entrar("conductor.demo@parkspotter.co")
        reservaPagada(diasAdelante = 4)
        mostrar { NotificacionesScreen(onVolver = {}, onAbrirReserva = {}) }
        esperarTexto("Pago aprobado")
        capturar("18_avisos")
    }

    /** Crea una reserva en Garaje Javeriana y la paga con la tarjeta de prueba; devuelve su id. */
    private fun reservaPagada(diasAdelante: Long): String = runBlocking {
        val garaje = (AppContainer.garajeRepository.detalle(garajeId("Garaje Javeriana")) as Resource.Success).data.garaje
        val inicio = AppContainer.reloj().plusDays(diasAdelante).withHour(8).withMinute(0).withSecond(0).withNano(0)
        val reserva = (AppContainer.reservaRepository.crear(SolicitudReserva(garaje, TipoVehiculo.CAR, "AGH430", inicio, inicio.plusHours(2)))
            as Resource.Success).data
        var pago = (AppContainer.pagoRepository.pagar(reserva.id, Tarjeta.PRUEBA_APROBADA, "captura-${reserva.id}") as Resource.Success).data
        repeat(20) { if (pago.estado == EstadoPago.PENDING) { Thread.sleep(500); pago = (AppContainer.pagoRepository.obtener(pago.id) as Resource.Success).data } }
        check(pago.estado == EstadoPago.APPROVED) { "El pago no se aprobó: ${pago.estado}" }
        reserva.id
    }

    @Test fun perfil() {
        entrar("propietario.demo@parkspotter.co", RolUsuario.PROPIETARIO)
        mostrar { PerfilScreen(onPestana = {}, onRolCambiado = {}, onSesionCerrada = {}) }
        esperarTexto("propietario.demo")
        capturar("13_perfil")
    }
}
