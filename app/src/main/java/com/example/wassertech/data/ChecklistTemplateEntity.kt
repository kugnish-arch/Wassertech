package com.example.wassertech.data

import androidx.room.*

@Entity(
    tableName = "checklist_templates",
    indices = [Index("componentType")]
)
data class ChecklistTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val componentType: ComponentType
)
