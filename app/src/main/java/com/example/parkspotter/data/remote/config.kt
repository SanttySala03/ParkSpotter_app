package com.example.parkspotter.data.remote

import com.example.parkspotter.BuildConfig

/**
 * URL del backend Spring Boot (ParkSpotter-BackEnd, rama develop).
 *
 * - EMULADOR de Android Studio: 10.0.2.2 es el "localhost" de tu PC  -> valor por defecto.
 * - CELULAR FÍSICO: agrega en local.properties
 *       parkspotter.apiUrl=http://192.168.1.15:8080/api/v1/
 *   (IP de tu PC en la red Wi-Fi, ver `ipconfig`) y esa IP en
 *   res/xml/network_security_config.xml para permitir HTTP.
 *
 * La URL debe terminar en "/".
 */
object Config {
    /** La propiedad del sistema solo la usan las pruebas en JVM (apuntan a un backend local). */
    val BASE_URL: String = System.getProperty("parkspotter.apiUrl")?.takeIf { it.isNotBlank() } ?: BuildConfig.API_BASE_URL

    /** Zona horaria del negocio: la misma que usa el backend para "abierto ahora". */
    const val ZONA_HORARIA = "America/Bogota"
}
