package com.example.wassertech.data

import androidx.room.*

@Entity(
    tableName = "maintenance_sessions",
    foreignKeys = [
        ForeignKey(entity = SiteEntity::class, parentColumns = ["id"], childColumns = ["siteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = InstallationEntity::class, parentColumns = ["id"], childColumns = ["installationId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("siteId"), Index("installationId")]
)
data class MaintenanceSessionEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val installationId: String?,
    val startedAtEpoch: Long,
    val finishedAtEpoch: Long? = null,
    val technician: String? = null,
    val notes: String? = null,
    val synced: Boolean = false
)
