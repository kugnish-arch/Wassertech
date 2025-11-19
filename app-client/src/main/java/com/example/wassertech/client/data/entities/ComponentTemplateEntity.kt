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
        Index("dirtyFlag"),
        Index("syncStatus"),
        Index("origin"),
        Index("created_by_user_id")
    ]
)
data class ComponentTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,                  // e.g. "Filter", "Softener", "RO", "Valve"
    val defaultParamsJson: String? = null,  // JSON of default key-value params for instances
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0, // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
    // Другие поля
    val sortOrder: Int = 0,
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
