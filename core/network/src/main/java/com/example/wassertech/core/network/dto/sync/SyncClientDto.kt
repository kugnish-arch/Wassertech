package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации клиента
 */
data class SyncClientDto(
    val id: String,
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
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isCorporate: Boolean = false,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val sortOrder: Int = 0,
    val clientGroupId: String? = null
)

