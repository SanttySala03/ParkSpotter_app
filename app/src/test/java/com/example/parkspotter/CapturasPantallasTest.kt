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
import com.example.parkspotter.model.FiltrosBusqueda
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.model.SolicitudReserva
import com.example.parkspotter.model.TipoVehiculo
import com.example.parkspotter.screens.DetalleGarajeScreen
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
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
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
        mostrar { ReservarScreen(garajeId = id, onVolver = {}, onVerReservas = {}, onIrAlMapa = {}) }
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
        mostrar { MisReservasScreen(esPropietario = false, onPestana = {}, onVerGaraje = {}) }
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
        mostrar { ReservarScreen(garajeId = id, onVolver = {}, onVerReservas = {}, onIrAlMapa = {}) }
        esperarTexto("¿Por cuánto tiempo?")
        compose.onNodeWithText("Placa").performTextInput("AGH430")
        capturar("15_reserva_moto_placa_carro")
    }

    @Test fun perfil() {
        entrar("propietario.demo@parkspotter.co", RolUsuario.PROPIETARIO)
        mostrar { PerfilScreen(onPestana = {}, onRolCambiado = {}, onSesionCerrada = {}) }
        esperarTexto("propietario.demo")
        capturar("13_perfil")
    }
}
