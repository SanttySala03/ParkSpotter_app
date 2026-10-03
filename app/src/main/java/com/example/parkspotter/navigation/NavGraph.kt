package com.example.parkspotter.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.parkspotter.ParkSpotterApp
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.screens.LoginScreen
import com.example.parkspotter.screens.MisGarajesScreen
import com.example.parkspotter.screens.PerfilScreen
import com.example.parkspotter.screens.RegisterScreen
import com.example.parkspotter.screens.RegistrarGarajeScreen
import com.example.parkspotter.screens.SplashScreen

/**
 * Rutas centralizadas de la app. Usar estas constantes en lugar de strings
 * sueltos evita errores de tipeo al navegar.
 */
object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAPA = "mapa"                 // ParkSpotterApp (pantalla principal con el mapa)
    const val PERFIL = "perfil"             // Sprint 1 — gestionar perfil
    const val MIS_GARAJES = "mis_garajes"   // Sprint 2 — listado del propietario
    const val REGISTRAR_GARAJE = "registrar_garaje" // Sprint 2 — alta / edición
    const val ARG_GARAJE_ID = "garajeId"

    fun editarGaraje(id: String) = "$REGISTRAR_GARAJE?$ARG_GARAJE_ID=$id"
    fun inicioPara(rol: RolUsuario) = if (rol == RolUsuario.PROPIETARIO) MIS_GARAJES else MAPA
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }

    fun irALogin() {
        navController.navigate(Rutas.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
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
                    val destino = if (session.isLoggedIn) Rutas.inicioPara(session.rolLocal) else Rutas.LOGIN
                    navController.navigate(destino) {
                        popUpTo(Rutas.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginSuccess = { rol ->
                    navController.navigate(Rutas.inicioPara(rol)) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Rutas.REGISTER) }
            )
        }

        composable(Rutas.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { rol ->
                    navController.navigate(Rutas.inicioPara(rol)) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Rutas.MAPA) {
            ParkSpotterApp(onPerfil = { navController.navigate(Rutas.PERFIL) })
        }

        composable(Rutas.PERFIL) {
            PerfilScreen(
                onVolver = { navController.popBackStack() },
                onSesionCerrada = { irALogin() }
            )
        }

        // ── Sprint 2: Gestión de garajes ──
        composable(Rutas.MIS_GARAJES) {
            MisGarajesScreen(
                onAgregarGaraje = { navController.navigate(Rutas.REGISTRAR_GARAJE) },
                onEditarGaraje = { id -> navController.navigate(Rutas.editarGaraje(id)) },
                onVerMapa = { navController.navigate(Rutas.MAPA) },
                onPerfil = { navController.navigate(Rutas.PERFIL) }
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
    }
}
