package com.example.parkspotter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.parkspotter.ParkSpotterApp
import com.example.parkspotter.model.RolUsuario
import com.example.parkspotter.screens.LoginScreen
import com.example.parkspotter.screens.MisGarajesScreen
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
    const val MIS_GARAJES = "mis_garajes"   // Sprint 2 — listado del propietario
    const val REGISTRAR_GARAJE = "registrar_garaje" // Sprint 2 — formulario de alta
}

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Rutas.SPLASH) {

        composable(Rutas.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // TODO: cuando exista el rol real del usuario autenticado,
                    // enrutar propietarios a MIS_GARAJES y conductores a MAPA.
                    navController.navigate(Rutas.MAPA) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Rutas.REGISTER) }
            )
        }

        composable(Rutas.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { rol ->
                    val destino = if (rol == RolUsuario.PROPIETARIO) Rutas.MIS_GARAJES else Rutas.MAPA
                    navController.navigate(destino) {
                        popUpTo(Rutas.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Rutas.MAPA) {
            // Pantalla principal ya implementada (mapa MapLibre + bottom sheet)
            ParkSpotterApp()
        }

        // ── Sprint 2: Gestión de garajes ──
        composable(Rutas.MIS_GARAJES) {
            MisGarajesScreen(
                onAgregarGaraje = { navController.navigate(Rutas.REGISTRAR_GARAJE) },
                onVerMapa = { navController.navigate(Rutas.MAPA) }
            )
        }

        composable(Rutas.REGISTRAR_GARAJE) {
            RegistrarGarajeScreen(
                onGarajeGuardado = { navController.popBackStack() },
                onCancelar = { navController.popBackStack() }
            )
        }
    }
}
