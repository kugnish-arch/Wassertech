package com.example.wassertech.data

import androidx.room.*

@Entity(
    tableName = "observations",
    foreignKeys = [
        ForeignKey(entity = MaintenanceSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ComponentEntity::class, parentColumns = ["id"], childColumns = ["componentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("componentId"), Index("fieldKey")]
)
data class ObservationEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val componentId: String,
    val fieldKey: String,
    val valueBool: Boolean? = null,
    val valueNumber: Double? = null,
    val valueText: String? = null
)
