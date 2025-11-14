package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность сессии технического обслуживания.
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 * 
 * Примечание: поле `synced` оставлено для обратной совместимости,
 * но рекомендуется использовать `syncStatus` вместо него.
 */
@Entity(
    tableName = "maintenance_sessions",
    indices = [
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
data class MaintenanceSessionEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val installationId: String? = null,
    val startedAtEpoch: Long,
    val finishedAtEpoch: Long? = null,
    val technician: String? = null,
    val notes: String? = null,
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0, // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
    // Legacy поле (для обратной совместимости)
    val synced: Boolean = false
)

