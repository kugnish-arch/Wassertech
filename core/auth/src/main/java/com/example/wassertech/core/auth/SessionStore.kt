package ru.wassertech.core.auth

/**
 * Интерфейс для хранения данных сессии пользователя
 */
interface SessionStore {
    /**
     * Получить ID текущего пользователя
     */
    fun getUserId(): String?
    
    /**
     * Сохранить ID пользователя
     */
    fun saveUserId(userId: String)
    
    /**
     * Получить имя пользователя
     */
    fun getUsername(): String?
    
    /**
     * Сохранить имя пользователя
     */
    fun saveUsername(username: String)
    
    /**
     * Очистить данные сессии
     */
    fun clearSession()
    
    /**
     * Проверить, есть ли активная сессия
     */
    fun hasSession(): Boolean = getUserId() != null
}

