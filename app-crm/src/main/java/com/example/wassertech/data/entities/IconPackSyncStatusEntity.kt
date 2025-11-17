package ru.wassertech.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность для отслеживания статуса синхронизации и загрузки икон-пака.
 * Используется для отслеживания, какие паки полностью загружены локально.
 */
@Entity(
    tableName = "icon_pack_sync_status",
    indices = [
        Index("packId"),
        Index("isDownloaded")
    ]
)
data class IconPackSyncStatusEntity(
    @PrimaryKey val packId: String, // FK → icon_packs.id
    @ColumnInfo(name = "lastSyncEpoch") val lastSyncEpoch: Long = 0, // Время последней синхронизации
    @ColumnInfo(name = "isDownloaded") val isDownloaded: Boolean = false, // Все иконки пака загружены локально
    @ColumnInfo(name = "totalIcons") val totalIcons: Int = 0, // Общее количество иконок в паке
    @ColumnInfo(name = "downloadedIcons") val downloadedIcons: Int = 0 // Количество загруженных иконок
)

