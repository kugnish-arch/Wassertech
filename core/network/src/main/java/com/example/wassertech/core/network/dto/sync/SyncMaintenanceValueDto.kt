package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации значения ТО
 */
data class SyncMaintenanceValueDto(
    val id: String? = null, // Может отсутствовать при создании
    val sessionId: String,
    val siteId: String,
    val installationId: String? = null,
    val componentId: String,
    val fieldKey: String,
    val valueText: String? = null,
    val valueNumber: Double? = null, // Для NUMBER типа
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val valueBool: Boolean? = null,
    val createdAtEpoch: Long? = null,
    val updatedAtEpoch: Long? = null
)

