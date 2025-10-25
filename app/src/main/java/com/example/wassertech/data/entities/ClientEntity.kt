package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val isCorporate: Boolean = false
)
