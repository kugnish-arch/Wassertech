package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sites",
    indices = [
        Index("isArchived")
    ]
)
data class SiteEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val name: String,
    val address: String? = null,
    val orderIndex: Int = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null
)