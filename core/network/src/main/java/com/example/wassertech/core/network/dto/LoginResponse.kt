package ru.wassertech.core.network.dto

/**
 * DTO для ответа авторизации.
 * Согласно REFACTORING_ROLES_AND_OWNERSHIP_FINAL.md, сервер возвращает объект user с clientId.
 */
data class LoginResponse(
    val token: String,
    val exp: Long,
    val user: LoginUserDto? = null // Опционально для обратной совместимости
)

/**
 * DTO для объекта пользователя в ответе логина.
 */
data class LoginUserDto(
    val id: String,
    val login: String,
    val name: String? = null,
    val email: String? = null,
    val role: String, // "ADMIN", "ENGINEER", "CLIENT"
    val clientId: String? = null, // Для CLIENT обязательно, для ADMIN/ENGINEER может быть null
    val permissions: String? = null // JSON строка с правами доступа (опционально)
)

