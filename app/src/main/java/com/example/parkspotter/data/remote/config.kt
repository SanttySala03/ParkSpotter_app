package com.example.parkspotter.data.remote

/**
 * URL del backend Spring Boot (ParkSpotter-BackEnd).
 *
 * - EMULADOR de Android Studio: 10.0.2.2 es el "localhost" de tu PC  -> deja el valor por defecto.
 * - CELULAR FÍSICO: usa la IP de tu PC en la red Wi-Fi (ipconfig en Windows),
 *   p. ej. "http://192.168.1.15:8080/api/v1/", y agrega esa IP en
 *   res/xml/network_security_config.xml para permitir HTTP.
 *
 * La URL debe terminar en "/".
 */
object Config {
    const val BASE_URL = "http://10.0.2.2:8080/api/v1/"
}
