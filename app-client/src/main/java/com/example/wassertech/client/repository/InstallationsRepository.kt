package ru.wassertech.client.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.wassertech.core.auth.DataStoreTokenStorage
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.ApiConfig
import ru.wassertech.core.network.api.SyncApi
import ru.wassertech.core.network.dto.SyncInstallationDto
import ru.wassertech.core.network.interceptor.NetworkException
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.client.data.entities.SettingsEntity
import ru.wassertech.core.auth.UserAuthService

/**
 * Репозиторий для работы с установками через SyncApi с сохранением в Room
 */
class InstallationsRepository(
    private val context: Context
) {
    
    private val syncApi: SyncApi by lazy {
        ApiClient.createService<SyncApi>(
            tokenStorage = DataStoreTokenStorage(context),
            baseUrl = ApiConfig.getBaseUrl(),
            enableLogging = true
        )
    }
    
    private val database = AppDatabase.getInstance(context)
    private val tokenStorage = DataStoreTokenStorage(context)
    
    companion object {
        private const val TAG = "InstallationsRepository"
    }
    
    /**
     * Загрузить список установок из Room (после синхронизации через SyncEngine)
     */
    suspend fun loadInstallations(): List<SyncInstallationDto> {
        return withContext(Dispatchers.IO) {
            val installations = database.hierarchyDao().getAllNonArchivedInstallationsNow()
            Log.d(TAG, "Загружено установок из Room: ${installations.size}")
            installations.map { entity ->
                SyncInstallationDto(
                    id = entity.id,
                    siteId = entity.siteId,
                    name = entity.name,
                    orderIndex = entity.orderIndex,
                    createdAtEpoch = entity.createdAtEpoch ?: 0L,
                    updatedAtEpoch = entity.updatedAtEpoch ?: 0L,
                    isArchived = entity.isArchived ?: false,
                    archivedAtEpoch = entity.archivedAtEpoch
                )
            }
        }
    }
    
    /**
     * Синхронизировать установки с сервером через SyncApi
     */
    suspend fun syncInstallations(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем наличие токена
                val token = tokenStorage.getAccessToken()
                if (token == null) {
                    return@withContext Result.failure(IllegalStateException("Пользователь не авторизован"))
                }
                
                // Получаем timestamp последней синхронизации (в миллисекундах)
                val lastSyncTimestampMs = database.settingsDao().getValueSync("last_sync_timestamp")?.toLongOrNull() ?: 0L
                // Backend ожидает timestamp в секундах и требует since > 0
                val lastSyncTimestampSec = if (lastSyncTimestampMs == 0L) 1L else lastSyncTimestampMs / 1000
                
                Log.d(TAG, "Синхронизация установок: since=$lastSyncTimestampSec")
                
                // Запрашиваем изменения
                val response = syncApi.syncPull(since = lastSyncTimestampSec)
                
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code()) {
                        401 -> "Токен авторизации недействителен"
                        403 -> "Доступ запрещен"
                        else -> "Ошибка синхронизации: ${response.code()}"
                    }
                    Log.e(TAG, errorMsg)
                    return@withContext Result.failure(HttpException(response))
                }
                
                val pullResponse = response.body()
                if (pullResponse == null) {
                    return@withContext Result.failure(IllegalStateException("Пустой ответ от сервера"))
                }
                
                // Применяем установки к Room
                pullResponse.installations.forEach { dto ->
                    applyInstallationToRoom(dto)
                }
                
                // Обновляем timestamp последней синхронизации
                val timestampMs = pullResponse.timestamp * 1000
                database.settingsDao().setValue(
                    SettingsEntity(
                        key = "last_sync_timestamp",
                        value = timestampMs.toString()
                    )
                )
                
                Log.d(TAG, "Синхронизация завершена. Загружено установок: ${pullResponse.installations.size}")
                Result.success(pullResponse.installations.size)
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP ошибка при синхронизации", e)
                Result.failure(e)
            } catch (e: NetworkException) {
                Log.e(TAG, "Сетевая ошибка при синхронизации", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при синхронизации", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Применить установку к Room (с проверкой версий)
     */
    private suspend fun applyInstallationToRoom(dto: SyncInstallationDto) {
        val existing = database.hierarchyDao().getInstallationNow(dto.id)
        val entity = InstallationEntity(
            id = dto.id,
            siteId = dto.siteId,
            name = dto.name,
            createdAtEpoch = dto.createdAtEpoch,
            updatedAtEpoch = dto.updatedAtEpoch,
            isArchived = dto.isArchived,
            archivedAtEpoch = dto.archivedAtEpoch,
            deletedAtEpoch = null,
            dirtyFlag = false,
            syncStatus = 0, // SYNCED
            orderIndex = dto.orderIndex
        )
        if (existing == null) {
            database.hierarchyDao().insertInstallation(entity)
        } else if (dto.updatedAtEpoch > existing.updatedAtEpoch) {
            database.hierarchyDao().upsertInstallation(entity)
        }
    }
    
    /**
     * Проверить, авторизован ли пользователь
     */
    suspend fun isAuthenticated(): Boolean {
        return UserAuthService.isLoggedIn(context)
    }
}

