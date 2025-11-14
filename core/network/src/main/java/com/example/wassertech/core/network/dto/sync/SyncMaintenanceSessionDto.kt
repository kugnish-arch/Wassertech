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
    // Для /sync/push: вложенные values (опционально)
    val values: List<SyncMaintenanceValueDto>? = null
)

