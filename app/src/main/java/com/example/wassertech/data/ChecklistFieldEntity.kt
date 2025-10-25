package com.example.wassertech.data

import androidx.room.*

@Entity(
    tableName = "checklist_fields",
    foreignKeys = [ForeignKey(
        entity = ChecklistTemplateEntity::class,
        parentColumns = ["id"],
        childColumns = ["templateId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("templateId"), Index("key", unique = false)]
)
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
