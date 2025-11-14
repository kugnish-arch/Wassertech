package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации компонента
 */
data class SyncComponentDto(
    val id: String,
    val installationId: String,
    val name: String,
    val type: String, // COMMON, HEAD и т.д.
    val orderIndex: Int = 0,
    val templateId: String? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null
)

