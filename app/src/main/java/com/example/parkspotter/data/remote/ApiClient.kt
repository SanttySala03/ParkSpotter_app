package com.example.parkspotter.data.remote

import android.content.Context
import com.example.parkspotter.data.session.SessionManager
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Cliente Retrofit único, compartido por AuthApi, GarajeApi y ReservaApi. */
object ApiClient {

    private val PUBLIC_AUTH_PATHS = setOf("auth/register", "auth/login", "auth/refresh", "auth/logout")

    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var sseClient: OkHttpClient? = null

    fun createAuthApi(context: Context): AuthApi =
        getRetrofit(context).create(AuthApi::class.java)

    fun createGarajeApi(context: Context): GarajeApi =
        getRetrofit(context).create(GarajeApi::class.java)

    fun createReservaApi(context: Context): ReservaApi =
        getRetrofit(context).create(ReservaApi::class.java)

    /**
     * Cliente para los streams SSE de disponibilidad (Sprint 3). Sin timeout de lectura:
     * la conexión queda abierta y el backend envía un "ping" cada 25 s.
     */
    fun sseHttpClient(context: Context): OkHttpClient =
        sseClient ?: synchronized(this) {
            sseClient ?: (getRetrofit(context).callFactory() as OkHttpClient).newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build()
                .also { sseClient = it }
        }

    private fun getRetrofit(context: Context): Retrofit =
        retrofit ?: synchronized(this) {
            retrofit ?: build(context.applicationContext).also { retrofit = it }
        }

    private fun isPublic(request: Request): Boolean =
        PUBLIC_AUTH_PATHS.any { request.url.encodedPath.endsWith(it) }

    private fun build(context: Context): Retrofit {
        val session = SessionManager(context)

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
            if (!isPublic(original)) {
                session.accessToken?.let { token -> request.header("Authorization", "Bearer $token") }
            }
            request.header("X-Client-Id", "android")
            chain.proceed(request.build())
        }

        // ISO/IEC 27001: el log nunca muestra tokens (ni el header ni los cuerpos de /auth).
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

        // Cliente "limpio" (sin authenticator) solo para renovar el token.
        val refreshApi = Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(logging).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RefreshApi::class.java)

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(session, refreshApi))
            .build()

        return Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * El access token dura 15 min. Cuando el backend responde 401, se usa el
     * refresh token para obtener uno nuevo y se reintenta la petición una vez.
     * Si el refresh también falla, se cierra la sesión y se avisa a la UI.
     */
    private class TokenAuthenticator(
        private val session: SessionManager,
        private val refreshApi: RefreshApi
    ) : Authenticator {

        override fun authenticate(route: Route?, response: Response): Request? {
            if (isPublic(response.request)) return null
            if (responseCount(response) >= 2) return null

            synchronized(this) {
                val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
                val current = session.accessToken
                // Otro hilo ya renovó el token mientras esperábamos.
                if (current != null && current != failedToken) {
                    return response.request.newBuilder().header("Authorization", "Bearer $current").build()
                }

                val refresh = session.refreshToken ?: return expirar()
                val result = runCatching { refreshApi.refresh(RefreshRequest(refresh)).execute() }.getOrNull()
                    ?: return null // sin red: no cerramos sesión
                val body = result.body()
                if (!result.isSuccessful || body == null) return expirar()

                session.accessToken = body.accessToken
                session.refreshToken = body.refreshToken
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${body.accessToken}")
                    .build()
            }
        }

        private fun expirar(): Request? {
            session.clear()
            SessionManager.notificarSesionExpirada()
            return null
        }

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
    }
}
