package ru.wassertech.client.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.wassertech.core.auth.DataStoreTokenStorage
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.ApiConfig
import ru.wassertech.core.network.api.SyncApi
import ru.wassertech.core.network.dto.SyncPullResponse
import ru.wassertech.core.network.dto.SyncPushRequest
import ru.wassertech.core.network.dto.SyncPushResponse
import ru.wassertech.core.network.dto.SyncSiteDto
import ru.wassertech.core.network.dto.SyncInstallationDto
import ru.wassertech.core.network.dto.SyncComponentDto
import ru.wassertech.core.network.dto.SyncIconPackDto
import ru.wassertech.core.network.dto.SyncIconDto
import ru.wassertech.core.network.interceptor.NetworkException
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.SiteEntity
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.client.data.entities.ComponentEntity
import ru.wassertech.client.data.entities.SettingsEntity
import ru.wassertech.client.data.types.ComponentType

/**
 * Движок синхронизации для app-client.
 * Синхронизирует все сущности (sites, installations, components и т.д.) с сервером через REST API.
 */
class SyncEngine(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val tokenStorage = DataStoreTokenStorage(context)
    private val settingsDao = database.settingsDao()
    
    private val syncApi: SyncApi by lazy {
        ApiClient.createService<SyncApi>(
            tokenStorage = tokenStorage,
            baseUrl = ApiConfig.getBaseUrl(),
            enableLogging = true
        )
    }
    
    companion object {
        private const val TAG = "SyncEngine"
        private const val SETTINGS_KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    }
    
    /**
     * Получение изменений с сервера
     */
    suspend fun syncPull(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало получения изменений с сервера")
                
                // Проверяем наличие токена перед синхронизацией
                val token = tokenStorage.getAccessToken()
                if (token == null) {
                    val errorMsg = "Токен авторизации отсутствует. Необходимо войти в систему."
                    Log.e(TAG, errorMsg)
                    return@withContext SyncResult(
                        success = false,
                        message = errorMsg
                    )
                }
                
                Log.d(TAG, "Токен найден: ${token.take(20)}... (длина: ${token.length})")
                
                // Получаем информацию о текущем пользователе для логирования и передачи client_id
                val currentSession = ru.wassertech.core.auth.SessionManager.getInstance(context).getCurrentSession()
                val userClientId = currentSession?.clientId
                val userRole = currentSession?.role
                val userId = currentSession?.userId
                Log.d(TAG, "=== ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ ===")
                Log.d(TAG, "userId=$userId")
                Log.d(TAG, "role=${userRole?.name} (enum: $userRole)")
                Log.d(TAG, "clientId=$userClientId")
                Log.d(TAG, "isClientRole=${userRole == ru.wassertech.core.auth.UserRole.CLIENT}")
                Log.d(TAG, "clientId.isNullOrBlank()=${userClientId.isNullOrBlank()}")
                
                // Определяем, нужно ли передавать client_id в запрос
                // Для роли CLIENT передаем client_id, если он доступен
                val shouldSendClientId = userRole == ru.wassertech.core.auth.UserRole.CLIENT && !userClientId.isNullOrBlank()
                val clientIdForRequest = if (shouldSendClientId) userClientId else null
                Log.d(TAG, "shouldSendClientId=$shouldSendClientId")
                Log.d(TAG, "clientIdForRequest=$clientIdForRequest")
                
                // Получаем timestamp последней синхронизации (в миллисекундах)
                var lastSyncTimestampMs = getLastSyncTimestamp() ?: 0L
                
                // Проверяем, что timestamp разумен (не больше текущего времени + небольшой запас)
                val currentTimeMs = System.currentTimeMillis()
                val maxReasonableTimestamp = currentTimeMs + 86400000L // +1 день запас
                
                if (lastSyncTimestampMs > maxReasonableTimestamp) {
                    Log.w(TAG, "Обнаружен некорректный timestamp: ${lastSyncTimestampMs}ms (текущее время: ${currentTimeMs}ms). Сбрасываем.")
                    lastSyncTimestampMs = 0L
                    // Очищаем некорректный timestamp
                    saveLastSyncTimestamp(0L)
                }
                
                // Backend ожидает timestamp в секундах и требует since > 0
                val lastSyncTimestampSec = if (lastSyncTimestampMs == 0L) 1L else lastSyncTimestampMs / 1000
                Log.d(TAG, "Последняя синхронизация: ${lastSyncTimestampMs}ms = ${lastSyncTimestampSec}s")
                
                // Формируем URL для логирования
                val baseUrl = ApiConfig.getBaseUrl()
                val urlParams = buildString {
                    append("since=$lastSyncTimestampSec")
                    if (clientIdForRequest != null) {
                        append("&client_id=$clientIdForRequest")
                    }
                }
                val fullUrl = "$baseUrl/sync/pull?$urlParams"
                Log.d(TAG, "=== ВЫЗОВ SYNC API ===")
                Log.d(TAG, "Вызываю syncApi.syncPull(since=$lastSyncTimestampSec${if (clientIdForRequest != null) ", client_id=$clientIdForRequest" else ", client_id=null"})")
                Log.d(TAG, "Полный URL запроса: $fullUrl")
                Log.d(TAG, "Параметры запроса: since=$lastSyncTimestampSec, clientId=$clientIdForRequest")
                
                // Запрашиваем изменения с передачей client_id для роли CLIENT
                // ВАЖНО: Retrofit автоматически игнорирует null параметры, поэтому clientIdForRequest будет добавлен в URL только если не null
                Log.d(TAG, "Перед вызовом syncApi.syncPull: since=$lastSyncTimestampSec, clientId=$clientIdForRequest")
                val response = syncApi.syncPull(since = lastSyncTimestampSec, clientId = clientIdForRequest)
                
                Log.d(TAG, "=== ПОСЛЕ ВЫЗОВА SYNC API ===")
                Log.d(TAG, "Запрос выполнен, получен ответ")
                
                // Дополнительная проверка: логируем фактический URL запроса из ответа (если доступен)
                try {
                    val requestUrl = response.raw().request.url.toString()
                    Log.d(TAG, "Фактический URL запроса (из response.raw()): $requestUrl")
                    if (clientIdForRequest != null && !requestUrl.contains("client_id=$clientIdForRequest")) {
                        Log.e(TAG, "⚠️ ОШИБКА: client_id НЕ найден в фактическом URL запроса!")
                        Log.e(TAG, "Ожидалось: client_id=$clientIdForRequest")
                        Log.e(TAG, "Фактический URL: $requestUrl")
                    } else if (clientIdForRequest != null) {
                        Log.d(TAG, "✓ client_id найден в URL запроса")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Не удалось получить URL из response.raw(): ${e.message}")
                }
                
                Log.d(TAG, "Ответ от сервера: код=${response.code()}, успешно=${response.isSuccessful}")
                
                if (!response.isSuccessful) {
                    val errorCode = response.code()
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }
                    
                    val errorMsg = when (errorCode) {
                        401 -> "Токен авторизации недействителен или истек"
                        403 -> "Доступ запрещен"
                        else -> "Ошибка получения: код $errorCode"
                    }
                    Log.e(TAG, "$errorMsg. Тело ошибки: $errorBody")
                    return@withContext SyncResult(
                        success = false,
                        message = errorMsg
                    )
                }
                
                val pullResponse = response.body()
                if (pullResponse == null) {
                    Log.e(TAG, "Пустой ответ от сервера. Код ответа: ${response.code()}")
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }
                    Log.e(TAG, "Тело ошибки: $errorBody")
                    return@withContext SyncResult(
                        success = false,
                        message = "Пустой ответ от сервера (код: ${response.code()})"
                    )
                }
                
                Log.d(TAG, "Получен ответ: sites=${pullResponse.sites.size}, installations=${pullResponse.installations.size}, components=${pullResponse.components.size}")
                
                // Детальное логирование полученных данных
                if (pullResponse.sites.isNotEmpty()) {
                    Log.d(TAG, "=== ПОЛУЧЕННЫЕ ОБЪЕКТЫ (sites) ===")
                    pullResponse.sites.forEachIndexed { index, site ->
                        Log.d(TAG, "  [$index] id=${site.id}, name='${site.name}', clientId=${site.clientId}, origin=${site.origin}, createdAt=${site.createdAtEpoch}, updatedAt=${site.updatedAtEpoch}")
                    }
                } else {
                    Log.w(TAG, "⚠️ Получено 0 объектов (sites). Ожидались объекты для clientId=$userClientId")
                }
                
                if (pullResponse.installations.isNotEmpty()) {
                    Log.d(TAG, "=== ПОЛУЧЕННЫЕ УСТАНОВКИ (installations) ===")
                    pullResponse.installations.forEachIndexed { index, inst ->
                        Log.d(TAG, "  [$index] id=${inst.id}, name='${inst.name}', siteId=${inst.siteId}, origin=${inst.origin}")
                    }
                } else {
                    Log.w(TAG, "⚠️ Получено 0 установок (installations)")
                }
                
                if (pullResponse.components.isNotEmpty()) {
                    Log.d(TAG, "=== ПОЛУЧЕННЫЕ КОМПОНЕНТЫ (components) ===")
                    pullResponse.components.forEachIndexed { index, comp ->
                        Log.d(TAG, "  [$index] id=${comp.id}, name='${comp.name}', installationId=${comp.installationId}, origin=${comp.origin}")
                    }
                }
                
                // Применяем изменения к Room
                processPullResponse(pullResponse)
                
                // Обновляем timestamp последней синхронизации
                // Сервер возвращает timestamp в миллисекундах, сохраняем как есть
                val timestampMs = pullResponse.timestamp
                saveLastSyncTimestamp(timestampMs)
                Log.d(TAG, "Сохранён новый timestamp последней синхронизации: ${timestampMs}ms (${timestampMs / 1000}s)")
                
                val message = buildString {
                    append("Загружено: ")
                    append("объектов=${pullResponse.sites.size}, ")
                    append("установок=${pullResponse.installations.size}, ")
                    append("компонентов=${pullResponse.components.size}")
                }
                
                Log.d(TAG, message)
                
                SyncResult(
                    success = true,
                    message = message
                )
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP ошибка при получении", e)
                SyncResult(
                    success = false,
                    message = "Ошибка сети: ${e.message}"
                )
            } catch (e: NetworkException) {
                Log.e(TAG, "Сетевая ошибка при получении", e)
                SyncResult(
                    success = false,
                    message = "Ошибка сети: ${e.message}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении", e)
                SyncResult(
                    success = false,
                    message = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                )
            }
        }
    }
    
    private suspend fun processPullResponse(response: SyncPullResponse) {
        // Применяем изменения к Room по принципу last-write-wins
        
        Log.d(TAG, "=== ОБРАБОТКА ПОЛУЧЕННЫХ ДАННЫХ ===")
        
        // Получаем информацию о текущем пользователе для определения необходимости очистки
        val currentSession = ru.wassertech.core.auth.SessionManager.getInstance(context).getCurrentSession()
        val userRole = currentSession?.role
        val userClientId = currentSession?.clientId
        val isClientRole = userRole == ru.wassertech.core.auth.UserRole.CLIENT
        
        // Обрабатываем объекты (sites)
        Log.d(TAG, "Обработка объектов (sites): ${response.sites.size}")
        response.sites.forEachIndexed { index, dto ->
            Log.d(TAG, "  [$index] Применяю объект: id=${dto.id}, name='${dto.name}', clientId=${dto.clientId}")
            applySiteToRoom(dto)
        }
        
        // Обрабатываем установки (installations)
        Log.d(TAG, "Обработка установок (installations): ${response.installations.size}")
        response.installations.forEachIndexed { index, dto ->
            Log.d(TAG, "  [$index] Применяю установку: id=${dto.id}, name='${dto.name}', siteId=${dto.siteId}")
            applyInstallationToRoom(dto)
        }
        
        // Обрабатываем компоненты (components)
        Log.d(TAG, "Обработка компонентов (components): ${response.components.size}")
        response.components.forEachIndexed { index, dto ->
            Log.d(TAG, "  [$index] Применяю компонент: id=${dto.id}, name='${dto.name}', installationId=${dto.installationId}")
            applyComponentToRoom(dto)
        }
        
        // Обрабатываем паки иконок
        if (response.iconPacks.isNotEmpty()) {
            Log.d(TAG, "Получено паков иконок с сервера: ${response.iconPacks.size}")
            var appliedCount = 0
            response.iconPacks.forEach { dto ->
                val entity = dto.toEntity()
                database.iconPackDao().upsert(entity)
                appliedCount++
            }
            Log.d(TAG, "Обработано паков иконок: применено=$appliedCount")
        }
        
        // Обрабатываем иконки
        if (response.icons.isNotEmpty()) {
            Log.d(TAG, "Получено иконок с сервера: ${response.icons.size}")
            var appliedCount = 0
            var skippedCount = 0
            response.icons.forEach { dto ->
                val entity = dto.toEntity()
                if (entity != null) {
                    database.iconDao().upsert(entity)
                    appliedCount++
                } else {
                    skippedCount++
                    Log.w(TAG, "Пропуск иконки ${dto.id}: отсутствует packId или code")
                }
            }
            Log.d(TAG, "Обработано иконок: применено=$appliedCount, пропущено=$skippedCount")
        }
        
        // Обрабатываем удаления
        if (response.deleted.isNotEmpty()) {
            Log.d(TAG, "Получено удалений с сервера: ${response.deleted.size}")
            response.deleted.forEach { deleted ->
                val entityName = deleted.getEntityName()
                val recordId = deleted.recordId
                
                if (recordId.isNullOrBlank()) {
                    Log.w(TAG, "Пропускаем удаление $entityName: пустой id")
                    return@forEach
                }
                
                handleDeletedRecord(entityName, recordId)
            }
        }
        
        // Для роли CLIENT: очищаем чужие данные после применения ответа
        if (isClientRole && userClientId != null) {
            Log.d(TAG, "=== ОЧИСТКА ЧУЖИХ ДАННЫХ ДЛЯ РОЛИ CLIENT ===")
            Log.d(TAG, "Текущий clientId: $userClientId")
            
            try {
                // Удаляем клиентов, кроме текущего
                val deletedClients = database.clientDao().getAllClientsNow()
                    .filter { it.id != userClientId }
                    .map { it.id }
                if (deletedClients.isNotEmpty()) {
                    database.hierarchyDao().deleteClientsExcept(userClientId)
                    Log.d(TAG, "Удалено чужих клиентов: ${deletedClients.size}")
                }
                
                // Удаляем объекты, не принадлежащие текущему клиенту
                database.hierarchyDao().deleteSitesNotBelongingToClient(userClientId)
                Log.d(TAG, "Удалены чужие объекты (sites)")
                
                // Удаляем установки, не принадлежащие текущему клиенту
                database.hierarchyDao().deleteInstallationsNotBelongingToClient(userClientId)
                Log.d(TAG, "Удалены чужие установки (installations)")
                
                // Удаляем компоненты, не принадлежащие текущему клиенту
                database.hierarchyDao().deleteComponentsNotBelongingToClient(userClientId)
                Log.d(TAG, "Удалены чужие компоненты (components)")
                
                // Удаляем сессии ТО, не принадлежащие текущему клиенту
                database.sessionsDao().deleteSessionsNotBelongingToClient(userClientId)
                Log.d(TAG, "Удалены чужие сессии ТО (maintenance_sessions)")
                
                // Удаляем значения ТО, не принадлежащие текущему клиенту
                database.sessionsDao().deleteValuesNotBelongingToClient(userClientId)
                Log.d(TAG, "Удалены чужие значения ТО (maintenance_values)")
                
                // Очищаем иконки и паки: удаляем те, которых нет в ответе
                val receivedPackIds = response.iconPacks.map { it.id }
                if (receivedPackIds.isNotEmpty()) {
                    database.iconPackDao().deletePacksNotInList(receivedPackIds)
                    Log.d(TAG, "Удалены чужие паки иконок (icon_packs), оставлено: ${receivedPackIds.size}")
                } else {
                    // Если не пришло ни одного пака, удаляем все (не должно быть, но на всякий случай)
                    database.iconPackDao().deleteAll()
                    Log.d(TAG, "Удалены все паки иконок (ответ пустой)")
                }
                
                val receivedIconIds = response.icons.mapNotNull { dto ->
                    dto.toEntity()?.id
                }
                if (receivedIconIds.isNotEmpty()) {
                    database.iconDao().deleteIconsNotInList(receivedIconIds)
                    Log.d(TAG, "Удалены чужие иконки (icons), оставлено: ${receivedIconIds.size}")
                } else {
                    // Если не пришло ни одной иконки, удаляем все
                    database.iconDao().deleteAll()
                    Log.d(TAG, "Удалены все иконки (ответ пустой)")
                }
                
                Log.d(TAG, "✓ Очистка чужих данных завершена")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при очистке чужих данных", e)
                // Не прерываем синхронизацию из-за ошибки очистки
            }
        }
        
        // Проверяем, что данные действительно сохранились в Room
        val savedSites = database.hierarchyDao().getAllSitesNow()
        val savedInstallations = database.hierarchyDao().getAllInstallationsNow()
        val savedComponents = database.hierarchyDao().getAllComponentsNow()
        
        Log.d(TAG, "=== ПРОВЕРКА СОХРАНЁННЫХ ДАННЫХ В ROOM ===")
        Log.d(TAG, "Сохранено объектов (sites): ${savedSites.size}")
        savedSites.forEachIndexed { index, site ->
            Log.d(TAG, "  [$index] id=${site.id}, name='${site.name}', clientId=${site.clientId}, origin=${site.origin}")
        }
        Log.d(TAG, "Сохранено установок (installations): ${savedInstallations.size}")
        savedInstallations.forEachIndexed { index, inst ->
            Log.d(TAG, "  [$index] id=${inst.id}, name='${inst.name}', siteId=${inst.siteId}, origin=${inst.origin}")
        }
        Log.d(TAG, "Сохранено компонентов (components): ${savedComponents.size}")
    }
    
    private suspend fun applySiteToRoom(dto: SyncSiteDto) {
        val existing = database.hierarchyDao().getSiteNow(dto.id)
        val entity = SiteEntity(
            id = dto.id,
            clientId = dto.clientId,
            name = dto.name,
            address = dto.address,
            orderIndex = dto.orderIndex,
            iconId = dto.iconId,
            isArchived = dto.isArchived,
            archivedAtEpoch = dto.archivedAtEpoch,
            createdAtEpoch = dto.createdAtEpoch,
            updatedAtEpoch = dto.updatedAtEpoch,
            deletedAtEpoch = null,
            dirtyFlag = false, // Данные с сервера уже синхронизированы
            syncStatus = 0, // SYNCED
            ownerClientId = dto.clientId, // Используем clientId как ownerClientId
            origin = dto.origin,
            createdByUserId = dto.created_by_user_id
        )
        
        Log.d(TAG, "  → Объект: id=${dto.id}, name='${dto.name}', clientId=${dto.clientId}, origin=${dto.origin}, existing=${existing != null}")
        
        if (existing == null) {
            database.hierarchyDao().insertSite(entity)
            Log.d(TAG, "  ✓ Создан объект в Room: ${dto.name} (id: ${dto.id}, clientId: ${dto.clientId})")
        } else {
            // Обновляем только если данные с сервера новее
            if (dto.updatedAtEpoch >= existing.updatedAtEpoch) {
                database.hierarchyDao().upsertSite(entity)
                Log.d(TAG, "  ✓ Обновлён объект в Room: ${dto.name} (id: ${dto.id}, clientId: ${dto.clientId})")
            } else {
                Log.d(TAG, "  ⊘ Пропущен объект: ${dto.name} (id: ${dto.id}) - локальная версия новее (локальная: ${existing.updatedAtEpoch}, сервер: ${dto.updatedAtEpoch})")
            }
        }
    }
    
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
            orderIndex = dto.orderIndex,
            iconId = dto.iconId,
            ownerClientId = null, // Установки не имеют прямого ownerClientId, он определяется через site
            origin = dto.origin,
            createdByUserId = dto.created_by_user_id
        )
        
        Log.d(TAG, "  → Установка: id=${dto.id}, name='${dto.name}', siteId=${dto.siteId}, origin=${dto.origin}, existing=${existing != null}")
        
        if (existing == null) {
            database.hierarchyDao().insertInstallation(entity)
            Log.d(TAG, "  ✓ Создана установка в Room: ${dto.name} (id: ${dto.id}, siteId: ${dto.siteId})")
        } else if (dto.updatedAtEpoch > existing.updatedAtEpoch) {
            database.hierarchyDao().upsertInstallation(entity)
            Log.d(TAG, "  ✓ Обновлена установка в Room: ${dto.name} (id: ${dto.id}, siteId: ${dto.siteId})")
        } else {
            Log.d(TAG, "  ⊘ Пропущена установка: ${dto.name} (id: ${dto.id}) - локальная версия новее или равна")
        }
    }
    
    private suspend fun applyComponentToRoom(dto: SyncComponentDto) {
        val existing = database.hierarchyDao().getComponent(dto.id)
        val componentType = try {
            ComponentType.valueOf(dto.type)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Неизвестный тип компонента: ${dto.type}, используем COMMON")
            ComponentType.COMMON
        }
        
        val entity = ComponentEntity(
            id = dto.id,
            installationId = dto.installationId,
            name = dto.name,
            type = componentType,
            orderIndex = dto.orderIndex,
            templateId = dto.templateId,
            iconId = dto.iconId,
            createdAtEpoch = dto.createdAtEpoch,
            updatedAtEpoch = dto.updatedAtEpoch,
            isArchived = dto.isArchived,
            archivedAtEpoch = dto.archivedAtEpoch,
            deletedAtEpoch = null,
            dirtyFlag = false, // Данные с сервера уже синхронизированы
            syncStatus = 0, // SYNCED
            ownerClientId = null, // Компоненты не имеют прямого ownerClientId, он определяется через installation -> site
            origin = dto.origin,
            createdByUserId = dto.created_by_user_id
        )
        
        if (existing == null) {
            database.hierarchyDao().insertComponent(entity)
            Log.d(TAG, "Создан компонент: ${dto.name} (id: ${dto.id})")
        } else {
            // Обновляем только если данные с сервера новее
            if (dto.updatedAtEpoch >= existing.updatedAtEpoch) {
                database.hierarchyDao().upsertComponent(entity)
                Log.d(TAG, "Обновлён компонент: ${dto.name} (id: ${dto.id})")
            } else {
                Log.d(TAG, "Пропущен компонент: ${dto.name} (id: ${dto.id}) - локальная версия новее")
            }
        }
    }
    
    private suspend fun handleDeletedRecord(entityName: String?, recordId: String) {
        when (entityName) {
            "sites" -> {
                database.hierarchyDao().deleteSite(recordId)
                Log.d(TAG, "Удалён объект: $recordId")
            }
            "installations" -> {
                database.hierarchyDao().deleteInstallation(recordId)
                Log.d(TAG, "Удалена установка: $recordId")
            }
            "components" -> {
                database.hierarchyDao().deleteComponent(recordId)
                Log.d(TAG, "Удалён компонент: $recordId")
            }
            else -> {
                Log.w(TAG, "Неизвестный тип сущности для удаления: $entityName")
            }
        }
    }
    
    private suspend fun getLastSyncTimestamp(): Long? {
        val value = settingsDao.getValueSync(SETTINGS_KEY_LAST_SYNC_TIMESTAMP)
        return value?.toLongOrNull()
    }
    
    private suspend fun saveLastSyncTimestamp(timestamp: Long) {
        settingsDao.setValue(
            SettingsEntity(
                key = SETTINGS_KEY_LAST_SYNC_TIMESTAMP,
                value = timestamp.toString()
            )
        )
    }
    
    /**
     * Отправка локальных изменений на сервер
     */
    suspend fun syncPush(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало отправки локальных изменений на сервер")
                
                // Проверяем наличие токена
                val token = tokenStorage.getAccessToken()
                if (token == null) {
                    val errorMsg = "Токен авторизации отсутствует. Необходимо войти в систему."
                    Log.e(TAG, errorMsg)
                    return@withContext SyncResult(
                        success = false,
                        message = errorMsg
                    )
                }
                
                // Собираем все "грязные" записи
                val request = buildSyncPushRequest()
                
                if (request.sites.isEmpty() && request.installations.isEmpty() && request.components.isEmpty()) {
                    Log.d(TAG, "Нет локальных изменений для отправки")
                    return@withContext SyncResult(
                        success = true,
                        message = "Нет изменений для отправки"
                    )
                }
                
                Log.d(TAG, "Отправляю на сервер: sites=${request.sites.size}, installations=${request.installations.size}, components=${request.components.size}")
                
                // Отправляем на сервер
                val response = syncApi.syncPush(request)
                
                if (!response.isSuccessful) {
                    val errorCode = response.code()
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }
                    
                    val errorMsg = when (errorCode) {
                        401 -> "Токен авторизации недействителен или истек"
                        403 -> "Доступ запрещен"
                        else -> "Ошибка отправки: код $errorCode"
                    }
                    Log.e(TAG, "$errorMsg. Тело ошибки: $errorBody")
                    return@withContext SyncResult(
                        success = false,
                        message = errorMsg
                    )
                }
                
                val pushResponse = response.body()
                if (pushResponse == null) {
                    Log.e(TAG, "Пустой ответ от сервера при отправке")
                    return@withContext SyncResult(
                        success = false,
                        message = "Пустой ответ от сервера"
                    )
                }
                
                // Обрабатываем ответ
                processPushResponse(pushResponse, request)
                
                val message = buildString {
                    append("Отправлено: ")
                    append("объектов=${request.sites.size}, ")
                    append("установок=${request.installations.size}, ")
                    append("компонентов=${request.components.size}")
                }
                
                Log.d(TAG, message)
                
                SyncResult(
                    success = true,
                    message = message
                )
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP ошибка при отправке", e)
                SyncResult(
                    success = false,
                    message = "Ошибка сети: ${e.message}"
                )
            } catch (e: NetworkException) {
                Log.e(TAG, "Сетевая ошибка при отправке", e)
                SyncResult(
                    success = false,
                    message = "Ошибка сети: ${e.message}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при отправке", e)
                SyncResult(
                    success = false,
                    message = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                )
            }
        }
    }
    
    /**
     * Полная синхронизация: сначала push, затем pull
     */
    suspend fun syncFull(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало полной синхронизации")
                
                // 1. Push локальных изменений
                val pushResult = syncPush()
                if (!pushResult.success) {
                    return@withContext pushResult
                }
                
                // 2. Pull изменений с сервера
                val pullResult = syncPull()
                
                SyncResult(
                    success = pushResult.success && pullResult.success,
                    message = "Push: ${pushResult.message}; Pull: ${pullResult.message}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при полной синхронизации", e)
                SyncResult(
                    success = false,
                    message = "Ошибка синхронизации: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun buildSyncPushRequest(): SyncPushRequest {
        // Собираем все "грязные" записи из Room
        val sites = database.hierarchyDao().getDirtySitesNow().map { it.toSyncDto() }
        val installations = database.hierarchyDao().getDirtyInstallationsNow().map { it.toSyncDto() }
        val components = database.hierarchyDao().getDirtyComponentsNow().map { it.toSyncDto() }
        
        Log.d(TAG, "Найдено dirty записей: sites=${sites.size}, installations=${installations.size}, components=${components.size}")
        
        return SyncPushRequest(
            clients = emptyList(), // Клиенты не создаются в app-client
            sites = sites,
            installations = installations,
            components = components,
            maintenance_sessions = emptyList(), // Пока не реализовано
            maintenance_values = emptyList(), // Пока не реализовано
            component_templates = emptyList(), // Пока не реализовано
            component_template_fields = emptyList(), // Пока не реализовано
            deleted = emptyList() // Пока не реализовано
        )
    }
    
    private suspend fun processPushResponse(response: SyncPushResponse, request: SyncPushRequest) {
        if (!response.success) {
            Log.e(TAG, "Push failed: success=false")
            return
        }
        
        val errors = response.errors.orEmpty()
        
        // Собираем ID записей с ошибками по типам сущностей
        val errorIdsByType = errors.groupBy { it.entityType }
            .mapValues { (_, errorList) -> errorList.map { it.entityId }.toSet() }
        
        // Обрабатываем sites
        val siteIds = request.sites.map { it.id }
        val siteErrorIds = errorIdsByType["sites"] ?: emptySet()
        val siteSuccessIds = siteIds.filter { it !in siteErrorIds }
        if (siteSuccessIds.isNotEmpty()) {
            database.hierarchyDao().markSitesAsSynced(siteSuccessIds)
            Log.d(TAG, "Помечено как синхронизировано объектов: ${siteSuccessIds.size}")
        }
        if (siteErrorIds.isNotEmpty()) {
            Log.e(TAG, "Ошибки при синхронизации объектов: ${siteErrorIds.size}")
        }
        
        // Обрабатываем installations
        val installationIds = request.installations.map { it.id }
        val installationErrorIds = errorIdsByType["installations"] ?: emptySet()
        val installationSuccessIds = installationIds.filter { it !in installationErrorIds }
        if (installationSuccessIds.isNotEmpty()) {
            database.hierarchyDao().markInstallationsAsSynced(installationSuccessIds)
            Log.d(TAG, "Помечено как синхронизировано установок: ${installationSuccessIds.size}")
        }
        
        // Обрабатываем components
        val componentIds = request.components.map { it.id }
        val componentErrorIds = errorIdsByType["components"] ?: emptySet()
        val componentSuccessIds = componentIds.filter { it !in componentErrorIds }
        if (componentSuccessIds.isNotEmpty()) {
            database.hierarchyDao().markComponentsAsSynced(componentSuccessIds)
            Log.d(TAG, "Помечено как синхронизировано компонентов: ${componentSuccessIds.size}")
        }
    }
    
    // Вспомогательные методы для преобразования Entity -> DTO
    private fun SiteEntity.toSyncDto() = SyncSiteDto(
        id = id,
        clientId = clientId,
        name = name,
        address = address,
        orderIndex = orderIndex,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        origin = origin,
        created_by_user_id = createdByUserId,
        iconId = iconId
    )
    
    private fun InstallationEntity.toSyncDto() = SyncInstallationDto(
        id = id,
        siteId = siteId,
        name = name,
        orderIndex = orderIndex,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        origin = origin,
        created_by_user_id = createdByUserId,
        iconId = iconId
    )
    
    private fun ComponentEntity.toSyncDto() = SyncComponentDto(
        id = id,
        installationId = installationId,
        name = name,
        type = type.name,
        orderIndex = orderIndex,
        templateId = templateId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        origin = origin,
        created_by_user_id = createdByUserId,
        iconId = iconId
    )
    
    private fun SyncIconPackDto.toEntity() = ru.wassertech.client.data.entities.IconPackEntity(
        id = id,
        code = code,
        name = name,
        description = description,
        isBuiltin = isBuiltin,
        isPremium = isPremium,
        origin = origin ?: "CRM", // По умолчанию CRM для старых данных
        createdByUserId = createdByUserId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch
    )
    
    private fun SyncIconDto.toEntity(): ru.wassertech.client.data.entities.IconEntity? {
        // Пропускаем иконки без packId или code, так как они обязательны
        val validPackId = packId ?: return null
        val validCode = code.ifBlank { return null }
        val validEntityType = entityType ?: "ANY"
        
        return ru.wassertech.client.data.entities.IconEntity(
            id = id,
            packId = validPackId,
            code = validCode,
            label = label,
            entityType = validEntityType,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            androidResName = androidResName,
            isActive = isActive,
            origin = origin ?: "CRM", // По умолчанию CRM для старых данных
            createdByUserId = createdByUserId,
            createdAtEpoch = createdAtEpoch,
            updatedAtEpoch = updatedAtEpoch
        )
    }
    
    /**
     * Результат синхронизации
     */
    data class SyncResult(
        val success: Boolean,
        val message: String
    )
}

