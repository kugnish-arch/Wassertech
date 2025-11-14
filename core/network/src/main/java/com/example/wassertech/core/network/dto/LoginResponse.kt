package ru.wassertech.core.network.dto

/**
 * DTO для ответа авторизации
 */
data class LoginResponse(
    val token: String,
    val exp: Long
)

