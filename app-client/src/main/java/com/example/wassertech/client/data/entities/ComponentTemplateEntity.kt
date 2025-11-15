package ru.wassertech.client.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Editable "component type" stored in DB.
 * Do not confuse with ChecklistTemplateEntity (service checklist schema).
 */
@Entity(tableName = "component_templates")
data class ComponentTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,                  // e.g. "Filter", "Softener", "RO", "Valve"
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val defaultParamsJson: String? = null,  // JSON of default key-value params for instances
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis(),
    // TODO: Добавить поля ownerClientId и origin после миграции БД
    val ownerClientId: String? = null, // FK → clients.id
    val origin: String? = null // "CRM" или "CLIENT"
) {
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     * Переименовано из getOrigin() чтобы избежать конфликта с Room (Room генерирует getOrigin() для поля origin).
     */
    fun getOriginType(): ru.wassertech.client.auth.OriginType {
        return ru.wassertech.client.auth.OriginType.fromString(origin)
    }
}
