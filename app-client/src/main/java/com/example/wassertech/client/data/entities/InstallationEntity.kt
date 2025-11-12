package ru.wassertech.client.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "installations",
    indices = [
        Index("isArchived")
    ]
)
data class InstallationEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String,
    val orderIndex: Int = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null
)