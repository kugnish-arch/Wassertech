package ru.wassertech.client.auth

import ru.wassertech.core.auth.UserRole

/**
 * Сессия текущего пользователя в app-client.
 * Содержит информацию о пользователе, полученную из JWT-токена.
 */
interface UserSession {
    /**
     * ID пользователя (из JWT sub claim).
     */
    val userId: String
    
    /**
     * Роль пользователя (из JWT role claim).
     * В app-client всегда должна быть UserRole.CLIENT.
     */
    val role: UserRole
    
    /**
     * ID клиента, к которому относится этот пользователь (из JWT client_id claim).
     * Соответствует clients.id в базе данных.
     */
    val clientId: String
}

/**
 * Реализация UserSession.
 */
data class UserSessionImpl(
    override val userId: String,
    override val role: UserRole,
    override val clientId: String
) : UserSession

/**
 * Менеджер для работы с текущей сессией пользователя.
 * TODO: Интегрировать с JWT-парсером для получения данных из токена.
 */
object UserSessionManager {
    private var currentSession: UserSession? = null
    
    /**
     * Устанавливает текущую сессию пользователя.
     * Вызывается после успешного логина, когда получен JWT-токен.
     */
    fun setCurrentSession(session: UserSession) {
        currentSession = session
    }
    
    /**
     * Получает текущую сессию пользователя.
     * @return текущая сессия или null, если пользователь не залогинен
     */
    fun getCurrentSession(): UserSession? {
        return currentSession
    }
    
    /**
     * Очищает текущую сессию (при выходе из системы).
     */
    fun clearSession() {
        currentSession = null
    }
    
    /**
     * Проверяет, залогинен ли пользователь.
     */
    fun isLoggedIn(): Boolean {
        return currentSession != null
    }
}

