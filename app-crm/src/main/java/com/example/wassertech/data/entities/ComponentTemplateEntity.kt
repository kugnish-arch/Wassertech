package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Editable "component type" stored in DB.
 * Do not confuse with ChecklistTemplateEntity (service checklist schema).
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
@Entity(
    tableName = "component_templates",
    indices = [
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
data class ComponentTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,                  // e.g. "Filter", "Softener", "RO", "Valve"
    val defaultParamsJson: String? = null,  // JSON of default key-value params for instances
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
    val sortOrder: Int = 0,
    val isHeadComponent: Boolean = false // Заглавный компонент (отображается в отчете иначе)
)
