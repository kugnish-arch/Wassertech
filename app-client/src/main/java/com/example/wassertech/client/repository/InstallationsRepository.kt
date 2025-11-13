package ru.wassertech.client.repository

import android.content.Context
import android.util.Log
import retrofit2.HttpException
import ru.wassertech.client.api.ApiConfig
import ru.wassertech.client.api.WassertechApi
import ru.wassertech.client.api.dto.InstallationDto
import ru.wassertech.client.auth.AuthRepository
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.OfflineModeManager
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.core.network.ApiClient

/**
 * Репозиторий для работы с установками через API с сохранением в Room
 */
class InstallationsRepository(
    private val context: Context,
    private val authRepository: AuthRepository
) {
    
    private val api: WassertechApi by lazy {
        ApiClient.createService<WassertechApi>(
            tokenStorage = null, // Токен передаем вручную через заголовок
            baseUrl = ApiConfig.getBaseUrl(),
            enableLogging = true
        )
    }
    
    private val database = AppDatabase.getInstance(context)
    private val offlineModeManager = OfflineModeManager.getInstance(context)
    
    companion object {
        private const val TAG = "InstallationsRepository"
    }
    
    /**
     * Загрузить список установок (с сервера или из Room в зависимости от режима)
     */
    suspend fun loadInstallations(): List<InstallationDto> {
        val isOffline = offlineModeManager.isOfflineMode()
        
        if (isOffline) {
            // Оффлайн режим: читаем из Room
            Log.d(TAG, "Оффлайн режим: загрузка установок из Room")
            return loadInstallationsFromRoom()
        } else {
            // Онлайн режим: загружаем с сервера и сохраняем в Room
            Log.d(TAG, "Онлайн режим: загрузка установок с сервера")
            return loadInstallationsFromServer()
        }
    }
    
    /**
     * Загрузить установки с сервера и сохранить в Room
     */
    private suspend fun loadInstallationsFromServer(): List<InstallationDto> {
        val token = authRepository.getToken()
            ?: throw IllegalStateException("Пользователь не авторизован")
        
        return try {
            val response = api.getInstallations("Bearer $token")
            
            if (response.isSuccessful) {
                val installationsDto = response.body() ?: emptyList()
                Log.d(TAG, "Успешно загружено установок с сервера: ${installationsDto.size}")
                
                // Сохраняем в Room
                saveInstallationsToRoom(installationsDto)
                
                installationsDto
            } else {
                when (response.code()) {
                    401 -> {
                        Log.e(TAG, "Токен недействителен (401)")
                        throw HttpException(response)
                    }
                    403 -> {
                        Log.e(TAG, "Доступ запрещен (403)")
                        throw HttpException(response)
                    }
                    else -> {
                        Log.e(TAG, "Ошибка загрузки установок: ${response.code()}")
                        throw HttpException(response)
                    }
                }
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ошибка при загрузке установок", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке установок", e)
            throw e
        }
    }
    
    /**
     * Загрузить установки из Room
     */
    private suspend fun loadInstallationsFromRoom(): List<InstallationDto> {
        val installations = database.hierarchyDao().getAllNonArchivedInstallationsNow()
        Log.d(TAG, "Загружено установок из Room: ${installations.size}")
        return installations.map { entity ->
            InstallationDto(
                id = entity.id,
                siteId = entity.siteId,
                name = entity.name,
                orderIndex = entity.orderIndex
            )
        }
    }
    
    /**
     * Сохранить установки в Room и синхронизировать (удалить лишние)
     */
    private suspend fun saveInstallationsToRoom(installationsDto: List<InstallationDto>) {
        try {
            val installationsEntity = installationsDto.map { dto ->
                InstallationEntity(
                    id = dto.id,
                    siteId = dto.siteId,
                    name = dto.name,
                    orderIndex = dto.orderIndex,
                    isArchived = false,
                    archivedAtEpoch = null
                )
            }
            
            // Получаем множество ID установок с сервера
            val serverInstallationIds = installationsDto.map { it.id }.toSet()
            
            // Получаем все установки из Room
            val roomInstallations = database.hierarchyDao().getAllInstallationsNow()
            val roomInstallationIds = roomInstallations.map { it.id }.toSet()
            
            // Находим установки, которые есть в Room, но отсутствуют на сервере
            val installationsToDelete = roomInstallationIds - serverInstallationIds
            
            // Удаляем лишние установки из Room
            if (installationsToDelete.isNotEmpty()) {
                Log.d(TAG, "Удаление установок из Room, которых нет на сервере: ${installationsToDelete.size}")
                installationsToDelete.forEach { installationId ->
                    try {
                        database.hierarchyDao().deleteInstallation(installationId)
                        Log.d(TAG, "Удалена установка из Room: $installationId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при удалении установки $installationId из Room", e)
                    }
                }
            }
            
            // Используем upsert для обновления существующих и добавления новых
            installationsEntity.forEach { installation ->
                database.hierarchyDao().upsertInstallation(installation)
            }
            
            Log.d(TAG, "Синхронизация завершена. Сохранено установок в Room: ${installationsEntity.size}, удалено: ${installationsToDelete.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении установок в Room", e)
            // Не прерываем выполнение, если не удалось сохранить
        }
    }
    
    /**
     * Проверить, доступен ли онлайн режим
     */
    suspend fun isOnlineModeAvailable(): Boolean {
        return !offlineModeManager.isOfflineMode()
    }
}

