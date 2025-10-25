package com.example.wassertech.data

import androidx.room.*

@Entity(
    tableName = "installations",
    foreignKeys = [ForeignKey(
        entity = SiteEntity::class,
        parentColumns = ["id"],
        childColumns = ["siteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("siteId"), Index("name")]
)
data class InstallationEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String
)
