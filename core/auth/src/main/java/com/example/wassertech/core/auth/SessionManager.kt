package ru.wassertech.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Менеджер для работы с текущей сессией пользователя.
 * Хранит сессию в SharedPreferences для восстановления при перезапуске приложения.
 */
class SessionManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "wassertech_session",
        Context.MODE_PRIVATE
    )
    
    private var currentSession: UserSession? = null
    
    companion object {
        private const val TAG = "SessionManager"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LOGIN = "login"
        private const val KEY_ROLE = "role"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        
        @Volatile
        private var INSTANCE: SessionManager? = null
        
        /**
         * Получает экземпляр SessionManager для приложения.
         */
        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Устанавливает текущую сессию пользователя.
     * Вызывается после успешного логина, когда получен JWT-токен и данные пользователя.
     */
    fun setCurrentSession(session: UserSession) {
        currentSession = session
        saveSessionToPrefs(session)
        Log.d(TAG, "Сессия установлена: userId=${session.userId}, role=${session.role}, clientId=${session.clientId}")
    }
    
    /**
     * Получает текущую сессию пользователя.
     * Сначала пытается получить из памяти, затем из SharedPreferences.
     * @return текущая сессия или null, если пользователь не залогинен
     */
    fun getCurrentSession(): UserSession? {
        if (currentSession != null) {
            return currentSession
        }
        
        // Пытаемся восстановить из SharedPreferences
        val userId = prefs.getString(KEY_USER_ID, null)
        if (userId != null) {
            val login = prefs.getString(KEY_LOGIN, "") ?: ""
            val roleStr = prefs.getString(KEY_ROLE, null)
            val clientId = prefs.getString(KEY_CLIENT_ID, null)
            val name = prefs.getString(KEY_NAME, null)
            val email = prefs.getString(KEY_EMAIL, null)
            
            val role = UserRole.fromString(roleStr)
            
            currentSession = UserSessionImpl(
                userId = userId,
                login = login,
                role = role,
                clientId = clientId,
                name = name,
                email = email
            )
            
            Log.d(TAG, "Сессия восстановлена из SharedPreferences: userId=$userId, role=$role, clientId=$clientId")
            return currentSession
        }
        
        return null
    }
    
    /**
     * Очищает текущую сессию (при выходе из системы).
     */
    fun clearSession() {
        currentSession = null
        clearSessionFromPrefs()
        Log.d(TAG, "Сессия очищена")
    }
    
    /**
     * Проверяет, залогинен ли пользователь.
     */
    fun isLoggedIn(): Boolean {
        return getCurrentSession() != null
    }
    
    /**
     * Сохраняет сессию в SharedPreferences.
     */
    private fun saveSessionToPrefs(session: UserSession) {
        prefs.edit().apply {
            putString(KEY_USER_ID, session.userId)
            putString(KEY_LOGIN, session.login)
            putString(KEY_ROLE, session.role.name)
            putString(KEY_CLIENT_ID, session.clientId)
            putString(KEY_NAME, session.name)
            putString(KEY_EMAIL, session.email)
            apply()
        }
    }
    
    /**
     * Очищает сессию из SharedPreferences.
     */
    private fun clearSessionFromPrefs() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_LOGIN)
            remove(KEY_ROLE)
            remove(KEY_CLIENT_ID)
            remove(KEY_NAME)
            remove(KEY_EMAIL)
            apply()
        }
    }
}


