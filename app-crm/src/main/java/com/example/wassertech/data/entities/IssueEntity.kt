package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.wassertech.data.types.Severity

@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val componentId: String? = null,
    val description: String,
    val severity: Severity
)