package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Editable "component type" stored in DB.
 * Do not confuse with ChecklistTemplateEntity (service checklist schema).
 */
@Entity(tableName = "component_templates")
data class ComponentTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String?,                  // e.g. "Filter", "Softener", "RO", "Valve"
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val defaultParamsJson: String? = null,  // JSON of default key-value params for instances
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)
