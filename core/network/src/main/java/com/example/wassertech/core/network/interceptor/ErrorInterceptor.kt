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
            throw NetworkException("Network error: ${e.message}", e)
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

