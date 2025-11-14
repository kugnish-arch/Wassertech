package ru.wassertech.core.auth.impl

import ru.wassertech.core.auth.AuthRepository
import ru.wassertech.core.auth.AuthResult
import ru.wassertech.core.auth.TokenRefreshResult
import ru.wassertech.core.network.TokenStorage
import ru.wassertech.core.auth.SessionStore
import kotlinx.coroutines.delay

/**
 * Мок-реализация AuthRepository для тестирования и разработки
 */
class MockAuthRepository(
    private val tokenStorage: TokenStorage,
    private val sessionStore: SessionStore
) : AuthRepository {
    
    override suspend fun login(username: String, password: String): AuthResult {
        // Имитация сетевой задержки
        delay(1000)
        
        // Мок-валидация (для демонстрации)
        if (username.isBlank() || password.isBlank()) {
            return AuthResult.Error("Username and password are required", 400)
        }
        
        // Генерируем мок-токены
        val accessToken = "mock_access_token_${System.currentTimeMillis()}"
        val refreshToken = "mock_refresh_token_${System.currentTimeMillis()}"
        val userId = "user_${username.hashCode()}"
        
        // Сохраняем токены и данные сессии
        tokenStorage.saveAccessToken(accessToken)
        tokenStorage.saveRefreshToken(refreshToken)
        sessionStore.saveUserId(userId)
        sessionStore.saveUsername(username)
        
        return AuthResult.Success(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            username = username
        )
    }
    
    override suspend fun logout() {
        delay(500)
        tokenStorage.clearTokens()
        sessionStore.clearSession()
    }
    
    override suspend fun refreshToken(): TokenRefreshResult {
        delay(500)
        
        val refreshToken = tokenStorage.getRefreshToken()
        if (refreshToken == null) {
            return TokenRefreshResult.Error("No refresh token available")
        }
        
        // Генерируем новый access token
        val newAccessToken = "mock_access_token_${System.currentTimeMillis()}"
        tokenStorage.saveAccessToken(newAccessToken)
        
        return TokenRefreshResult.Success(newAccessToken)
    }
    
    override suspend fun isAuthenticated(): Boolean {
        delay(100)
        return tokenStorage.hasToken() && sessionStore.hasSession()
    }
}

