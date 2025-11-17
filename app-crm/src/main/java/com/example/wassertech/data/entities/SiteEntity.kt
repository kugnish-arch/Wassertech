package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sites",
    indices = [
        Index("isArchived"),
        Index("dirtyFlag"),
        Index("syncStatus"),
        Index("origin"),
        Index("created_by_user_id")
    ]
)
/**
 * Сущность объекта клиента (место установки систем).
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
data class SiteEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val name: String,
    val address: String? = null,
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0, // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
    // Другие поля
    val orderIndex: Int = 0,
    @androidx.room.ColumnInfo(name = "icon_id") val iconId: String? = null, // FK → icons.id
    // Поля для ролей и владения данными
    val origin: String? = null, // "CRM" или "CLIENT", по умолчанию "CRM" для старых данных
    @androidx.room.ColumnInfo(name = "created_by_user_id") val createdByUserId: String? = null // FK → users.id
) {
    /**
     * Получает OriginType (по умолчанию CRM для старых данных).
     */
    fun getOriginType(): ru.wassertech.core.auth.OriginType {
        return ru.wassertech.core.auth.OriginType.fromString(origin)
    }
}