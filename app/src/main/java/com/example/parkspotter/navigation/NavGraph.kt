package com.example.parkspotter.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.screens.DetalleGarajeScreen
import com.example.parkspotter.screens.LoginScreen
import com.example.parkspotter.screens.MapaScreen
import com.example.parkspotter.screens.MisGarajesScreen
import com.example.parkspotter.screens.MisReservasScreen
import com.example.parkspotter.screens.PerfilScreen
import com.example.parkspotter.screens.RegisterScreen
import com.example.parkspotter.screens.RegistrarGarajeScreen
import com.example.parkspotter.screens.ReservarScreen
import com.example.parkspotter.screens.SplashScreen
import com.example.parkspotter.ui.components.Pestana

/**
 * Rutas centralizadas de la app. Usar estas constantes en lugar de strings
 * sueltos evita errores de tipeo al navegar.
 */
object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAPA = "mapa"                     // Sprint 3 — búsqueda y mapa en tiempo real
    const val RESERVAS = "reservas"             // Sprint 4 — historial de reservas
    const val PERFIL = "perfil"                 // Sprint 1 — gestionar perfil
    const val MIS_GARAJES = "mis_garajes"       // Sprint 2 — listado del propietario
    const val REGISTRAR_GARAJE = "registrar_garaje" // Sprint 2 — alta / edición
    const val DETALLE = "garaje"                // Sprint 3 — detalle (y gestión de plazas del propietario)
    const val RESERVAR = "reservar"             // Sprint 4 — flujo de reserva
    const val ARG_GARAJE_ID = "garajeId"
    const val ARG_PROPIETARIO = "propietario"

    fun editarGaraje(id: String) = "$REGISTRAR_GARAJE?$ARG_GARAJE_ID=$id"
    fun detalle(id: String, propietario: Boolean = false) = "$DETALLE/$id?$ARG_PROPIETARIO=$propietario"
    fun reservar(id: String) = "$RESERVAR/$id"
    fun inicioPara(rol: RolUsuario) = if (rol == RolUsuario.PROPIETARIO) MIS_GARAJES else MAPA

    fun de(p: Pestana) = when (p) {
        Pestana.MAPA -> MAPA
        Pestana.RESERVAS -> RESERVAS
        Pestana.GARAJES -> MIS_GARAJES
        Pestana.PERFIL -> PERFIL
    }
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val session = AppContainer.session
    val auth = AppContainer.authRepository
    var esPropietario by remember { mutableStateOf(auth.rolGuardado() == RolUsuario.PROPIETARIO) }

    fun irALogin() {
        navController.navigate(Rutas.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun entrar(rol: RolUsuario) {
        esPropietario = rol == RolUsuario.PROPIETARIO
        navController.navigate(Rutas.inicioPara(rol)) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    /** Cambio de pestaña de la barra inferior conservando el estado de cada una. */
    fun irAPestana(p: Pestana) {
        navController.navigate(Rutas.de(p)) {
            popUpTo(navController.graph.id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Si el refresh token venció o fue revocado, volvemos al login desde cualquier pantalla.
    LaunchedEffect(Unit) {
        SessionManager.sesionExpirada.collect { irALogin() }
    }

    NavHost(navController = navController, startDestination = Rutas.SPLASH) {

        composable(Rutas.SPLASH) {
            SplashScreen(
                onFinished = {
                    // Sesión persistente: si ya hay tokens guardados se entra directo.
                    if (session.isLoggedIn) entrar(auth.rolGuardado()) else irALogin()
                }
            )
        }

        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginSuccess = { rol -> entrar(rol) },
                onNavigateToRegister = { navController.navigate(Rutas.REGISTER) }
            )
        }

        composable(Rutas.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { rol -> entrar(rol) },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Pestañas principales ──
        composable(Rutas.MAPA) {
            MapaScreen(
                esPropietario = esPropietario,
                onPestana = ::irAPestana,
                onVerDetalle = { id -> navController.navigate(Rutas.detalle(id)) },
                onReservar = { id -> navController.navigate(Rutas.reservar(id)) }
            )
        }

        composable(Rutas.RESERVAS) {
            MisReservasScreen(
                esPropietario = esPropietario,
                onPestana = ::irAPestana,
                onVerGaraje = { id -> navController.navigate(Rutas.detalle(id)) }
            )
        }

        composable(Rutas.PERFIL) {
            PerfilScreen(
                onPestana = ::irAPestana,
                onRolCambiado = { rol -> esPropietario = rol == RolUsuario.PROPIETARIO },
                onSesionCerrada = { irALogin() }
            )
        }

        // ── Sprint 2: Gestión de garajes ──
        composable(Rutas.MIS_GARAJES) {
            MisGarajesScreen(
                onAgregarGaraje = { navController.navigate(Rutas.REGISTRAR_GARAJE) },
                onEditarGaraje = { id -> navController.navigate(Rutas.editarGaraje(id)) },
                onGestionarPlazas = { id -> navController.navigate(Rutas.detalle(id, propietario = true)) },
                onPestana = ::irAPestana
            )
        }

        composable(
            route = "${Rutas.REGISTRAR_GARAJE}?${Rutas.ARG_GARAJE_ID}={${Rutas.ARG_GARAJE_ID}}",
            arguments = listOf(navArgument(Rutas.ARG_GARAJE_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            RegistrarGarajeScreen(
                garajeId = entry.arguments?.getString(Rutas.ARG_GARAJE_ID),
                onGarajeGuardado = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }

        // ── Sprint 3: detalle del garaje ──
        composable(
            route = "${Rutas.DETALLE}/{${Rutas.ARG_GARAJE_ID}}?${Rutas.ARG_PROPIETARIO}={${Rutas.ARG_PROPIETARIO}}",
            arguments = listOf(
                navArgument(Rutas.ARG_GARAJE_ID) { type = NavType.StringType },
                navArgument(Rutas.ARG_PROPIETARIO) { type = NavType.BoolType; defaultValue = false }
            )
        ) { entry ->
            DetalleGarajeScreen(
                garajeId = entry.arguments?.getString(Rutas.ARG_GARAJE_ID).orEmpty(),
                modoPropietario = entry.arguments?.getBoolean(Rutas.ARG_PROPIETARIO) == true,
                onVolver = { navController.popBackStack() },
                onReservar = { id -> navController.navigate(Rutas.reservar(id)) }
            )
        }

        // ── Sprint 4: flujo de reserva ──
        composable(
            route = "${Rutas.RESERVAR}/{${Rutas.ARG_GARAJE_ID}}",
            arguments = listOf(navArgument(Rutas.ARG_GARAJE_ID) { type = NavType.StringType })
        ) { entry ->
            ReservarScreen(
                garajeId = entry.arguments?.getString(Rutas.ARG_GARAJE_ID).orEmpty(),
                onVolver = { navController.popBackStack() },
                onVerReservas = { irAPestana(Pestana.RESERVAS) },
                onIrAlMapa = { irAPestana(Pestana.MAPA) }
            )
        }
    }
}
