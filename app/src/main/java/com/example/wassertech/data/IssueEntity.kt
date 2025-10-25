package com.example.wassertech.data

import androidx.room.*

@Entity(
    tableName = "issues",
    foreignKeys = [
        ForeignKey(entity = MaintenanceSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ComponentEntity::class, parentColumns = ["id"], childColumns = ["componentId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("sessionId"), Index("componentId")]
)
data class IssueEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val componentId: String?,
    val description: String,
    val severity: Severity = Severity.MEDIUM
)
