package ru.wassertech.client.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import ru.wassertech.client.api.dto.InstallationDto
import ru.wassertech.client.api.dto.LoginRequest
import ru.wassertech.core.network.dto.LoginResponse

/**
 * Интерфейс API для Wassertech Client
 */
interface WassertechApi {
    
    /**
     * Авторизация пользователя
     */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
    
    /**
     * Получение списка установок
     */
    @GET("installations")
    suspend fun getInstallations(
        @Header("Authorization") authorization: String
    ): Response<List<InstallationDto>>
}

