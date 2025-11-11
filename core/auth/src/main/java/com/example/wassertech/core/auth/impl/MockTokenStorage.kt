package com.example.wassertech.core.auth.impl

import com.example.wassertech.core.auth.TokenStorage

/**
 * Мок-реализация TokenStorage для тестирования и разработки
 * Хранит токены в памяти
 */
class MockTokenStorage : TokenStorage {
    
    private var accessToken: String? = null
    private var refreshToken: String? = null
    
    override fun getAccessToken(): String? = accessToken
    
    override fun saveAccessToken(token: String) {
        accessToken = token
    }
    
    override fun getRefreshToken(): String? = refreshToken
    
    override fun saveRefreshToken(token: String) {
        refreshToken = token
    }
    
    override fun clearTokens() {
        accessToken = null
        refreshToken = null
    }
    
    override fun hasToken(): Boolean = accessToken != null
}

