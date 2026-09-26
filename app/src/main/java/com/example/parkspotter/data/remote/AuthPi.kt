package com.example.parkspotter.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<TokenPairResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<TokenPairResponse>

    @GET("auth/me")
    suspend fun me(): Response<UserResponse>
}