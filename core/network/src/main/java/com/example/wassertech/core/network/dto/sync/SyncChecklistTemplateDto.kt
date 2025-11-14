package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации шаблона чек-листа
 */
data class SyncChecklistTemplateDto(
    val id: String,
    val title: String? = null, // Может быть null, если не указан
    val componentType: String, // COMMON, HEAD
    val componentTemplateId: String? = null,
    val sortOrder: Int = 0,
    val createdAtEpoch: Long? = null,
    val updatedAtEpoch: Long,
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null
)

