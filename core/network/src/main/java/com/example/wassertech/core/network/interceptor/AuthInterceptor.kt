package ru.wassertech.core.network.interceptor

import android.util.Log
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
    
    companion object {
        private const val TAG = "AuthInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // Логируем URL для sync/pull запросов для отладки
        if (url.contains("/sync/pull")) {
            Log.d(TAG, "=== SYNC/PULL ЗАПРОС ===")
            Log.d(TAG, "Полный URL: $url")
            Log.d(TAG, "Query параметры: ${originalRequest.url.query}")
        }
        
        // Не добавляем токен к запросам авторизации
        if (url.contains("/auth/login")) {
            Log.d(TAG, "Пропускаем добавление токена для /auth/login")
            return chain.proceed(originalRequest)
        }
        
        val token = tokenStorage.getAccessToken()
        
        val newRequest = if (token != null) {
            Log.d(TAG, "Добавляю токен в заголовок для запроса: $url")
            Log.d(TAG, "Токен: ${token.take(20)}... (длина: ${token.length})")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.w(TAG, "Токен отсутствует для запроса: $url")
            originalRequest
        }
        
        val response = chain.proceed(newRequest)
        
        if (!response.isSuccessful) {
            Log.e(TAG, "Запрос неуспешен: код=${response.code}, URL=$url")
        }
        
        return response
    }
}

