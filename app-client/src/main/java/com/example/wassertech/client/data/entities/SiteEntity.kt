package ru.wassertech.client.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sites",
    indices = [
        Index("isArchived"),
        Index("origin"),
        Index("created_by_user_id"),
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
data class SiteEntity(
    @PrimaryKey val id: String,
    val clientId: String, // TODO: Переименовать в ownerClientId после миграции БД
    val name: String,
    val address: String? = null,
    val orderIndex: Int = 0,
    @ColumnInfo(name = "icon_id") val iconId: String? = null, // FK → icons.id
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis(),
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0, // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
    // Поля для ролей и владения данными
    val ownerClientId: String? = null, // Если null, используем clientId
    val origin: String? = null, // "CRM" или "CLIENT", преобразуется в OriginType
    @ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null // FK → users.id
) {
    /**
     * Получает ownerClientId (использует ownerClientId если есть, иначе clientId для обратной совместимости).
     * Переименовано из getOwnerClientId() чтобы избежать конфликта с Room.
     */
    fun effectiveOwnerClientId(): String = ownerClientId ?: clientId
    
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     * Переименовано из getOrigin() чтобы избежать конфликта с Room (Room генерирует getOrigin() для поля origin).
     */
    fun getOriginType(): ru.wassertech.core.auth.OriginType {
        return ru.wassertech.core.auth.OriginType.fromString(origin)
    }
}