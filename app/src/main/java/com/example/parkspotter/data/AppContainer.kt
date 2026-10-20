package com.example.parkspotter.data

import android.content.Context
import com.example.parkspotter.data.local.PerfilLocalStore
import com.example.parkspotter.data.local.ReservaLocalDataSource
import com.example.parkspotter.data.remote.ApiClient
import com.example.parkspotter.data.remote.Config
import com.example.parkspotter.data.remote.DisponibilidadEnVivo
import com.example.parkspotter.data.repository.AuthRepository
import com.example.parkspotter.data.repository.GarajeRepository
import com.example.parkspotter.data.repository.NotificacionRepository
import com.example.parkspotter.data.repository.PagoRepository
import com.example.parkspotter.data.repository.ReservaRepository
import com.example.parkspotter.data.session.SessionManager
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Inyección de dependencias manual (sin Hilt): una sola instancia de cada repositorio
 * para toda la app, creada la primera vez que se pide.
 */
object AppContainer {

    @Volatile private var contexto: Context? = null

    fun init(context: Context) {
        contexto = context.applicationContext
    }

    private val app: Context
        get() = checkNotNull(contexto) { "AppContainer.init() no se llamó en MainActivity" }

    val reloj: () -> ZonedDateTime = { ZonedDateTime.now(ZoneId.of(Config.ZONA_HORARIA)) }

    val session: SessionManager by lazy { SessionManager(app) }
    val perfiles: PerfilLocalStore by lazy { PerfilLocalStore(app) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(ApiClient.createAuthApi(app), session, perfiles)
    }

    val garajeRepository: GarajeRepository by lazy {
        GarajeRepository(ApiClient.createGarajeApi(app), DisponibilidadEnVivo(ApiClient.sseHttpClient(app)))
    }

    val pagoRepository: PagoRepository by lazy { PagoRepository(ApiClient.createPagoApi(app)) }

    val notificacionRepository: NotificacionRepository by lazy {
        NotificacionRepository(ApiClient.createNotificacionApi(app))
    }

    /** Revisa la bandeja mientras la app está abierta y avisa de lo nuevo con una notificación del sistema. */
    val avisos: AvisosMonitor by lazy { AvisosMonitor(app, notificacionRepository, session) }

    val reservaRepository: ReservaRepository by lazy {
        ReservaRepository(ApiClient.createReservaApi(app), ReservaLocalDataSource(app), session, perfiles, reloj)
    }
}
