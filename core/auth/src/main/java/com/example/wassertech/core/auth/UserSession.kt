package ru.wassertech.core.auth

/**
 * Сессия текущего пользователя.
 * Содержит информацию о пользователе, полученную из JWT-токена или локального хранилища.
 */
interface UserSession {
    /**
     * ID пользователя (из JWT sub claim или users.id).
     */
    val userId: String
    
    /**
     * Логин пользователя.
     */
    val login: String
    
    /**
     * Роль пользователя (из JWT role claim или users.role).
     */
    val role: UserRole
    
    /**
     * ID клиента, к которому относится этот пользователь (из JWT client_id claim или users.client_id).
     * Соответствует clients.id в базе данных.
     * null для ADMIN и ENGINEER, обязателен для CLIENT.
     */
    val clientId: String?
    
    /**
     * Имя пользователя (опционально).
     */
    val name: String?
    
    /**
     * Email пользователя (опционально).
     */
    val email: String?
    
    /**
     * Проверяет, является ли пользователь администратором.
     */
    fun isAdmin(): Boolean = role == UserRole.ADMIN
    
    /**
     * Проверяет, является ли пользователь инженером.
     */
    fun isEngineer(): Boolean = role == UserRole.ENGINEER || role == UserRole.USER || role == UserRole.VIEWER
    
    /**
     * Проверяет, является ли пользователь клиентом.
     */
    fun isClient(): Boolean = role == UserRole.CLIENT
    
    /**
     * Проверяет, может ли пользователь редактировать сущности (ADMIN и ENGINEER могут редактировать всё).
     */
    fun canEditAll(): Boolean = isAdmin() || isEngineer()
}

/**
 * Реализация UserSession.
 */
data class UserSessionImpl(
    override val userId: String,
    override val login: String,
    override val role: UserRole,
    override val clientId: String?,
    override val name: String? = null,
    override val email: String? = null
) : UserSession

