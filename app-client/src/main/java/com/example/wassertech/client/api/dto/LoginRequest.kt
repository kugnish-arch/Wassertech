package ru.wassertech.client.api.dto

/**
 * DTO для запроса авторизации
 */
data class LoginRequest(
    val login: String,
    val password: String
)

