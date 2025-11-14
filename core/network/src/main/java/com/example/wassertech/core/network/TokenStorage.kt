package ru.wassertech.core.network

/**
 * Интерфейс для хранения токенов авторизации
 */
interface TokenStorage {
    /**
     * Получить access token
     */
    fun getAccessToken(): String?
    
    /**
     * Сохранить access token
     */
    fun saveAccessToken(token: String)
    
    /**
     * Получить refresh token
     */
    fun getRefreshToken(): String?
    
    /**
     * Сохранить refresh token
     */
    fun saveRefreshToken(token: String)
    
    /**
     * Очистить все токены
     */
    fun clearTokens()
    
    /**
     * Проверить, есть ли сохраненный токен
     */
    fun hasToken(): Boolean = getAccessToken() != null
}

