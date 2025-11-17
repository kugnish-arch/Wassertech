package ru.wassertech.core.network.dto

/**
 * DTO для синхронизации поля шаблона чек-листа
 */
data class SyncChecklistFieldDto(
    val id: String,
    val templateId: String,
    val key: String,
    val label: String? = null,
    val type: String, // TEXT, NUMBER, CHECKBOX и т.д.
    val unit: String? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val isForMaintenance: Boolean = true,
    val required: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtEpoch: Long? = null,
    val updatedAtEpoch: Long? = null
)


