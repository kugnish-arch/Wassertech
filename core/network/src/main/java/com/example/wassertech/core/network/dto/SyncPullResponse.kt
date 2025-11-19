package ru.wassertech.core.network.dto

import com.google.gson.annotations.SerializedName

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
    val component_templates: List<SyncComponentTemplateDto> = emptyList(),
    val component_template_fields: List<SyncChecklistFieldDto> = emptyList(), // Используем SyncChecklistFieldDto для совместимости структуры
    @SerializedName("icon_packs") val iconPacks: List<SyncIconPackDto> = emptyList(),
    val icons: List<SyncIconDto> = emptyList(),
    @SerializedName("user_membership") val userMembership: List<ru.wassertech.core.network.dto.sync.SyncUserMembershipDto>? = null,
    val deleted: List<DeletedRecordDto> = emptyList()
)

/**
 * Информация об удаленной записи
 * Поддерживает оба варианта: entity (новый формат) и tableName (старый формат для совместимости с сервером)
 */
data class DeletedRecordDto(
    val entity: String? = null,        // Имя сущности (clients, sites, installations, etc.) - новый формат
    val tableName: String? = null,     // Имя таблицы (для обратной совместимости с сервером)
    @SerializedName("id") val recordId: String,  // В JSON поле называется "id", но в коде используем recordId для ясности
    val deletedAtEpoch: Long
) {
    /**
     * Получить имя сущности (использует entity, если доступно, иначе tableName)
     */
    fun getEntityName(): String? = entity ?: tableName
}
