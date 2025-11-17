package ru.wassertech.repository

import android.content.Context
import android.util.Log
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
import java.io.File

/**
 * Репозиторий для работы с отчётами в app-crm.
 * Используется для загрузки сгенерированных PDF-отчётов на сервер.
 */
class ReportsRepository(private val context: Context) {
    
    private val tokenStorage: TokenStorage = DataStoreTokenStorage(context)
    
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
     * Загружает PDF-отчёт на сервер для указанной сессии ТО.
     * 
     * @param sessionId UUID сессии ТО
     * @param pdfFile Файл PDF для загрузки
     * @param fileName Имя файла для отображения (опционально, если null - используется имя файла)
     * 
     * @return Result.success(ReportDto) при успехе, Result.failure(Exception) при ошибке
     */
    suspend fun uploadReportForSession(
        sessionId: String,
        pdfFile: File,
        fileName: String? = null
    ): Result<ReportDto> {
        return try {
            Log.d(TAG, "Начинаем загрузку отчёта для sessionId=$sessionId, file=${pdfFile.absolutePath}")
            
            // Проверяем, что файл существует
            if (!pdfFile.exists()) {
                val error = IllegalStateException("PDF файл не существует: ${pdfFile.absolutePath}")
                Log.e(TAG, error.message ?: "PDF файл не существует")
                return Result.failure(error)
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
                val report = response.body()
                if (report != null) {
                    Log.d(TAG, "Отчёт успешно загружен: id=${report.id}, fileUrl=${report.fileUrl}")
                    Result.success(report)
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

