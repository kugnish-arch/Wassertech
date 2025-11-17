package ru.wassertech.core.ui.icons

import java.io.File

/**
 * Интерфейс для работы с иконками: загрузка, кеширование, получение локальных путей.
 * 
 * Реализации этого интерфейса должны быть в app-модулях (app-crm, app-client),
 * так как они зависят от конкретных Room-сущностей и AppDatabase.
 */
interface IconDataSource {
    /**
     * Получить локальный путь к изображению иконки, если оно загружено.
     * 
     * @param iconId ID иконки
     * @return Абсолютный путь к файлу или null, если файл не загружен
     */
    suspend fun getLocalIconPath(iconId: String): String?
    
    /**
     * Проверить, загружена ли иконка локально.
     * 
     * @param iconId ID иконки
     * @return true, если файл существует и не пустой
     */
    suspend fun isIconDownloaded(iconId: String): Boolean
    
    /**
     * Загрузить изображение иконки с сервера и сохранить локально.
     * 
     * @param iconId ID иконки
     * @param imageUrl URL изображения на сервере
     * @return Result с File, если загрузка успешна, или Result.failure с ошибкой
     */
    suspend fun downloadIconImage(iconId: String, imageUrl: String): Result<File>
    
    /**
     * Загрузить изображения всех иконок из пака.
     * 
     * @param packId ID пака иконок
     * @param onProgress Колбэк для отслеживания прогресса: (downloaded, total)
     * @return Result.success(Unit) при успехе, Result.failure при ошибке
     */
    suspend fun downloadPackImages(
        packId: String,
        onProgress: ((Int, Int) -> Unit)?
    ): Result<Unit>
    
    /**
     * Получить статус синхронизации пака.
     * 
     * @param packId ID пака
     * @return IconPackSyncStatusEntity или null, если статус не найден
     */
    suspend fun getPackSyncStatus(packId: String): IconPackSyncStatus?
    
    /**
     * Удалить локальные файлы иконок пака.
     * 
     * @param packId ID пака
     */
    suspend fun clearPackImages(packId: String)
}

/**
 * Статус синхронизации пака иконок.
 * Используется для отслеживания прогресса загрузки изображений.
 */
data class IconPackSyncStatus(
    val packId: String,
    val lastSyncEpoch: Long,
    val isDownloaded: Boolean,
    val totalIcons: Int,
    val downloadedIcons: Int
)

