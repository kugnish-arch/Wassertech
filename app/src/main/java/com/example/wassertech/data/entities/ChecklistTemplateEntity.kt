package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wassertech.data.types.ComponentType

@Entity(tableName = "checklist_templates")
data class ChecklistTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,

    /** Legacy binding by enum type (kept for compatibility) */
    val componentType: ComponentType,

    /** Optional link to component template (future use) */
    val componentTemplateId: String? = null,

    /** ---------- Новые поля ---------- */

    /** Порядок сортировки (чем меньше значение — тем выше в списке). null = в конце */
    val sortOrder: Int? = null,

    /** Признак архивации шаблона */
    val isArchived: Boolean = false,

    /** Время архивации в миллисекундах (или null, если не архивирован) */
    val archivedAtEpoch: Long? = null,

    /** Время последнего обновления (для синхронизации и сортировки) */
    val updatedAtEpoch: Long? = null
)
