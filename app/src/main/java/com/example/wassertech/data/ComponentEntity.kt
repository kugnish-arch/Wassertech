package com.example.wassertech.data

import androidx.room.*

@Entity(
    tableName = "components",
    foreignKeys = [ForeignKey(
        entity = InstallationEntity::class,
        parentColumns = ["id"],
        childColumns = ["installationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("installationId"), Index("type")]
)
data class ComponentEntity(
    @PrimaryKey val id: String,
    val installationId: String,
    val name: String,
    val type: ComponentType
)
