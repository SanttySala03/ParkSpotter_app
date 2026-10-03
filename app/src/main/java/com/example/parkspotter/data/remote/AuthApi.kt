package com.example.parkspotter.data.remote

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenPairResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenPairResponse>

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>

    @GET("auth/me")
    suspend fun me(): Response<UserResponse>

    // Sprint 1 — gestionar perfil
    @PUT("auth/me")
    suspend fun actualizarPerfil(@Body body: UpdateProfileRequest): Response<UserResponse>

    @PATCH("auth/password")
    suspend fun cambiarPassword(@Body body: ChangePasswordRequest): Response<Unit>
}

/** Usada solo por el TokenAuthenticator (llamada síncrona dentro de OkHttp). */
interface RefreshApi {
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<TokenPairResponse>
}
