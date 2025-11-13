package ru.wassertech.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Interceptor для обработки ошибок сети
 */
class ErrorInterceptor : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        try {
            val response = chain.proceed(request)
            
            // Обработка HTTP ошибок
            when (response.code) {
                401 -> {
                    // Unauthorized - токен истек или невалиден
                    // Можно добавить логику обновления токена здесь
                }
                403 -> {
                    // Forbidden
                }
                404 -> {
                    // Not Found
                }
                500 -> {
                    // Internal Server Error
                }
            }
            
            return response
        } catch (e: IOException) {
            // Обработка сетевых ошибок
            // Сохраняем оригинальное сообщение об ошибке для лучшей диагностики
            val errorMessage = e.message ?: "Unknown network error"
            val detailedMessage = when {
                errorMessage.contains("exhausted all routes") -> {
                    "Failed to connect to server. Check internet connection and server availability. Original: $errorMessage"
                }
                errorMessage.contains("timeout") -> {
                    "Connection timeout. Server may be unavailable. Original: $errorMessage"
                }
                errorMessage.contains("SSL") || errorMessage.contains("certificate") -> {
                    "SSL certificate error. Original: $errorMessage"
                }
                else -> "Network error: $errorMessage"
            }
            throw NetworkException(detailedMessage, e)
        }
    }
}

/**
 * Исключение для сетевых ошибок
 */
class NetworkException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)

