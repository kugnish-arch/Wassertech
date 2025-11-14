package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import ru.wassertech.data.types.FieldType

/**
 * Сущность поля шаблона чеклиста.
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
@Entity(
    tableName = "checklist_fields",
    indices = [
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
data class ChecklistFieldEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val key: String,
    val label: String,
    val type: FieldType,
    val unit: String? = null,
    val min: Double? = null,
    val max: Double? = null,
    @ColumnInfo(name = "isForMaintenance", defaultValue = "1")
    val isForMaintenance: Boolean = true, // ← новое поле: TRUE = участвует в ТО, FALSE = просто характеристика
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0 // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
)