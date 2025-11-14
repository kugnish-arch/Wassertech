package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter

/**
 * DTO для синхронизации объекта (Site)
 */
data class SyncSiteDto(
    val id: String,
    val clientId: String,
    val name: String,
    val address: String? = null,
    val orderIndex: Int = 0,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null
)

