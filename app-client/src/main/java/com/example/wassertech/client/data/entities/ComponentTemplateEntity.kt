package ru.wassertech.client.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Editable "component type" stored in DB.
 * Do not confuse with ChecklistTemplateEntity (service checklist schema).
 */
@Entity(
    tableName = "component_templates",
    indices = [
        Index("origin"),
        Index("created_by_user_id")
    ]
)
data class ComponentTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,                  // e.g. "Filter", "Softener", "RO", "Valve"
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val defaultParamsJson: String? = null,  // JSON of default key-value params for instances
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis(),
    // Поля для ролей и владения данными
    val ownerClientId: String? = null, // FK → clients.id
    val origin: String? = null, // "CRM" или "CLIENT"
    @ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null // FK → users.id
) {
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     * Переименовано из getOrigin() чтобы избежать конфликта с Room (Room генерирует getOrigin() для поля origin).
     */
    fun getOriginType(): ru.wassertech.core.auth.OriginType {
        return ru.wassertech.core.auth.OriginType.fromString(origin)
    }
}
