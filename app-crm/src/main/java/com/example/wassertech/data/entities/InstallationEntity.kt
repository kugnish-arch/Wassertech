package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installations",
    indices = [
        Index("isArchived"),
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
/**
 * Сущность установки на объекте.
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
data class InstallationEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String,
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
    val orderIndex: Int = 0
)