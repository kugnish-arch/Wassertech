package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.wassertech.data.types.FieldType
import androidx.room.ColumnInfo

@Entity(tableName = "checklist_fields")
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
    val isForMaintenance: Boolean = true // ← новое поле: TRUE = участвует в ТО, FALSE = просто характеристика
)