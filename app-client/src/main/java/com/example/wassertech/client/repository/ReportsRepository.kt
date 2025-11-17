package ru.wassertech.client.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import ru.wassertech.client.api.ApiConfig
import ru.wassertech.client.api.ReportsApi
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.ReportEntity
import ru.wassertech.core.auth.SessionManager
import ru.wassertech.core.auth.UserRole
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.auth.DataStoreTokenStorage
import java.io.File

/**
 * Репозиторий для работы с отчётами в app-client.
 * Используется для синхронизации списка отчётов и скачивания PDF-файлов.
 */
class ReportsRepository(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val reportsDao = database.reportsDao()
    private val tokenStorage = DataStoreTokenStorage(context)
    
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
     * Синхронизирует список отчётов с сервером для текущего клиента.
     * 
     * @return Result.success(Unit) при успехе, Result.failure(Exception) при ошибке
     */
    suspend fun syncReportsForCurrentClient(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Начинаем синхронизацию отчётов")
            
            // Получаем текущую сессию
            val session = SessionManager.getInstance(context).getCurrentSession()
            if (session == null) {
                val error = IllegalStateException("Пользователь не авторизован")
                Log.e(TAG, error.message ?: "Пользователь не авторизован")
                return@withContext Result.failure(error)
            }
            
            // Проверяем роль
            if (session.role != UserRole.CLIENT) {
                val error = IllegalStateException("Синхронизация отчётов доступна только для роли CLIENT")
                Log.e(TAG, error.message ?: "Неверная роль")
                return@withContext Result.failure(error)
            }
            
            // Получаем clientId
            val clientId = session.clientId
            if (clientId.isNullOrBlank()) {
                val error = IllegalStateException("client_id не установлен для пользователя CLIENT")
                Log.e(TAG, error.message ?: "client_id не установлен")
                return@withContext Result.failure(error)
            }
            
            Log.d(TAG, "Синхронизация отчётов для clientId=$clientId")
            
            // Получаем последний updatedAtEpoch для инкрементальной синхронизации
            val lastUpdatedAtEpoch = reportsDao.getMaxUpdatedAtEpoch(clientId)
            Log.d(TAG, "Последний updatedAtEpoch: $lastUpdatedAtEpoch")
            
            // Выполняем запрос к API
            val response = api.getReports(sinceUpdatedAtEpoch = lastUpdatedAtEpoch)
            
            if (response.isSuccessful) {
                val reportsDto = response.body()
                if (reportsDto != null) {
                    Log.d(TAG, "Получено ${reportsDto.size} отчётов с сервера")
                    
                    // Преобразуем DTO в Entity
                    val reportsEntity = reportsDto.map { dto ->
                        ReportEntity(
                            id = dto.id,
                            sessionId = dto.sessionId,
                            clientId = dto.clientId ?: clientId, // Используем clientId из сессии, если в DTO null
                            siteId = dto.siteId,
                            installationId = dto.installationId,
                            fileName = dto.fileName,
                            fileUrl = dto.fileUrl,
                            createdAtEpoch = dto.createdAtEpoch,
                            updatedAtEpoch = dto.updatedAtEpoch ?: dto.createdAtEpoch,
                            isArchived = dto.isArchived != 0,
                            localFilePath = null, // На этом шаге файл ещё не скачан
                            isDownloaded = false
                        )
                    }
                    
                    // Сохраняем в БД (upsert)
                    reportsDao.insertOrUpdateReports(reportsEntity)
                    Log.d(TAG, "Отчёты сохранены в БД")
                    
                    Result.success(Unit)
                } else {
                    val error = IllegalStateException("Пустой ответ от сервера")
                    Log.e(TAG, error.message ?: "Пустой ответ от сервера")
                    Result.failure(error)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                val error = HttpException(response)
                Log.e(TAG, "Ошибка синхронизации отчётов: HTTP ${response.code()}, body=$errorBody")
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при синхронизации отчётов", e)
            Result.failure(e)
        }
    }
    
    /**
     * Скачивает недостающие PDF-файлы для текущего клиента.
     * 
     * @param onProgress Callback для отслеживания прогресса (current, total)
     * 
     * @return Result.success(Unit) при успехе, Result.failure(Exception) при ошибке
     */
    suspend fun downloadMissingReportsForCurrentClient(
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Начинаем скачивание недостающих отчётов")
            
            // Получаем текущую сессию
            val session = SessionManager.getInstance(context).getCurrentSession()
            if (session == null) {
                val error = IllegalStateException("Пользователь не авторизован")
                Log.e(TAG, error.message ?: "Пользователь не авторизован")
                return@withContext Result.failure(error)
            }
            
            // Получаем clientId
            val clientId = session.clientId
            if (clientId.isNullOrBlank()) {
                val error = IllegalStateException("client_id не установлен для пользователя CLIENT")
                Log.e(TAG, error.message ?: "client_id не установлен")
                return@withContext Result.failure(error)
            }
            
            // Получаем список отчётов для скачивания
            val reportsToDownload = reportsDao.getReportsToDownload(clientId)
            
            if (reportsToDownload.isEmpty()) {
                Log.d(TAG, "Нет отчётов для скачивания")
                return@withContext Result.success(Unit)
            }
            
            val total = reportsToDownload.size
            Log.d(TAG, "Найдено $total отчётов для скачивания")
            
            // Создаём директорию для сохранения файлов
            val reportsDir = File(context.filesDir, "reports/$clientId").apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            
            // Создаём HTTP-клиент для скачивания файлов
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val token = tokenStorage.getAccessToken()
            if (token == null) {
                val error = IllegalStateException("Токен авторизации отсутствует")
                Log.e(TAG, error.message ?: "Токен отсутствует")
                return@withContext Result.failure(error)
            }
            
            var current = 0
            var hasErrors = false
            
            // Скачиваем каждый отчёт
            for (report in reportsToDownload) {
                try {
                    val fileUrl = report.fileUrl
                    if (fileUrl.isNullOrBlank()) {
                        Log.w(TAG, "Пропускаем отчёт ${report.id}: fileUrl пустой")
                        current++
                        onProgress(current, total)
                        continue
                    }
                    
                    // Формируем полный URL
                    val baseUrl = ApiConfig.getBaseUrl().removeSuffix("/")
                    val fullUrl = if (fileUrl.startsWith("http")) {
                        fileUrl
                    } else {
                        "$baseUrl/$fileUrl"
                    }
                    
                    Log.d(TAG, "Скачиваем отчёт ${report.id} из $fullUrl")
                    
                    // Создаём запрос
                    val request = Request.Builder()
                        .url(fullUrl)
                        .header("Authorization", "Bearer $token")
                        .get()
                        .build()
                    
                    // Выполняем запрос
                    val response = httpClient.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body
                        if (responseBody != null) {
                            // Сохраняем файл
                            val localFile = File(reportsDir, "${report.id}.pdf")
                            localFile.outputStream().use { output ->
                                responseBody.byteStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            
                            // Обновляем запись в БД
                            reportsDao.updateReportDownloadStatus(
                                reportId = report.id,
                                localFilePath = localFile.absolutePath
                            )
                            
                            Log.d(TAG, "Отчёт ${report.id} успешно скачан: ${localFile.absolutePath}")
                        } else {
                            Log.w(TAG, "Пустое тело ответа для отчёта ${report.id}")
                            hasErrors = true
                        }
                    } else {
                        Log.w(TAG, "Ошибка скачивания отчёта ${report.id}: HTTP ${response.code}")
                        hasErrors = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при скачивании отчёта ${report.id}", e)
                    hasErrors = true
                }
                
                current++
                onProgress(current, total)
            }
            
            if (hasErrors) {
                Log.w(TAG, "Скачивание завершено с ошибками")
                // Возвращаем успех, но с предупреждением - часть файлов могла быть скачана
            } else {
                Log.d(TAG, "Все отчёты успешно скачаны")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Исключение при скачивании отчётов", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получить Flow со списком отчётов для текущего клиента.
     */
    fun observeReportsForCurrentClient(): kotlinx.coroutines.flow.Flow<List<ReportEntity>>? {
        val session = SessionManager.getInstance(context).getCurrentSession()
        val clientId = session?.clientId
        return if (clientId != null) {
            reportsDao.observeReportsForClient(clientId)
        } else {
            null
        }
    }
}

