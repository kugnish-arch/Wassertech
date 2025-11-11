package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_sessions")
data class MaintenanceSessionEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val installationId: String? = null,
    val startedAtEpoch: Long,
    val finishedAtEpoch: Long? = null,
    val technician: String? = null,
    val notes: String? = null,
    val synced: Boolean = false
)

