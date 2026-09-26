package com.example.parkspotter.data.remote

import android.content.Context
import com.example.parkspotter.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Cliente Retrofit único, compartido por AuthApi y GarajeApi. */
object ApiClient {

    @Volatile private var retrofit: Retrofit? = null

    fun createAuthApi(context: Context): AuthApi =
        getRetrofit(context).create(AuthApi::class.java)

    fun createGarajeApi(context: Context): GarajeApi =
        getRetrofit(context).create(GarajeApi::class.java)

    private fun getRetrofit(context: Context): Retrofit =
        retrofit ?: synchronized(this) {
            retrofit ?: build(context).also { retrofit = it }
        }

    private fun build(context: Context): Retrofit {
        val session = SessionManager(context.applicationContext)

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
            session.accessToken?.let { token ->
                request.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(request.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}