package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "client_groups",
    indices = [
        Index("title"),
        Index("isArchived"),
        Index("sortOrder"),
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
/**
 * Сущность группы клиентов.
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
data class ClientGroupEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,                         // напр. "КП Кембридж"
    val notes: String? = null,
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
    val sortOrder: Int = 0                    // на будущее: ручная сортировка
)
