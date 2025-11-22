package ru.wassertech.core.network.dto

/**
 * Ответ на запрос отправки изменений
 */
data class SyncPushResponse(
    val success: Boolean,
    val processed: ProcessedCounts? = null,
    val result: PushResult? = null,
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
    val component_templates: Int = 0,
    val component_template_fields: Int = 0
)

/**
 * Результат обработки запроса push с детализацией по типам операций
 */
data class PushResult(
    val clients: EntityResult? = null,
    val sites: EntityResult? = null,
    val installations: EntityResult? = null,
    val components: EntityResult? = null,
    val maintenance_sessions: EntityResult? = null,
    val maintenance_values: EntityResult? = null,
    val component_templates: EntityResult? = null,
    val component_template_fields: EntityResult? = null
)

/**
 * Результат обработки сущностей одного типа
 */
data class EntityResult(
    val inserted: List<String> = emptyList(),
    val updated: List<String> = emptyList(),
    val skipped: List<String> = emptyList()
)

/**
 * Ошибка при синхронизации конкретной записи
 */
data class SyncError(
    val entityType: String,
    val entityId: String,
    val message: String
)




