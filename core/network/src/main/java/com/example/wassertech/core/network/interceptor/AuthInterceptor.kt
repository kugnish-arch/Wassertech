package ru.wassertech.core.network.interceptor

import ru.wassertech.core.network.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor для добавления токена авторизации в заголовки запросов
 * Не добавляет токен к запросам /auth/login
 */
class AuthInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Не добавляем токен к запросам авторизации
        val url = originalRequest.url.toString()
        if (url.contains("/auth/login")) {
            return chain.proceed(originalRequest)
        }
        
        val token = tokenStorage.getAccessToken()
        
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}

