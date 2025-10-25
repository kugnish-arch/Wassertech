package com.example.wassertech.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clients",
    indices = [Index("name", unique = false)]
)
data class ClientEntity(
    @PrimaryKey val id: String, // UUID
    val name: String,
    val phone: String? = null,
    val notes: String? = null
)
