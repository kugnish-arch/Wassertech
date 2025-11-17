package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации шаблона компонента
 */
data class SyncComponentTemplateDto(
    val id: String,
    val name: String,
    val category: String? = null,
    val sortOrder: Int = 0,
    val defaultParamsJson: String? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    // Поля для ролей и владения данными
    val origin: String? = null, // "CRM" или "CLIENT"
    val created_by_user_id: String? = null // FK → users.id
)

