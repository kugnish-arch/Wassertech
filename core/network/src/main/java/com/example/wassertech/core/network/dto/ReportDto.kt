package ru.wassertech.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для отчёта из API.
 * Соответствует структуре JSON-ответа из REPORTS_API_README.md
 */
data class ReportDto(
    val id: String,
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("clientId") val clientId: String? = null,
    @SerializedName("siteId") val siteId: String? = null,
    @SerializedName("installationId") val installationId: String? = null,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("fileUrl") val fileUrl: String? = null,
    @SerializedName("filePath") val filePath: String? = null, // Путь до файла на диске (для совместимости)
    @SerializedName("fileSize") val fileSize: Long? = null, // Размер файла в байтах
    @SerializedName("mimeType") val mimeType: String? = null, // Тип содержимого, по умолчанию application/pdf
    @SerializedName("createdAtEpoch") val createdAtEpoch: Long,
    @SerializedName("updatedAtEpoch") val updatedAtEpoch: Long? = null,
    @SerializedName("createdByUserId") val createdByUserId: String? = null, // ID пользователя, загрузившего отчёт
    @SerializedName("isArchived") val isArchived: Int = 0 // 0 = активен, 1 = архивирован
)



