package ru.wassertech.client.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.wassertech.client.data.types.ComponentType

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val name: String,
    val type: ComponentType,
    val orderIndex: Int,
    @ColumnInfo(name = "templateId") val templateId: String? = null
)
