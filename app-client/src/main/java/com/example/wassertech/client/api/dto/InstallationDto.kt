package ru.wassertech.client.api.dto

/**
 * DTO для установки
 */
data class InstallationDto(
    val id: String,
    val siteId: String,
    val name: String,
    val orderIndex: Int
)

