package com.example.parkspotter.data

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.parkspotter.MainActivity
import com.example.parkspotter.R
import com.example.parkspotter.data.repository.NotificacionRepository
import com.example.parkspotter.data.session.SessionManager
import com.example.parkspotter.model.Notificacion
import com.example.parkspotter.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sprint 5 — Avisos dentro de la app: mientras está abierta revisa la bandeja cada 30 s, publica el número
 * de no leídas (campana) y muestra cada aviso nuevo como notificación del sistema. Al tocarla, la app abre
 * "Mis reservas".
 *
 * shortcut: con la app cerrada no llegan avisos; para eso hace falta push con Firebase (FCM).
 */
class AvisosMonitor(
    private val context: Context,
    private val repository: NotificacionRepository,
    private val session: SessionManager
) {

    private val _noLeidas = MutableStateFlow(0)
    val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    private val vistas = mutableSetOf<String>()
    private var primeraRevision = true

    /** Bucle de revisión; se corta al cancelar la corrutina (la app pasa a segundo plano). */
    suspend fun vigilar() {
        while (true) {
            if (session.isLoggedIn) revisar()
            delay(INTERVALO_MS)
        }
    }

    /** Revisión inmediata (al abrir la bandeja o después de pagar). */
    suspend fun revisar() {
        val r = repository.listar() as? Resource.Success ?: return
        val noLeidas = r.data.filter { !it.leida }
        _noLeidas.value = noLeidas.size
        // En la primera revisión no se repiten como notificación los avisos que ya estaban.
        if (!primeraRevision) noLeidas.filter { it.id !in vistas }.forEach(::mostrar)
        vistas += r.data.map { it.id }
        primeraRevision = false
    }

    fun descontar() {
        _noLeidas.value = (_noLeidas.value - 1).coerceAtLeast(0)
    }

    fun ponerEnCero() {
        _noLeidas.value = 0
    }

    private fun mostrar(n: Notificacion) {
        val sinPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (sinPermiso) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CANAL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Avisos de ParkSpotter")
                .build()
        )
        val abrir = PendingIntent.getActivity(
            context, n.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_DESTINO, MainActivity.DESTINO_RESERVAS)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setColor(0xFF0B2D5B.toInt())
            .setContentTitle(n.titulo)
            .setContentText(n.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(n.mensaje))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        manager.notify(n.id.hashCode(), notificacion)
    }

    private companion object {
        const val CANAL = "avisos"
        const val INTERVALO_MS = 30_000L
    }
}
