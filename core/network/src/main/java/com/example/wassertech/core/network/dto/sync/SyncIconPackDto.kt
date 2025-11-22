package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * DTO для синхронизации пака иконок (icon_packs)
 */
data class SyncIconPackDto(
    val id: String,
    val code: String,
    val name: String,
    val description: String? = null,
    val folder: String? = null, // Подпапка для иконок (например, "water", "car", "home")
    @SerializedName("isBuiltin")
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isBuiltin: Boolean = false,
    @SerializedName("isPremium")
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isPremium: Boolean = false,
    val origin: String? = null, // "CRM" или "CLIENT"
    @SerializedName("created_by_user_id") val createdByUserId: String? = null, // FK → users.id
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long
)





