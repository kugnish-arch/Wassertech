package com.example.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "clients",
    indices = [
        Index("name"),
        Index("phone"),
        Index("email"),
        Index("isArchived"),
        Index("sortOrder"),
        Index("clientGroupId") // ← ДОБАВИЛИ: быстрый выбор по группе
    ]
)

data class ClientEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val legalName: String? = null,
    val contactPerson: String? = null,
    val phone: String? = null,
    val phone2: String? = null,
    val email: String? = null,
    val addressFull: String? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val taxId: String? = null,
    val vatNumber: String? = null,
    val externalId: String? = null,
    val tagsJson: String? = null,
    val notes: String? = null,
    val isCorporate: Boolean = false,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val clientGroupId: String? = null
)