package ru.wassertech.core.network.interceptor

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * Callback для обработки истечения сессии
 */
interface SessionExpiredCallback {
    fun onSessionExpired()
}

/**
 * Interceptor для обработки ошибок сети
 */
class ErrorInterceptor(
    private val sessionExpiredCallback: SessionExpiredCallback? = null
) : Interceptor {
    
    companion object {
        private const val TAG = "ErrorInterceptor"
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var last401Time = 0L
    private val minIntervalBetween401Events = 2000L // 2 секунды между событий
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        try {
            val response = chain.proceed(request)
            
            // Обработка HTTP ошибок
            when (response.code) {
                401 -> {
                    // Unauthorized - токен истек или невалиден
                    val currentTime = System.currentTimeMillis()
                    // Отправляем событие только если прошло достаточно времени с последнего события
                    // чтобы избежать спама при серии запросов
                    if (currentTime - last401Time > minIntervalBetween401Events) {
                        last401Time = currentTime
                        Log.w(TAG, "Получен HTTP 401, отправляем событие истечения сессии")
                        sessionExpiredCallback?.onSessionExpired()
                    }
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

