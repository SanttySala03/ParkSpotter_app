package com.example.parkspotter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.parkspotter.data.AppContainer
import com.example.parkspotter.navigation.NavGraph
import com.example.parkspotter.ui.theme.ParkSpotterTheme
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(this)
        MapLibre.getInstance(this)
        enableEdgeToEdge()
        setContent {
            ParkSpotterTheme {
                NavGraph()
            }
        }
    }
}
