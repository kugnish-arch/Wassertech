package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: String,
    val clientId: String,
    val name: String,
    val address: String? = null,
    val orderIndex: Int = 0
)