package ru.wassertech.core.network.dto.sync

import com.google.gson.annotations.SerializedName

/**
 * DTO для синхронизации user_membership с сервером.
 */
data class SyncUserMembershipDto(
    val id: String? = null, // Опционально, если сервер использует составной ключ
    @SerializedName("user_id") val userId: String,
    val scope: String, // "CLIENT", "SITE", "INSTALLATION"
    @SerializedName("target_id") val targetId: String,
    @SerializedName("created_at_epoch") val createdAtEpoch: Long = 0,
    @SerializedName("updated_at_epoch") val updatedAtEpoch: Long = 0,
    @SerializedName("is_archived") val isArchived: Boolean = false,
    @SerializedName("archived_at_epoch") val archivedAtEpoch: Long? = null
)

