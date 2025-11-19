package ru.wassertech.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import ru.wassertech.api.ReportsApi
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.ApiConfig
import ru.wassertech.core.auth.DataStoreTokenStorage
import ru.wassertech.core.network.TokenStorage
import ru.wassertech.core.network.dto.ReportDto
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ReportEntity
import java.io.File

/**
 * Репозиторий для работы с отчётами в app-crm.
 * Используется для загрузки сгенерированных PDF-отчётов на сервер и работы с БД.
 */
class ReportsRepository(private val context: Context) {
    
    private val tokenStorage: TokenStorage = DataStoreTokenStorage(context)
    private val database = AppDatabase.getInstance(context)
    private val reportDao = database.reportDao()
    
    private val api: ReportsApi by lazy {
        ApiClient.createService<ReportsApi>(
            tokenStorage = tokenStorage,
            baseUrl = ApiConfig.getBaseUrl(),
            enableLogging = true
        )
    }
    
    companion object {
        private const val TAG = "ReportsRepository"
    }
    
    /**
     * Загружает PDF-отчёт на сервер для указанной сессии ТО и сохраняет в локальную БД.
     * 
     * @param sessionId UUID сессии ТО
     * @param pdfFile Файл PDF для загрузки
     * @param fileName Имя файла для отображения (опционально, если null - используется имя файла)
     * 
     * @return Result.success(ReportEntity) при успехе, Result.failure(Exception) при ошибке
     */
    suspend fun uploadReportForSession(
        sessionId: String,
        pdfFile: File,
        fileName: String? = null
    ): Result<ReportEntity> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начинаем загрузку отчёта для sessionId=$sessionId, file=${pdfFile.absolutePath}")
                
                // Проверяем, что файл существует
                if (!pdfFile.exists()) {
                    val error = IllegalStateException("PDF файл не существует: ${pdfFile.absolutePath}")
                    Log.e(TAG, error.message ?: "PDF файл не существует")
                    return@withContext Result.failure(error)
                }
                
                // Подготавливаем multipart данные
                val sessionIdBody = sessionId.toRequestBody("text/plain".toMediaType())
                val fileNameBody = (fileName ?: pdfFile.name).toRequestBody("text/plain".toMediaType())
                
                val requestFile = pdfFile.asRequestBody("application/pdf".toMediaType())
                val filePart = MultipartBody.Part.createFormData("file", pdfFile.name, requestFile)
                
                // Выполняем запрос
                val response = api.uploadReport(
                    sessionId = sessionIdBody,
                    fileName = fileNameBody,
                    file = filePart
                )
                
                if (response.isSuccessful) {
                    val reportDto = response.body()
                    if (reportDto != null) {
                        Log.d(TAG, "Отчёт успешно загружен: id=${reportDto.id}, fileUrl=${reportDto.fileUrl}")
                        
                        // Преобразуем DTO в Entity и сохраняем в БД
                        val reportEntity = reportDto.toEntity()
                        reportDao.insertOrUpdateReport(reportEntity)
                        Log.d(TAG, "Отчёт сохранён в локальную БД: id=${reportEntity.id}")
                        
                        Result.success(reportEntity)
                    } else {
                        val error = IllegalStateException("Пустой ответ от сервера")
                        Log.e(TAG, error.message ?: "Пустой ответ от сервера")
                        Result.failure(error)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    val error = HttpException(response)
                    Log.e(TAG, "Ошибка загрузки отчёта: HTTP ${response.code()}, body=$errorBody")
                    Result.failure(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при загрузке отчёта", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Маппинг ReportDto → ReportEntity
     */
    private fun ReportDto.toEntity(): ReportEntity {
        return ReportEntity(
            id = id,
            sessionId = sessionId,
            clientId = clientId,
            siteId = siteId,
            installationId = installationId,
            fileName = fileName,
            fileUrl = fileUrl,
            filePath = filePath,
            fileSize = fileSize,
            mimeType = mimeType,
            createdAtEpoch = createdAtEpoch,
            updatedAtEpoch = updatedAtEpoch ?: createdAtEpoch,
            createdByUserId = createdByUserId,
            isArchived = isArchived != 0
        )
    }
}

