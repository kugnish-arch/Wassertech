package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/**
 * Сущность для связи пользователей с установками/объектами/клиентами (для контроля доступа).
 * 
 * Структура соответствует таблице user_membership на сервере:
 * - PRIMARY KEY (user_id, scope, target_id)
 * - scope может быть: 'CLIENT', 'SITE', 'INSTALLATION'
 * - target_id - ID целевой сущности (клиента, объекта или установки)
 */
@Entity(
    tableName = "user_membership",
    primaryKeys = ["user_id", "scope", "target_id"],
    indices = [
        Index("user_id"),
        Index("scope"),
        Index("target_id"),
        Index("dirty_flag"),
        Index("sync_status")
    ]
)
data class UserMembershipEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    val scope: String, // "CLIENT", "SITE", "INSTALLATION"
    @ColumnInfo(name = "target_id") val targetId: String,
    @ColumnInfo(name = "created_at_epoch") val createdAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_epoch") val updatedAtEpoch: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "archived_at_epoch") val archivedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    @ColumnInfo(name = "dirty_flag") val dirtyFlag: Boolean = false,
    @ColumnInfo(name = "sync_status") val syncStatus: Int = 0 // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
)

