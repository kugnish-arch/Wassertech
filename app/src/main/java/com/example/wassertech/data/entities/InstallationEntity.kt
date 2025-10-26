package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installations")
data class InstallationEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String,
    val orderIndex: Int = 0
)