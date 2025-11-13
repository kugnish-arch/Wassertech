package ru.wassertech.client.api.dto

/**
 * DTO для ответа авторизации
 */
data class LoginResponse(
    val token: String,
    val exp: Long
)

