package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации сессии ТО
 * В /sync/push может содержать вложенные values
 */
data class SyncMaintenanceSessionDto(
    val id: String,
    val siteId: String,
    val installationId: String? = null,
    val startedAtEpoch: Long,
    val finishedAtEpoch: Long? = null,
    val technician: String? = null,
    val notes: String? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    // Поля для ролей и владения данными
    val origin: String? = null, // "CRM" или "CLIENT"
    val created_by_user_id: String? = null, // FK → users.id
    // Для /sync/push: вложенные values (опционально)
    val values: List<SyncMaintenanceValueDto>? = null
)

