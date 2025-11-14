package ru.wassertech.core.network

/**
 * Конфигурация API
 */
object ApiConfig {
    /**
     * Базовый URL API
     */
    const val BASE_URL = "https://2024.wassertech.ru/api/public/"
    
    /**
     * Получить базовый URL (можно переопределить через BuildConfig в будущем)
     */
    fun getBaseUrl(): String = BASE_URL
}

