package ru.wassertech.client.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность отчёта для Room.
 * Соответствует таблице reports из backend_reports_module.md
 */
@Entity(
    tableName = "reports",
    indices = [
        Index("clientId"),
        Index("sessionId"),
        Index("installationId"),
        Index("isArchived")
    ]
)
data class ReportEntity(
    @PrimaryKey val id: String,
    val sessionId: String? = null,
    val clientId: String? = null,
    val siteId: String? = null,
    val installationId: String? = null,
    val fileName: String,
    val fileUrl: String? = null,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long? = null,
    val isArchived: Boolean = false,
    // Локальные поля для управления скачиванием
    val localFilePath: String? = null, // Путь к файлу на устройстве
    val isDownloaded: Boolean = false // Файл скачан или нет
)

