package ru.wassertech.core.network.dto

/**
 * Ответ на запрос отправки изменений
 */
data class SyncPushResponse(
    val success: Boolean,
    val processed: ProcessedCounts? = null,
    val errors: List<SyncError> = emptyList()
)

/**
 * Количество обработанных записей по каждой сущности
 */
data class ProcessedCounts(
    val clients: Int = 0,
    val sites: Int = 0,
    val installations: Int = 0,
    val components: Int = 0,
    val maintenance_sessions: Int = 0,
    val maintenance_values: Int = 0,
    val checklist_templates: Int = 0,
    val checklist_fields: Int = 0,
    val component_templates: Int = 0
)

/**
 * Ошибка при синхронизации конкретной записи
 */
data class SyncError(
    val entityType: String,
    val entityId: String,
    val message: String
)

