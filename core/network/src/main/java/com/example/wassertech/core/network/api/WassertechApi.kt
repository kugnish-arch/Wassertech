package ru.wassertech.core.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import ru.wassertech.core.network.dto.LoginRequest
import ru.wassertech.core.network.dto.LoginResponse
import ru.wassertech.core.network.dto.UserMeResponse

/**
 * Интерфейс API для Wassertech
 * Используется как в app-client, так и в app-crm
 */
interface WassertechApi {
    
    /**
     * Авторизация пользователя
     */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>
    
    /**
     * Получение информации о текущем пользователе
     */
    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserMeResponse>
}

