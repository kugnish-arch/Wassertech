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
    val archivedAtEpoch: Long? = null,
    // Поля для ролей и владения данными
    val origin: String? = null, // "CRM" или "CLIENT"
    val created_by_user_id: String? = null, // FK → users.id
    @com.google.gson.annotations.SerializedName("icon_id") val iconId: String? = null // FK → icons.id
)

