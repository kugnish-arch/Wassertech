package ru.wassertech.core.network.dto

/**
 * DTO для ответа /auth/me
 */
data class UserMeResponse(
    val id: String,
    val login: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String,
    val permissions: String? = null,
    val lastLoginAtEpoch: Long? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long
)

