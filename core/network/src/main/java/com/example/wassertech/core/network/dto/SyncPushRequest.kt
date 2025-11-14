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
    val checklist_templates: List<SyncChecklistTemplateDto> = emptyList(),
    val checklist_fields: List<SyncChecklistFieldDto> = emptyList(),
    val component_templates: List<SyncComponentTemplateDto> = emptyList()
)

