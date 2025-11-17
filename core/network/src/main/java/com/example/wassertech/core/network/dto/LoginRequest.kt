package ru.wassertech.core.network.dto

/**
 * DTO для запроса авторизации
 */
data class LoginRequest(
    val login: String,
    val password: String
)


