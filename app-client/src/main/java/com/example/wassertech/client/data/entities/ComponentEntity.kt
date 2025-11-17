package ru.wassertech.client.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.wassertech.client.data.types.ComponentType

@Entity(
    tableName = "components",
    indices = [
        Index("origin"),
        Index("created_by_user_id"),
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
data class ComponentEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val name: String,
    val type: ComponentType,
    val orderIndex: Int,
    @ColumnInfo(name = "templateId") val templateId: String? = null,
    @ColumnInfo(name = "icon_id") val iconId: String? = null, // FK → icons.id
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0, // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
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
