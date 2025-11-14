package ru.wassertech.core.network.dto

/**
 * Ответ на запрос получения изменений с сервера
 */
data class SyncPullResponse(
    val timestamp: Long,
    val clients: List<SyncClientDto> = emptyList(),
    val sites: List<SyncSiteDto> = emptyList(),
    val installations: List<SyncInstallationDto> = emptyList(),
    val components: List<SyncComponentDto> = emptyList(),
    val maintenance_sessions: List<SyncMaintenanceSessionDto> = emptyList(),
    val maintenance_values: List<SyncMaintenanceValueDto> = emptyList(),
    val checklist_templates: List<SyncChecklistTemplateDto> = emptyList(),
    val checklist_fields: List<SyncChecklistFieldDto> = emptyList(),
    val component_templates: List<SyncComponentTemplateDto> = emptyList(),
    val deleted: List<DeletedRecordDto> = emptyList()
)

/**
 * Информация об удаленной записи
 */
data class DeletedRecordDto(
    val tableName: String,
    val recordId: String,
    val deletedAtEpoch: Long
)

