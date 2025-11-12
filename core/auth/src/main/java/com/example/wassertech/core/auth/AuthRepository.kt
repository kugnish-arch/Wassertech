package ru.wassertech.core.auth

/**
 * Интерфейс репозитория для авторизации
 */
interface AuthRepository {
    /**
     * Вход в систему
     * @param username имя пользователя
     * @param password пароль
     * @return результат авторизации
     */
    suspend fun login(username: String, password: String): AuthResult
    
    /**
     * Выход из системы
     */
    suspend fun logout()
    
    /**
     * Обновить access token используя refresh token
     * @return результат обновления токена
     */
    suspend fun refreshToken(): TokenRefreshResult
    
    /**
     * Проверить, авторизован ли пользователь
     */
    suspend fun isAuthenticated(): Boolean
}

/**
 * Результат авторизации
 */
sealed class AuthResult {
    data class Success(
        val accessToken: String,
        val refreshToken: String,
        val userId: String,
        val username: String
    ) : AuthResult()
    
    data class Error(
        val message: String,
        val code: Int? = null
    ) : AuthResult()
}

/**
 * Результат обновления токена
 */
sealed class TokenRefreshResult {
    data class Success(val accessToken: String) : TokenRefreshResult()
    data class Error(val message: String) : TokenRefreshResult()
}

