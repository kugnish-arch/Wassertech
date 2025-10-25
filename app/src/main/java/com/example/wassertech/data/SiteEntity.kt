package com.example.wassertech.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sites",
    foreignKeys = [ForeignKey(
        entity = ClientEntity::class,
        parentColumns = ["id"],
        childColumns = ["clientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("clientId"), Index("name")]
)
data class SiteEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val name: String,
    val address: String? = null
)
