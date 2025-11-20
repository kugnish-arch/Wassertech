package ru.wassertech.client.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность отчёта для Room.
 * Соответствует таблице reports из REPORTS_API_README.md
 */
@Entity(
    tableName = "reports",
    indices = [
        Index("clientId"),
        Index("siteId"),
        Index("sessionId"),
        Index("installationId"),
        Index("updatedAtEpoch"), // Для инкрементального sync
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
    val filePath: String? = null, // Путь до файла на диске (для совместимости)
    val fileSize: Long? = null, // Размер файла в байтах
    val mimeType: String? = null, // Тип содержимого, по умолчанию application/pdf
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long? = null,
    val createdByUserId: String? = null, // ID пользователя, загрузившего отчёт
    val isArchived: Boolean = false,
    // Локальные поля для управления скачиванием
    val localFilePath: String? = null, // Путь к файлу на устройстве
    val isDownloaded: Boolean = false // Файл скачан или нет
)



