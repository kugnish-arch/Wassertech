package com.example.wassertech.core.auth.impl

import com.example.wassertech.core.auth.SessionStore

/**
 * Мок-реализация SessionStore для тестирования и разработки
 * Хранит данные сессии в памяти
 */
class MockSessionStore : SessionStore {
    
    private var userId: String? = null
    private var username: String? = null
    
    override fun getUserId(): String? = userId
    
    override fun saveUserId(userId: String) {
        this.userId = userId
    }
    
    override fun getUsername(): String? = username
    
    override fun saveUsername(username: String) {
        this.username = username
    }
    
    override fun clearSession() {
        userId = null
        username = null
    }
    
    override fun hasSession(): Boolean = userId != null
}

