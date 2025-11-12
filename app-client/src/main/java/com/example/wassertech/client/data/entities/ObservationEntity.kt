package ru.wassertech.client.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val componentId: String,
    val fieldKey: String,
    val valueBool: Boolean? = null,
    val valueNumber: Double? = null,
    val valueText: String? = null
)