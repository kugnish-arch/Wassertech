package ru.wassertech.core.network.dto

/**
 * DTO для ответа /auth/me
 * Согласно REFACTORING_ROLES_AND_OWNERSHIP_FINAL.md, сервер возвращает clientId.
 */
data class UserMeResponse(
    val id: String,
    val login: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String, // "ADMIN", "ENGINEER", "CLIENT"
    val clientId: String? = null, // Для CLIENT обязательно, для ADMIN/ENGINEER может быть null
    val permissions: String? = null,
    val lastLoginAtEpoch: Long? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long
)

