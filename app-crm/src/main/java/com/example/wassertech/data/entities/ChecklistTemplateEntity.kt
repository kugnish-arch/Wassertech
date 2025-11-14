package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import ru.wassertech.data.types.ComponentType

/**
 * Сущность шаблона чеклиста для компонентов.
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
@Entity(
    tableName = "checklist_templates",
    indices = [
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
data class ChecklistTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,

    /** Legacy binding by enum type (kept for compatibility) */
    val componentType: ComponentType,

    /** Optional link to component template (future use) */
    val componentTemplateId: String? = null,
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long? = null,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0, // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
    // Другие поля
    /** Порядок сортировки (чем меньше значение — тем выше в списке). null = в конце */
    val sortOrder: Int? = null
)
