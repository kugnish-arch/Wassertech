package ru.wassertech.core.network.dto

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * DTO для синхронизации иконки (icons)
 */
data class SyncIconDto(
    val id: String,
    @SerializedName("pack_id") val packId: String? = null, // FK → icon_packs.id (может быть null, если не указан на сервере)
    val code: String,
    val label: String,
    @SerializedName("entity_type") val entityType: String? = null, // "SITE", "INSTALLATION", "COMPONENT", "ANY" (может быть null на сервере)
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("android_res_name") val androidResName: String? = null,
    @SerializedName("is_active")
    @JsonAdapter(BooleanFromIntTypeAdapter::class)
    val isActive: Boolean = true,
    val origin: String? = null, // "CRM" или "CLIENT"
    @SerializedName("created_by_user_id") val createdByUserId: String? = null, // FK → users.id
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long
)


