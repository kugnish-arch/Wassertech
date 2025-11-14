package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации установки
 */
data class SyncInstallationDto(
    val id: String,
    val siteId: String,
    val name: String,
    val orderIndex: Int = 0,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null
)

