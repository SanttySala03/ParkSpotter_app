package com.example.parkspotter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.navigation.NavGraph
import com.example.parkspotter.ui.theme.ParkSpotterTheme
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {

    /** Pantalla a abrir cuando la app se lanza desde una notificación. */
    private val destino = mutableStateOf<String?>(null)

    private val permisoNotificaciones = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(this)
        MapLibre.getInstance(this)
        enableEdgeToEdge()
        destino.value = intent.getStringExtra(EXTRA_DESTINO)
        pedirPermisoDeNotificaciones()
        // Sprint 5: los avisos se revisan solo mientras la app está visible.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { AppContainer.avisos.vigilar() }
        }
        setContent {
            ParkSpotterTheme {
                NavGraph(destino = destino.value, onDestinoAtendido = { destino.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        destino.value = intent.getStringExtra(EXTRA_DESTINO)
    }

    private fun pedirPermisoDeNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_DESTINO = "destino"
        const val DESTINO_RESERVAS = "reservas"
    }
}
