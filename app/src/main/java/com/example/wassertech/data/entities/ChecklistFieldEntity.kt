package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.wassertech.data.types.FieldType

@Entity(tableName = "checklist_fields")
data class ChecklistFieldEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val key: String,
    val label: String,
    val type: FieldType,
    val unit: String? = null,
    val min: Double? = null,
    val max: Double? = null
)