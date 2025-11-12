package ru.wassertech.core.network.interceptor

import ru.wassertech.core.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor для добавления токена авторизации в заголовки запросов
 */
class AuthInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
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

