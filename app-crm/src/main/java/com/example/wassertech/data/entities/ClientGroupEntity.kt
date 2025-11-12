package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "client_groups",
    indices = [
        Index("title"),
        Index("isArchived"),
        Index("sortOrder")
    ]
)
data class ClientGroupEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,                         // напр. "КП Кембридж"
    val notes: String? = null,

    val sortOrder: Int = 0,                    // на будущее: ручная сортировка
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,

    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)
