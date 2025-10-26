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

    /** NEW: bind checklist to a specific ComponentTemplateEntity when available */
    val componentTemplateId: String? = null
)
