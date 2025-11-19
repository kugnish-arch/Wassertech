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
     * Получить локальный путь к миниатюре иконки по ID.
     */
    suspend fun getLocalThumbnailPath(iconId: String): String? {
        val icon = iconDao.getById(iconId)
        // Сначала проверяем сохранённый путь в БД
        if (!icon?.thumbnailLocalPath.isNullOrBlank()) {
            val file = File(icon!!.thumbnailLocalPath!!)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        // Если путь в БД отсутствует или файл не найден, проверяем стандартное расположение
        val file = getIconFile(iconId, "thumbnail")
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
     * Загрузить миниатюру иконки с сервера и сохранить локально.
     */
    suspend fun downloadThumbnail(iconId: String, thumbnailUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Исправляем путь: заменяем publicuploads или public/uploads на uploads
            val correctedUrl = thumbnailUrl
                .replace("/publicuploads/", "/uploads/")
                .replace("/public/uploads/", "/uploads/")
            
            val fullUrl = if (correctedUrl.startsWith("http://") || correctedUrl.startsWith("https://")) {
                // Абсолютный URL
                correctedUrl
            } else {
                // Относительный URL - добавляем базовый URL
                val base = baseUrl.removeSuffix("/")
                val path = if (correctedUrl.startsWith("/")) correctedUrl else "/$correctedUrl"
                "$base$path"
            }
            
            Log.d(TAG, "Загрузка миниатюры иконки $iconId с URL: $fullUrl (исходный: $thumbnailUrl)")
            
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
            
            val file = getIconFile(iconId, "thumbnail")
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Обновляем путь в БД
            val icon = iconDao.getById(iconId)
            if (icon != null) {
                val updatedIcon = icon.copy(thumbnailLocalPath = file.absolutePath)
                iconDao.upsert(updatedIcon)
            }
            
            Log.d(TAG, "Миниатюра иконки $iconId загружена: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке миниатюры иконки $iconId", e)
            Result.failure(e)
        }
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
     * Загрузить изображения для нескольких паков.
     * 
     * @param packIds Список ID паков для загрузки
     * @param onProgress Колбэк для отслеживания прогресса: (currentPack, totalPacks, currentIcon, totalIcons)
     * @return Result.success(Unit) при успехе, Result.failure при ошибке
     */
    suspend fun downloadIconPacks(
        packIds: List<String>,
        onProgress: ((currentPack: Int, totalPacks: Int, currentIcon: Int, totalIcons: Int) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (packIds.isEmpty()) {
                return@withContext Result.success(Unit)
            }
            
            val totalPacks = packIds.size
            var currentPackIndex = 0
            
            // Подсчитываем общее количество иконок для прогресса
            var totalIcons = 0
            packIds.forEach { packId ->
                val icons = iconDao.getAllByPackId(packId)
                totalIcons += icons.count { icon ->
                    icon.imageUrl.isNullOrBlank().not() && icon.androidResName.isNullOrBlank()
                }
            }
            
            var currentIconIndex = 0
            
            packIds.forEach { packId ->
                currentPackIndex++
                
                // Загружаем пак с прогрессом
                val result = downloadPackImages(packId) { downloaded, total ->
                    currentIconIndex = downloaded
                    onProgress?.invoke(currentPackIndex, totalPacks, currentIconIndex, totalIcons)
                }
                
                if (result.isFailure) {
                    Log.w(TAG, "Ошибка при загрузке пака $packId: ${result.exceptionOrNull()?.message}")
                }
            }
            
            Log.d(TAG, "Загрузка паков завершена: $currentPackIndex/$totalPacks")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке паков", e)
            Result.failure(e)
        }
    }
    
    /**
     * Загрузить миниатюры для всех иконок, у которых есть thumbnailUrl, но нет локальной миниатюры.
     * Используется при синхронизации для предварительной загрузки миниатюр.
     */
    suspend fun downloadMissingThumbnails(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Получаем все иконки (включая неактивные) для загрузки миниатюр
            // Используем getAllActive() для получения только активных, так как миниатюры нужны только для активных иконок
            val allIcons = iconDao.getAllActive()
            var downloadedCount = 0
            var skippedCount = 0
            
            allIcons.forEach { icon ->
                // Пропускаем иконки без thumbnailUrl
                if (icon.thumbnailUrl.isNullOrBlank()) {
                    skippedCount++
                    return@forEach
                }
                
                // Пропускаем иконки с androidResName (встроенные ресурсы)
                if (!icon.androidResName.isNullOrBlank()) {
                    skippedCount++
                    return@forEach
                }
                
                // Проверяем, есть ли уже локальная миниатюра
                val existingPath = getLocalThumbnailPath(icon.id)
                if (existingPath != null) {
                    // Если путь есть в БД, но файл отсутствует, обновляем БД
                    val file = File(existingPath)
                    if (!file.exists()) {
                        val updatedIcon = icon.copy(thumbnailLocalPath = null)
                        iconDao.upsert(updatedIcon)
                    } else {
                        skippedCount++
                        return@forEach
                    }
                }
                
                // Загружаем миниатюру
                val result = downloadThumbnail(icon.id, icon.thumbnailUrl!!)
                if (result.isSuccess) {
                    downloadedCount++
                } else {
                    Log.w(TAG, "Не удалось загрузить миниатюру иконки ${icon.id}: ${result.exceptionOrNull()?.message}")
                }
            }
            
            Log.d(TAG, "Загрузка миниатюр завершена: загружено=$downloadedCount, пропущено=$skippedCount")
            Result.success(downloadedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке миниатюр", e)
            Result.failure(e)
        }
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
                    // Очищаем путь в БД
                    if (icon.thumbnailLocalPath != null) {
                        val updatedIcon = icon.copy(thumbnailLocalPath = null)
                        iconDao.upsert(updatedIcon)
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

