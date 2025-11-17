package ru.wassertech.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.wassertech.core.auth.DataStoreTokenStorage
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.ApiConfig
import ru.wassertech.core.ui.icons.IconDataSource
import ru.wassertech.core.ui.icons.IconPackSyncStatus
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.dao.IconDao
import ru.wassertech.data.dao.IconPackDao
import ru.wassertech.data.dao.IconPackSyncStatusDao
import ru.wassertech.data.entities.IconEntity
import ru.wassertech.data.entities.IconPackSyncStatusEntity
import java.io.File
import java.io.FileOutputStream

/**
 * Репозиторий для работы с иконками и загрузкой изображений.
 * Управляет загрузкой изображений иконок с сервера и их локальным кэшированием.
 * 
 * Реализует интерфейс IconDataSource для работы через общий API.
 */
class IconRepository(private val context: Context) : IconDataSource {
    
    private val database = AppDatabase.getInstance(context)
    private val iconPackDao: IconPackDao = database.iconPackDao()
    private val iconDao: IconDao = database.iconDao()
    private val syncStatusDao: IconPackSyncStatusDao = database.iconPackSyncStatusDao()
    
    private val tokenStorage = DataStoreTokenStorage(context)
    
    // Базовый URL для API запросов (содержит /api/public/)
    private val apiBaseUrl: String = ApiConfig.getBaseUrl().removeSuffix("/")
    
    // Базовый URL для загрузки файлов (без /public/, так как файлы лежат в /api/uploads/)
    private val baseUrl: String = apiBaseUrl.replace("/api/public/", "/api/").replace("/api/public", "/api")
    
    companion object {
        private const val TAG = "IconRepository"
        private const val ICONS_DIR_NAME = "icons"
    }
    
    /**
     * Получить директорию для хранения иконок.
     */
    private fun getIconsDirectory(): File {
        val iconsDir = File(context.filesDir, ICONS_DIR_NAME)
        if (!iconsDir.exists()) {
            iconsDir.mkdirs()
        }
        return iconsDir
    }
    
    /**
     * Получить локальный файл для иконки.
     */
    fun getIconFile(iconId: String, type: String = "image"): File {
        val iconsDir = getIconsDirectory()
        val extension = "png" // По умолчанию PNG, можно определить по URL
        return File(iconsDir, "${iconId}_$type.$extension")
    }
    
    /**
     * Получить локальный путь к изображению иконки.
     * @deprecated Используйте getLocalIconPath(iconId: String)
     */
    suspend fun getLocalIconPath(icon: IconEntity): String? {
        return getLocalIconPath(icon.id)
    }
    
    /**
     * Получить локальный путь к изображению иконки по ID.
     */
    override suspend fun getLocalIconPath(iconId: String): String? {
        val file = getIconFile(iconId, "image")
        return if (file.exists()) {
            file.absolutePath
        } else {
            null
        }
    }
    
    /**
     * Проверить, загружена ли иконка локально.
     */
    override suspend fun isIconDownloaded(iconId: String): Boolean {
        val file = getIconFile(iconId, "image")
        return file.exists() && file.length() > 0
    }
    
    /**
     * Загрузить изображение иконки с сервера и сохранить локально.
     * @deprecated Используйте downloadIconImage(iconId: String, imageUrl: String)
     */
    suspend fun downloadIconImage(icon: IconEntity): Result<File> {
        val imageUrl = icon.imageUrl ?: return Result.failure(
            IllegalArgumentException("imageUrl is null for icon ${icon.id}")
        )
        return downloadIconImage(icon.id, imageUrl)
    }
    
    /**
     * Загрузить изображение иконки с сервера и сохранить локально.
     */
    override suspend fun downloadIconImage(iconId: String, imageUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            
            // Исправляем путь: заменяем publicuploads или public/uploads на uploads
            val correctedUrl = imageUrl
                .replace("/publicuploads/", "/uploads/")
                .replace("/public/uploads/", "/uploads/")
            
            val fullUrl = if (correctedUrl.startsWith("http://") || correctedUrl.startsWith("https://")) {
                // Абсолютный URL
                correctedUrl
            } else {
                // Относительный URL - добавляем базовый URL
                // Убеждаемся, что baseUrl заканчивается на /, а correctedUrl начинается с /
                val base = baseUrl.removeSuffix("/")
                val path = if (correctedUrl.startsWith("/")) correctedUrl else "/$correctedUrl"
                "$base$path"
            }
            
            Log.d(TAG, "Загрузка изображения иконки $iconId с URL: $fullUrl (исходный: $imageUrl)")
            
            val okHttpClient = ApiClient.createOkHttpClient(tokenStorage, enableLogging = false)
            val request = Request.Builder()
                .url(fullUrl)
                .get()
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("HTTP ${response.code}: ${response.message}")
                )
            }
            
            val body = response.body ?: return@withContext Result.failure(
                Exception("Response body is null")
            )
            
            val file = getIconFile(iconId, "image")
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Изображение иконки $iconId загружено: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке изображения иконки $iconId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Загрузить изображения всех иконок из пака.
     */
    override suspend fun downloadPackImages(packId: String, onProgress: ((Int, Int) -> Unit)?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Используем getAllByPackId для получения всех иконок (включая неактивные)
            val allIcons = iconDao.getAllByPackId(packId)
            val activeIcons = iconDao.getByPackId(packId)
            
            Log.d(TAG, "Пак $packId: всего иконок в БД = ${allIcons.size}, активных = ${activeIcons.size}")
            
            if (allIcons.isEmpty()) {
                Log.w(TAG, "Пак $packId не содержит иконок в локальной БД. Возможно, нужно синхронизировать данные.")
                
                // Проверяем, есть ли вообще иконки в БД
                val totalIconsInDb = iconDao.getAllActive().size
                Log.d(TAG, "Всего активных иконок в БД: $totalIconsInDb")
                
                return@withContext Result.success(Unit)
            }
            
            val icons = allIcons
            
            var downloadedCount = 0
            var totalCount = 0
            var skippedNoUrl = 0
            var skippedAndroidRes = 0
            var skippedAlreadyDownloaded = 0
            
            // Подсчитываем только те иконки, которые нужно загружать
            icons.forEach { icon ->
                if (icon.imageUrl.isNullOrBlank()) {
                    skippedNoUrl++
                    Log.d(TAG, "Пропуск иконки ${icon.id} (${icon.label}): нет imageUrl")
                    return@forEach
                }
                if (!icon.androidResName.isNullOrBlank()) {
                    skippedAndroidRes++
                    Log.d(TAG, "Пропуск иконки ${icon.id} (${icon.label}): есть androidResName (встроенный ресурс)")
                    return@forEach
                }
                totalCount++
            }
            
            Log.d(TAG, "Пак $packId: нужно загрузить $totalCount иконок (пропущено: без URL=$skippedNoUrl, встроенные=$skippedAndroidRes)")
            
            if (totalCount == 0) {
                Log.w(TAG, "Пак $packId: нет иконок для загрузки (все пропущены или уже загружены)")
                return@withContext Result.success(Unit)
            }
            
            icons.forEachIndexed { index, icon ->
                // Пропускаем иконки без imageUrl или с androidResName (встроенные ресурсы)
                if (icon.imageUrl.isNullOrBlank() || !icon.androidResName.isNullOrBlank()) {
                    return@forEachIndexed
                }
                
                // Пропускаем уже загруженные иконки
                if (isIconDownloaded(icon.id)) {
                    downloadedCount++
                    onProgress?.invoke(downloadedCount, totalCount)
                    return@forEachIndexed
                }
                
                val imageUrl = icon.imageUrl ?: return@forEachIndexed
                val result = downloadIconImage(icon.id, imageUrl)
                if (result.isSuccess) {
                    downloadedCount++
                } else {
                    Log.w(TAG, "Не удалось загрузить изображение иконки ${icon.id}: ${result.exceptionOrNull()?.message}")
                }
                
                onProgress?.invoke(downloadedCount, totalCount)
            }
            
            // Обновляем статус синхронизации
            val isFullyDownloaded = downloadedCount == totalCount
            val status = syncStatusDao.getByPackId(packId)
            
            if (status != null) {
                syncStatusDao.updateDownloadStatus(
                    packId = packId,
                    isDownloaded = isFullyDownloaded,
                    downloadedIcons = downloadedCount,
                    totalIcons = totalCount,
                    lastSyncEpoch = System.currentTimeMillis()
                )
            } else {
                syncStatusDao.upsert(
                    IconPackSyncStatusEntity(
                        packId = packId,
                        lastSyncEpoch = System.currentTimeMillis(),
                        isDownloaded = isFullyDownloaded,
                        totalIcons = totalCount,
                        downloadedIcons = downloadedCount
                    )
                )
            }
            
            Log.d(TAG, "Загрузка пака $packId завершена: $downloadedCount/$totalCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке изображений пака $packId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Получить статус синхронизации пака.
     */
    override suspend fun getPackSyncStatus(packId: String): IconPackSyncStatus? {
        val entity = syncStatusDao.getByPackId(packId)
        return entity?.let {
            IconPackSyncStatus(
                packId = it.packId,
                lastSyncEpoch = it.lastSyncEpoch,
                isDownloaded = it.isDownloaded,
                totalIcons = it.totalIcons,
                downloadedIcons = it.downloadedIcons
            )
        }
    }
    
    /**
     * Получить статус синхронизации пака как Room-сущность (для внутреннего использования).
     */
    suspend fun getPackSyncStatusEntity(packId: String): IconPackSyncStatusEntity? {
        return syncStatusDao.getByPackId(packId)
    }
    
    /**
     * Получить все загруженные паки.
     */
    suspend fun getDownloadedPacks(): List<String> {
        val statuses = syncStatusDao.getDownloadedPacks()
        return statuses.map { it.packId }
    }
    
    /**
     * Удалить локальные файлы иконок пака.
     */
    override suspend fun clearPackImages(packId: String) {
        withContext(Dispatchers.IO) {
            try {
                val icons = iconDao.getAllByPackId(packId)
                icons.forEach { icon ->
                    val file = getIconFile(icon.id, "image")
                    if (file.exists()) {
                        file.delete()
                    }
                    val thumbFile = getIconFile(icon.id, "thumbnail")
                    if (thumbFile.exists()) {
                        thumbFile.delete()
                    }
                }
                
                // Удаляем статус синхронизации
                syncStatusDao.deleteByPackId(packId)
                
                Log.d(TAG, "Локальные файлы пака $packId удалены")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при удалении локальных файлов пака $packId", e)
            }
        }
    }
}

