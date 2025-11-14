package ru.wassertech.core.network.dto

/**
 * Запрос на отправку локальных изменений на сервер
 */
data class SyncPushRequest(
    val clients: List<SyncClientDto> = emptyList(),
    val sites: List<SyncSiteDto> = emptyList(),
    val installations: List<SyncInstallationDto> = emptyList(),
    val components: List<SyncComponentDto> = emptyList(),
    val maintenance_sessions: List<SyncMaintenanceSessionDto> = emptyList(),
    val maintenance_values: List<SyncMaintenanceValueDto> = emptyList(),
    val component_templates: List<SyncComponentTemplateDto> = emptyList(),
    val component_template_fields: List<SyncChecklistFieldDto> = emptyList(), // Используем SyncChecklistFieldDto для совместимости структуры
    val deleted: List<DeletedRecordDto> = emptyList()
)
