package ru.wassertech.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.wassertech.data.types.ComponentType

@Entity(
    tableName = "components",
    indices = [
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
/**
 * Сущность компонента установки.
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
data class ComponentEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val name: String,
    val type: ComponentType,
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
    val orderIndex: Int,
    @ColumnInfo(name = "templateId") val templateId: String? = null
)
