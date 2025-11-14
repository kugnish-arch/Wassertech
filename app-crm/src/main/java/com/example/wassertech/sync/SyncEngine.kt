package ru.wassertech.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.wassertech.core.auth.DataStoreTokenStorage
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.ApiConfig
import ru.wassertech.core.network.api.SyncApi
import ru.wassertech.core.network.dto.*
import ru.wassertech.core.network.interceptor.NetworkException
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.dao.SettingsDao
import ru.wassertech.data.entities.*
import ru.wassertech.data.types.ComponentType
import ru.wassertech.data.types.FieldType
import ru.wassertech.data.types.SyncStatus

/**
 * Движок синхронизации данных с сервером через REST API
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
                    message = "Push: ${pushResult.message}; Pull: ${pullResult.message}",
                    pushStats = pushResult.pushStats,
                    pullStats = pullResult.pullStats
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
    
    /**
     * Отправка локальных изменений на сервер
     */
    suspend fun syncPush(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало отправки локальных изменений")
                
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
                Log.d(TAG, "Токен найден, длина: ${token.length}")
                
                // Собираем все "грязные" записи
                val request = buildSyncPushRequest()
                
                // Проверяем, есть ли что отправлять
                val dirtyClients = request.clients.size
                val dirtySites = request.sites.size
                val dirtyInstallations = request.installations.size
                val dirtyComponents = request.components.size
                val dirtySessions = request.maintenance_sessions.size
                val dirtyValues = request.maintenance_values.size
                val dirtyComponentTemplates = request.component_templates.size
                val dirtyComponentTemplateFields = request.component_template_fields.size
                val dirtyDeleted = request.deleted.size
                val totalCount = dirtyClients + dirtySites + dirtyInstallations + dirtyComponents +
                    dirtySessions + dirtyValues + dirtyComponentTemplates + dirtyComponentTemplateFields + dirtyDeleted
                
                Log.d(TAG, "Dirty counts: clients=$dirtyClients, sites=$dirtySites, " +
                    "installations=$dirtyInstallations, components=$dirtyComponents, " +
                    "sessions=$dirtySessions, values=$dirtyValues, " +
                    "componentTemplates=$dirtyComponentTemplates, componentTemplateFields=$dirtyComponentTemplateFields, deleted=$dirtyDeleted")
                
                if (dirtyComponentTemplates > 0) {
                    Log.d(TAG, "Отправка шаблонов компонентов: количество=$dirtyComponentTemplates")
                }
                if (dirtyComponentTemplateFields > 0) {
                    Log.d(TAG, "Отправка полей шаблонов компонентов: количество=$dirtyComponentTemplateFields")
                }
                
                // Логируем содержимое запроса
                Log.d(TAG, "Содержимое запроса sync/push: " +
                        "component_templates=${request.component_templates.size}, " +
                        "component_template_fields=${request.component_template_fields.size}")
                if (request.component_templates.isNotEmpty()) {
                    request.component_templates.forEach { t ->
                        Log.d(TAG, "  → Шаблон: id=${t.id}, name=${t.name}, createdAt=${t.createdAtEpoch}, updatedAt=${t.updatedAtEpoch}")
                    }
                }
                if (request.component_template_fields.isNotEmpty()) {
                    request.component_template_fields.forEach { f ->
                        Log.d(TAG, "  → Поле: id=${f.id}, templateId=${f.templateId}, label=${f.label}, isForMaintenance=${f.isForMaintenance}")
                    }
                }
                
                if (dirtyDeleted > 0) {
                    val deletedByEntity = request.deleted.groupBy { it.entity }
                    Log.d(TAG, "Deleted records by entity: ${deletedByEntity.mapValues { it.value.size }}")
                }
                
                if (totalCount == 0) {
                    Log.d(TAG, "Нет локальных изменений для отправки")
                    return@withContext SyncResult(
                        success = true,
                        message = "Нет локальных изменений",
                        pushStats = SyncPushStats(0, 0, 0)
                    )
                }
                
                Log.d(TAG, "Отправка $totalCount записей на сервер через sync/push")
                
                // Отправляем запрос
                val response = syncApi.syncPush(request)
                Log.d(TAG, "Получен ответ от sync/push: код=${response.code()}, успешно=${response.isSuccessful}")
                
                if (!response.isSuccessful) {
                    val errorCode = response.code()
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }
                    
                    val errorMsg = when (errorCode) {
                        401 -> {
                            Log.e(TAG, "Ошибка 401: Токен недействителен или истек. Тело ошибки: $errorBody")
                            Log.e(TAG, "Текущий токен: ${token.take(50)}...")
                            "Токен авторизации недействителен или истек. Необходимо войти в систему заново."
                        }
                        403 -> {
                            Log.e(TAG, "Ошибка 403: Доступ запрещен. Тело ошибки: $errorBody")
                            "Доступ запрещен. Проверьте права доступа."
                        }
                        else -> {
                            Log.e(TAG, "Ошибка отправки: код=$errorCode, тело=$errorBody")
                            "Ошибка отправки: код $errorCode${if (errorBody != null) " ($errorBody)" else ""}"
                        }
                    }
                    
                    return@withContext SyncResult(
                        success = false,
                        message = errorMsg
                    )
                }
                
                val pushResponse = response.body()
                if (pushResponse == null) {
                    Log.e(TAG, "Пустой ответ от сервера sync/push")
                    return@withContext SyncResult(
                        success = false,
                        message = "Пустой ответ от сервера"
                    )
                }
                
                // Логируем ответ от сервера
                Log.d(TAG, "Ответ sync/push: success=${pushResponse.success}, " +
                        "processed=${pushResponse.processed}, errors=${pushResponse.errors.size}")
                if (pushResponse.processed != null) {
                    Log.d(TAG, "Обработано сервером: component_templates=${pushResponse.processed.component_templates}, " +
                            "component_template_fields=${pushResponse.processed.component_template_fields}")
                }
                if (pushResponse.errors.isNotEmpty()) {
                    pushResponse.errors.forEach { error ->
                        Log.e(TAG, "Ошибка синхронизации: entityType=${error.entityType}, entityId=${error.entityId}, message=${error.message}")
                    }
                }
                
                // Обрабатываем ответ и обновляем статусы в Room
                processPushResponse(pushResponse, request)
                
                val stats = calculatePushStats(pushResponse)
                val message = buildString {
                    append("Отправлено: ")
                    append("вставлено=${stats.inserted}, ")
                    append("обновлено=${stats.updated}, ")
                    append("пропущено=${stats.skipped}")
                    if (pushResponse.errors.isNotEmpty()) {
                        append(", ошибок=${pushResponse.errors.size}")
                    }
                }
                
                Log.d(TAG, message)
                
                SyncResult(
                    success = pushResponse.success,
                    message = message,
                    pushStats = stats
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
                
                // Получаем timestamp последней синхронизации (в миллисекундах)
                val lastSyncTimestampMs = getLastSyncTimestamp() ?: 0L
                // Backend ожидает timestamp в секундах и требует since > 0
                // При первом запуске используем 1 вместо 0
                val lastSyncTimestampSec = if (lastSyncTimestampMs == 0L) 1L else lastSyncTimestampMs / 1000
                Log.d(TAG, "Последняя синхронизация: ${lastSyncTimestampMs}ms = ${lastSyncTimestampSec}s (${if (lastSyncTimestampMs == 0L) "первый запуск, используем since=1" else "timestamp=$lastSyncTimestampSec"})")
                Log.d(TAG, "Вызываю syncPull(since=$lastSyncTimestampSec)")
                
                // Запрашиваем изменения (since в секундах, всегда > 0)
                val response = syncApi.syncPull(since = lastSyncTimestampSec)
                
                if (!response.isSuccessful) {
                    val errorCode = response.code()
                    val errorBody = try {
                        response.errorBody()?.string()
                    } catch (e: Exception) {
                        null
                    }
                    
                    val errorMsg = when (errorCode) {
                        401 -> {
                            Log.e(TAG, "Ошибка 401: Токен недействителен или истек. Тело ошибки: $errorBody")
                            "Токен авторизации недействителен или истек. Необходимо войти в систему заново."
                        }
                        403 -> {
                            Log.e(TAG, "Ошибка 403: Доступ запрещен. Тело ошибки: $errorBody")
                            "Доступ запрещен. Проверьте права доступа."
                        }
                        else -> {
                            Log.e(TAG, "Ошибка получения: код=$errorCode, тело=$errorBody")
                            "Ошибка получения: код $errorCode${if (errorBody != null) " ($errorBody)" else ""}"
                        }
                    }
                    
                    return@withContext SyncResult(
                        success = false,
                        message = errorMsg
                    )
                }
                
                val pullResponse = response.body()
                if (pullResponse == null) {
                    return@withContext SyncResult(
                        success = false,
                        message = "Пустой ответ от сервера"
                    )
                }
                
                // Применяем изменения к Room
                processPullResponse(pullResponse)
                
                // Обновляем timestamp последней синхронизации
                // Backend возвращает timestamp в секундах, конвертируем в миллисекунды для хранения
                val timestampMs = pullResponse.timestamp * 1000
                saveLastSyncTimestamp(timestampMs)
                Log.d(TAG, "Сохранён новый timestamp последней синхронизации: ${pullResponse.timestamp}s = ${timestampMs}ms")
                
                val stats = calculatePullStats(pullResponse)
                val message = buildString {
                    append("Загружено: ")
                    append("клиентов=${stats.clients}, ")
                    append("объектов=${stats.sites}, ")
                    append("установок=${stats.installations}, ")
                    append("компонентов=${stats.components}, ")
                    append("сессий ТО=${stats.sessions}, ")
                    append("значений ТО=${stats.values}, ")
                    append("шаблонов=${stats.templates}, ")
                    append("удалено=${stats.deleted}")
                }
                
                Log.d(TAG, message)
                
                SyncResult(
                    success = true,
                    message = message,
                    pullStats = stats
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
    
    // ... продолжение следует
    
    private suspend fun buildSyncPushRequest(): SyncPushRequest {
        // Собираем все "грязные" записи из Room
        val clients = database.clientDao().getDirtyClientsNow().map { it.toSyncDto() }
        val sites = database.hierarchyDao().getDirtySitesNow().map { it.toSyncDto() }
        val installations = database.hierarchyDao().getDirtyInstallationsNow().map { it.toSyncDto() }
        val components = database.hierarchyDao().getDirtyComponentsNow().map { it.toSyncDto() }
        
        // Для сессий нужно получать связанные values, поэтому используем suspend функцию
        val dirtySessions = database.sessionsDao().getDirtySessionsNow()
        val sessions = mutableListOf<SyncMaintenanceSessionDto>()
        for (session in dirtySessions) {
            sessions.add(session.toSyncDto())
        }
        
        val values = database.sessionsDao().getDirtyValuesNow().map { it.toSyncDto() }
        
        // Новая модель: собираем componentTemplates и componentTemplateFields
        val componentTemplates = database.componentTemplatesDao().getDirtyComponentTemplatesNow().map { it.toSyncDto() }
        val componentTemplateFields = database.componentTemplateFieldsDao().getDirtyComponentTemplateFieldsNow()
        
        if (componentTemplates.isNotEmpty()) {
            Log.d(TAG, "Найдено dirty шаблонов компонентов для отправки: ${componentTemplates.size}")
            componentTemplates.forEach { template ->
                Log.d(TAG, "  - Шаблон компонента: id=${template.id}, name=${template.name}")
            }
        }
        
        if (componentTemplateFields.isNotEmpty()) {
            Log.d(TAG, "Найдено dirty полей шаблонов компонентов для отправки: ${componentTemplateFields.size}")
            componentTemplateFields.forEach { field ->
                Log.d(TAG, "  - Поле шаблона: id=${field.id}, templateId=${field.templateId}, " +
                        "label=${field.label}, isCharacteristic=${field.isCharacteristic}, " +
                        "dirtyFlag=${field.dirtyFlag}, syncStatus=${field.syncStatus}")
            }
        }
        
        // Маппим component_template_fields в DTO для отправки
        val componentTemplateFieldsDto = componentTemplateFields.map { it.toChecklistFieldDto() }
        
        Log.d(TAG, "Итоговые данные для отправки: component_templates=${componentTemplates.size}, " +
                "component_template_fields=${componentTemplateFieldsDto.size}")
        
        // Собираем dirty-записи об удалениях
        val dirtyDeletedRecords = database.deletedRecordsDao().getDirtyDeletedRecordsNow()
        val deleted = dirtyDeletedRecords.map {
            DeletedRecordDto(
                entity = it.entity,
                recordId = it.recordId,
                deletedAtEpoch = it.deletedAtEpoch
            )
        }
        
        return SyncPushRequest(
            clients = clients,
            sites = sites,
            installations = installations,
            components = components,
            maintenance_sessions = sessions,
            maintenance_values = values,
            component_templates = componentTemplates,
            component_template_fields = componentTemplateFieldsDto,
            deleted = deleted
        )
    }
    
    private suspend fun processPushResponse(response: SyncPushResponse, request: SyncPushRequest) {
        // Обрабатываем ответ и обновляем статусы в Room
        // Для успешно обработанных записей - помечаем как SYNCED
        // Для записей с ошибками - помечаем как CONFLICT
        
        if (!response.success) {
            Log.w(TAG, "Push не успешен (success=false), не обновляем статусы")
            return
        }
        
        // Проверяем, что сервер действительно обработал записи
        val processed = response.processed
        if (processed == null) {
            Log.w(TAG, "Push успешен, но processed=null, не обновляем статусы")
            return
        }
        
        Log.d(TAG, "Обработка ответа push: processed.component_templates=${processed.component_templates}, " +
                "processed.component_template_fields=${processed.component_template_fields}")
        
        // Собираем ID всех отправленных записей из переданного request
        val allSentIds = mutableMapOf<String, List<String>>()
        allSentIds["clients"] = request.clients.map { it.id }
        allSentIds["sites"] = request.sites.map { it.id }
        allSentIds["installations"] = request.installations.map { it.id }
        allSentIds["components"] = request.components.map { it.id }
        allSentIds["maintenance_sessions"] = request.maintenance_sessions.map { it.id }
        allSentIds["maintenance_values"] = request.maintenance_values.mapNotNull { it.id }
        allSentIds["component_templates"] = request.component_templates.map { it.id }
        allSentIds["component_template_fields"] = request.component_template_fields.map { it.id }
        
        // Обрабатываем удаления отдельно
        val deletedRecords = database.deletedRecordsDao().getDirtyDeletedRecordsNow()
        val deletedRecordIds = deletedRecords.map { it.id }
        
        // Собираем ID записей с ошибками
        val errorIdsByType = response.errors.groupBy { it.entityType }
            .mapValues { (_, errors) -> errors.map { it.entityId } }
        
        // Помечаем успешно обработанные записи как синхронизированные
        // Используем processed для проверки, что сервер действительно обработал записи
        allSentIds.forEach { (entityType, ids) ->
            val errorIds = errorIdsByType[entityType] ?: emptyList()
            
            // Проверяем, сколько записей сервер обработал для этого типа сущности
            val processedCount = when (entityType) {
                "clients" -> processed.clients
                "sites" -> processed.sites
                "installations" -> processed.installations
                "components" -> processed.components
                "maintenance_sessions" -> processed.maintenance_sessions
                "maintenance_values" -> processed.maintenance_values
                "component_templates" -> processed.component_templates
                "component_template_fields" -> processed.component_template_fields
                else -> 0
            }
            
            Log.d(TAG, "Обработка $entityType: отправлено=${ids.size}, обработано сервером=$processedCount, ошибок=${errorIds.size}")
            
            // Помечаем как синхронизированные только те записи, которые сервер обработал и которые не имеют ошибок
            // Берем первые processedCount записей без ошибок
            val successIds = (ids - errorIds.toSet()).take(processedCount)
            
            if (successIds.isNotEmpty()) {
                when (entityType) {
                    "clients" -> database.clientDao().markClientsAsSynced(successIds)
                    "sites" -> database.hierarchyDao().markSitesAsSynced(successIds)
                    "installations" -> database.hierarchyDao().markInstallationsAsSynced(successIds)
                    "components" -> database.hierarchyDao().markComponentsAsSynced(successIds)
                    "maintenance_sessions" -> database.sessionsDao().markSessionsAsSynced(successIds)
                    "maintenance_values" -> database.sessionsDao().markValuesAsSynced(successIds)
                    "component_templates" -> {
                        database.componentTemplatesDao().markComponentTemplatesAsSynced(successIds)
                        Log.d(TAG, "Помечено как синхронизировано шаблонов компонентов: ${successIds.size}, IDs=${successIds.take(5)}")
                    }
                    "component_template_fields" -> {
                        database.componentTemplateFieldsDao().markComponentTemplateFieldsAsSynced(successIds)
                        Log.d(TAG, "Помечено как синхронизировано полей шаблонов компонентов: ${successIds.size}, IDs=${successIds.take(5)}")
                    }
                }
                if (entityType != "component_templates" && entityType != "component_template_fields") {
                    Log.d(TAG, "Помечено как синхронизировано: $entityType = ${successIds.size}")
                }
            } else if (ids.isNotEmpty() && processedCount == 0 && errorIds.isEmpty()) {
                Log.w(TAG, "ВНИМАНИЕ: $entityType отправлено ${ids.size} записей, но сервер обработал 0 (processed.$entityType=0). " +
                        "Записи НЕ помечены как синхронизированные.")
            }
            
            // Помечаем записи с ошибками как конфликтные
            if (errorIds.isNotEmpty()) {
                when (entityType) {
                    "clients" -> database.clientDao().markClientsAsConflict(errorIds)
                    "sites" -> database.hierarchyDao().markSitesAsConflict(errorIds)
                    "installations" -> database.hierarchyDao().markInstallationsAsConflict(errorIds)
                    "components" -> database.hierarchyDao().markComponentsAsConflict(errorIds)
                    "maintenance_sessions" -> database.sessionsDao().markSessionsAsConflict(errorIds)
                    "maintenance_values" -> database.sessionsDao().markValuesAsConflict(errorIds)
                    "component_templates" -> {
                        database.componentTemplatesDao().markComponentTemplatesAsConflict(errorIds)
                        Log.w(TAG, "Помечено как конфликт шаблонов компонентов: ${errorIds.size}, IDs=${errorIds.take(5)}")
                    }
                    "component_template_fields" -> {
                        database.componentTemplateFieldsDao().markComponentTemplateFieldsAsConflict(errorIds)
                        Log.w(TAG, "Помечено как конфликт полей шаблонов компонентов: ${errorIds.size}, IDs=${errorIds.take(5)}")
                    }
                }
                if (entityType != "component_templates" && entityType != "component_template_fields") {
                    Log.w(TAG, "Помечено как конфликт: $entityType = ${errorIds.size}")
                }
            }
        }
        
        // Обрабатываем удаления: помечаем как синхронизированные после успешного push
        if (deletedRecordIds.isNotEmpty()) {
            database.deletedRecordsDao().markAsSynced(deletedRecordIds)
            Log.d(TAG, "Помечено как синхронизировано удалений: ${deletedRecordIds.size}")
            
            // Удаляем синхронизированные записи об удалениях
            database.deletedRecordsDao().deleteAllSynced()
            Log.d(TAG, "Удалены синхронизированные записи об удалениях")
        }
        
        if (response.errors.isNotEmpty()) {
            Log.w(TAG, "Ошибки при синхронизации: ${response.errors.size}")
            response.errors.forEach { error ->
                Log.w(TAG, "  ${error.entityType}/${error.entityId}: ${error.message}")
            }
        }
    }
    
    private suspend fun processPullResponse(response: SyncPullResponse) {
        // Применяем изменения к Room по принципу last-write-wins
        
        // Обрабатываем каждую сущность
        response.clients.forEach { dto ->
            applyClientToRoom(dto)
        }
        
        response.sites.forEach { dto ->
            applySiteToRoom(dto)
        }
        
        response.installations.forEach { dto ->
            applyInstallationToRoom(dto)
        }
        
        response.components.forEach { dto ->
            applyComponentToRoom(dto)
        }
        
        response.maintenance_sessions.forEach { dto ->
            applyMaintenanceSessionToRoom(dto)
        }
        
        response.maintenance_values.forEach { dto ->
            applyMaintenanceValueToRoom(dto)
        }
        
        // Обрабатываем поля шаблонов компонентов
        if (response.component_template_fields.isNotEmpty()) {
            Log.d(TAG, "Получено полей шаблонов компонентов с сервера: ${response.component_template_fields.size}")
            var appliedCount = 0
            var skippedCount = 0
            response.component_template_fields.forEach { dto ->
                val before = database.componentTemplateFieldsDao().getFieldById(dto.id)
                applyChecklistFieldToComponentTemplateField(dto)
                val after = database.componentTemplateFieldsDao().getFieldById(dto.id)
                if (after != null) {
                    appliedCount++
                    if (before == null) {
                        Log.d(TAG, "  - Создано поле: id=${dto.id}, templateId=${dto.templateId}, label=${dto.label}")
                    } else {
                        Log.d(TAG, "  - Обновлено поле: id=${dto.id}, templateId=${dto.templateId}, label=${dto.label}")
                    }
                } else {
                    skippedCount++
                }
            }
            Log.d(TAG, "Обработано полей шаблонов компонентов: применено=$appliedCount, пропущено=$skippedCount")
        }
        
        // Обрабатываем шаблоны компонентов
        if (response.component_templates.isNotEmpty()) {
            Log.d(TAG, "Получено шаблонов компонентов с сервера: ${response.component_templates.size}")
            var appliedCount = 0
            var skippedCount = 0
            response.component_templates.forEach { dto ->
                val before = database.componentTemplatesDao().getById(dto.id)
                applyComponentTemplateToRoom(dto)
                val after = database.componentTemplatesDao().getById(dto.id)
                if (after != null) {
                    appliedCount++
                    if (before == null) {
                        Log.d(TAG, "  - Создан шаблон: id=${dto.id}, name=${dto.name}")
                    } else {
                        Log.d(TAG, "  - Обновлён шаблон: id=${dto.id}, name=${dto.name}")
                    }
                } else {
                    skippedCount++
                }
            }
            Log.d(TAG, "Обработано шаблонов компонентов: применено=$appliedCount, пропущено=$skippedCount")
        }
        
        // Обрабатываем удаления
        // ВАЖНО: обрабатываем удаления ПОСЛЕ обработки основных списков,
        // чтобы не удалить записи, которые просто архивированы (сервер может отправлять их в deleted)
        if (response.deleted.isNotEmpty()) {
            val deletedByEntity = response.deleted.groupBy { it.getEntityName() ?: "unknown" }
            Log.d(TAG, "Получено удалений с сервера: всего=${response.deleted.size}, по сущностям=${deletedByEntity.mapValues { it.value.size }}")
            
            // Собираем ID всех записей, которые присутствуют в основных списках
            val existingIds = mutableSetOf<String>()
            response.clients.forEach { existingIds.add(it.id) }
            response.sites.forEach { existingIds.add(it.id) }
            response.installations.forEach { existingIds.add(it.id) }
            response.components.forEach { existingIds.add(it.id) }
            response.maintenance_sessions.forEach { existingIds.add(it.id) }
            response.maintenance_values.forEach { value ->
                value.id?.let { id -> existingIds.add(id) }
            }
            response.component_templates.forEach { existingIds.add(it.id) }
            response.component_template_fields.forEach { existingIds.add(it.id) }
            
            var deletedCount = 0
            var skippedCount = 0
            response.deleted.forEach { deleted ->
                // Проверяем, не присутствует ли запись в основных списках
                // Если присутствует - это не удаление, а архивирование (сервер отправляет неправильно)
                if (existingIds.contains(deleted.recordId)) {
                    Log.w(TAG, "Пропущено удаление ${deleted.getEntityName()}/${deleted.recordId}: запись присутствует в основном списке (вероятно, архивирована, а не удалена). Сервер не должен отправлять архивные записи в секции deleted.")
                    skippedCount++
                } else {
                    // Запись действительно удалена на сервере - удаляем локально
                    Log.d(TAG, "Обработка удаления ${deleted.getEntityName()}/${deleted.recordId}: запись отсутствует в основном списке, удаляем локально")
                    handleDeletedRecord(deleted)
                    deletedCount++
                }
            }
            Log.d(TAG, "Обработано удалений локально: $deletedCount, пропущено (присутствуют в основном списке): $skippedCount")
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
    
    // Вспомогательные методы для преобразования Entity -> DTO
    private fun ClientEntity.toSyncDto() = SyncClientDto(
        id = id,
        name = name,
        legalName = legalName,
        contactPerson = contactPerson,
        phone = phone,
        phone2 = phone2,
        email = email,
        addressFull = addressFull,
        city = city,
        region = region,
        country = country,
        postalCode = postalCode,
        latitude = latitude,
        longitude = longitude,
        taxId = taxId,
        vatNumber = vatNumber,
        externalId = externalId,
        tagsJson = tagsJson,
        notes = notes,
        isCorporate = isCorporate,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        sortOrder = sortOrder,
        clientGroupId = clientGroupId
    )
    
    private fun SiteEntity.toSyncDto() = SyncSiteDto(
        id = id,
        clientId = clientId,
        name = name,
        address = address,
        orderIndex = orderIndex,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch
    )
    
    private fun InstallationEntity.toSyncDto() = SyncInstallationDto(
        id = id,
        siteId = siteId,
        name = name,
        orderIndex = orderIndex,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch
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
        archivedAtEpoch = archivedAtEpoch
    )
    
    private suspend fun MaintenanceSessionEntity.toSyncDto(): SyncMaintenanceSessionDto {
        // Получаем связанные values для этой сессии
        val values = database.sessionsDao().getValuesForSession(id)
            .map { it.toSyncDto() }
        
        return SyncMaintenanceSessionDto(
            id = id,
            siteId = siteId,
            installationId = installationId,
            startedAtEpoch = startedAtEpoch,
            finishedAtEpoch = finishedAtEpoch,
            technician = technician,
            notes = notes,
            createdAtEpoch = createdAtEpoch,
            updatedAtEpoch = updatedAtEpoch,
            isArchived = isArchived,
            archivedAtEpoch = archivedAtEpoch,
            values = values.ifEmpty { null }
        )
    }
    
    private fun MaintenanceValueEntity.toSyncDto(): SyncMaintenanceValueDto {
        // Преобразуем valueText в valueNumber, если это число
        val valueNumber = valueText?.toDoubleOrNull()
        return SyncMaintenanceValueDto(
            id = id,
            sessionId = sessionId,
            siteId = siteId,
            installationId = installationId,
            componentId = componentId,
            fieldKey = fieldKey,
            valueText = if (valueNumber == null) valueText else null, // Если число, то valueText = null
            valueNumber = valueNumber,
            valueBool = valueBool,
            createdAtEpoch = createdAtEpoch.takeIf { it > 0 },
            updatedAtEpoch = updatedAtEpoch.takeIf { it > 0 }
        )
    }
    
    private fun ChecklistTemplateEntity.toSyncDto() = SyncChecklistTemplateDto(
        id = id,
        title = title,
        componentType = componentType.name,
        componentTemplateId = componentTemplateId,
        sortOrder = sortOrder ?: 0,
        createdAtEpoch = createdAtEpoch.takeIf { it > 0 },
        updatedAtEpoch = updatedAtEpoch ?: createdAtEpoch.takeIf { it > 0 } ?: System.currentTimeMillis(),
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch
    )
    
    private fun ChecklistFieldEntity.toSyncDto() = SyncChecklistFieldDto(
        id = id,
        templateId = templateId,
        key = key,
        label = label,
        type = type.name,
        unit = unit,
        minValue = min,
        maxValue = max,
        isForMaintenance = isForMaintenance,
        required = false, // TODO: добавить поле в entity если нужно
        sortOrder = 0, // TODO: добавить поле в entity если нужно
        createdAtEpoch = createdAtEpoch.takeIf { it > 0 },
        updatedAtEpoch = updatedAtEpoch.takeIf { it > 0 }
    )
    
    private fun ComponentTemplateEntity.toSyncDto() = SyncComponentTemplateDto(
        id = id,
        name = name,
        category = category,
        sortOrder = sortOrder ?: 0,
        defaultParamsJson = defaultParamsJson,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch
    )
    
    // Вспомогательные методы для применения DTO -> Room (last-write-wins)
    private suspend fun applyClientToRoom(dto: SyncClientDto) {
        val existing = database.clientDao().getClientByIdNow(dto.id)
        val entity = dto.toEntity()
        if (existing == null) {
            // Вставляем новую запись
            database.clientDao().upsertClient(entity)
        } else if (dto.updatedAtEpoch > existing.updatedAtEpoch) {
            // Обновляем существующую запись
            database.clientDao().upsertClient(entity)
        }
        // Иначе локальная версия новее или равна - ничего не делаем
    }
    
    private suspend fun applySiteToRoom(dto: SyncSiteDto) {
        val existing = database.hierarchyDao().getSiteNow(dto.id)
        val entity = dto.toEntity()
        if (existing == null) {
            database.hierarchyDao().insertSite(entity)
        } else if (dto.updatedAtEpoch > existing.updatedAtEpoch) {
            database.hierarchyDao().upsertSite(entity)
        }
    }
    
    private suspend fun applyInstallationToRoom(dto: SyncInstallationDto) {
        val existing = database.hierarchyDao().getInstallationNow(dto.id)
        val entity = dto.toEntity()
        if (existing == null) {
            database.hierarchyDao().insertInstallation(entity)
        } else if (dto.updatedAtEpoch > existing.updatedAtEpoch) {
            database.hierarchyDao().upsertInstallation(entity)
        }
    }
    
    private suspend fun applyComponentToRoom(dto: SyncComponentDto) {
        val existing = database.hierarchyDao().getComponent(dto.id)
        val entity = dto.toEntity()
        if (existing == null) {
            database.hierarchyDao().insertComponent(entity)
        } else if (dto.updatedAtEpoch > existing.updatedAtEpoch) {
            database.hierarchyDao().upsertComponent(entity)
        }
    }
    
    private suspend fun applyMaintenanceSessionToRoom(dto: SyncMaintenanceSessionDto) {
        val existing = database.sessionsDao().getSessionById(dto.id)
        val entity = dto.toEntity()
        if (existing == null) {
            database.sessionsDao().upsertSession(entity)
            // Применяем связанные values
            dto.values?.forEach { valueDto ->
                applyMaintenanceValueToRoom(valueDto)
            }
        } else if (dto.updatedAtEpoch > existing.updatedAtEpoch) {
            database.sessionsDao().upsertSession(entity)
            // Применяем связанные values
            dto.values?.forEach { valueDto ->
                applyMaintenanceValueToRoom(valueDto)
            }
        }
    }
    
    private suspend fun applyMaintenanceValueToRoom(dto: SyncMaintenanceValueDto) {
        val entity = dto.toEntity()
        val dtoId = dto.id
        val dtoUpdatedAt = dto.updatedAtEpoch
        if (dtoId != null) {
            // Проверяем существование через запрос значений для сессии
            val existing = database.sessionsDao().getValuesForSession(dto.sessionId)
                .find { it.id == dtoId }
            if (existing == null) {
                database.sessionsDao().insertValue(entity)
            } else if (dtoUpdatedAt != null && dtoUpdatedAt > existing.updatedAtEpoch) {
                // Используем upsert через insertValues
                database.sessionsDao().insertValues(listOf(entity))
            }
        } else {
            // Новое значение без ID - вставляем
            database.sessionsDao().insertValue(entity)
        }
    }
    
    /**
     * Маппинг checklist_field (старый формат API) в component_template_field (новая модель)
     */
    private suspend fun applyChecklistFieldToComponentTemplateField(dto: SyncChecklistFieldDto) {
        // Маппим templateId: в старом формате это был ID checklist_template,
        // в новом формате это должен быть ID component_template
        // Если component_template с таким ID не существует, создаем его
        var componentTemplateId = dto.templateId
        
        // Проверяем, существует ли component_template с таким ID
        val componentTemplate = database.componentTemplatesDao().getById(componentTemplateId)
        if (componentTemplate == null) {
            // Создаем component_template на основе checklist_template (если он был отправлен)
            // Или используем templateId как есть (миграция уже создала component_template с таким ID)
            // В этом случае просто используем templateId
            Log.d(TAG, "Component template не найден для checklist_field templateId=${dto.templateId}, используем как есть")
        }
        
        // Маппим поле в новую модель
        val existing = database.componentTemplateFieldsDao().getFieldsForTemplate(componentTemplateId)
            .find { it.id == dto.id }
        
        val entity = dto.toComponentTemplateFieldEntity(componentTemplateId)
        val dtoUpdatedAt = dto.updatedAtEpoch ?: 0
        
        if (existing == null) {
            database.componentTemplateFieldsDao().upsertField(entity)
        } else {
            val existingUpdatedAt = existing.updatedAtEpoch
            if (dtoUpdatedAt > existingUpdatedAt) {
                database.componentTemplateFieldsDao().upsertField(entity)
            }
        }
    }
    
    private suspend fun applyComponentTemplateToRoom(dto: SyncComponentTemplateDto) {
        val existing = database.componentTemplatesDao().getById(dto.id)
        val entity = dto.toEntity()
        val dtoUpdatedAt = dto.updatedAtEpoch
        if (existing == null) {
            database.componentTemplatesDao().upsert(entity)
        } else {
            val existingUpdatedAt = existing.updatedAtEpoch ?: 0
            if (dtoUpdatedAt > existingUpdatedAt) {
                database.componentTemplatesDao().upsert(entity)
            }
        }
    }
    
    private suspend fun handleDeletedRecord(deleted: DeletedRecordDto) {
        val entityName = deleted.getEntityName()
        if (entityName == null) {
            Log.w(TAG, "Не удалось определить тип сущности для удаления: recordId=${deleted.recordId}")
            return
        }
        
        when (entityName) {
            "clients" -> {
                database.clientDao().deleteClient(deleted.recordId)
                Log.d(TAG, "Удалён клиент: ${deleted.recordId}")
            }
            "sites" -> {
                database.hierarchyDao().deleteSite(deleted.recordId)
                Log.d(TAG, "Удалён объект: ${deleted.recordId}")
            }
            "installations" -> {
                database.hierarchyDao().deleteInstallation(deleted.recordId)
                Log.d(TAG, "Удалена установка: ${deleted.recordId}")
            }
            "components" -> {
                database.hierarchyDao().deleteComponent(deleted.recordId)
                Log.d(TAG, "Удалён компонент: ${deleted.recordId}")
            }
            "maintenance_sessions" -> {
                database.sessionsDao().deleteSession(deleted.recordId)
                Log.d(TAG, "Удалена сессия ТО: ${deleted.recordId}")
            }
            "maintenance_values" -> {
                database.sessionsDao().deleteValue(deleted.recordId)
                Log.d(TAG, "Удалено значение ТО: ${deleted.recordId}")
            }
            "component_template_fields" -> {
                val field = database.componentTemplateFieldsDao().getFieldById(deleted.recordId)
                if (field != null) {
                    database.componentTemplateFieldsDao().deleteField(deleted.recordId)
                    Log.d(TAG, "Удалено поле шаблона компонента: id=${deleted.recordId}, templateId=${field.templateId}, label=${field.label}")
                } else {
                    Log.w(TAG, "Попытка удалить несуществующее поле шаблона: id=${deleted.recordId}")
                }
            }
            "component_templates" -> {
                val template = database.componentTemplatesDao().getById(deleted.recordId)
                if (template != null) {
                    database.componentTemplatesDao().delete(template)
                    Log.d(TAG, "Удалён шаблон компонента: id=${deleted.recordId}, name=${template.name}")
                } else {
                    Log.w(TAG, "Попытка удалить несуществующий шаблон компонента: id=${deleted.recordId}")
                }
            }
            else -> Log.w(TAG, "Неизвестная сущность для удаления: ${deleted.entity}")
        }
    }
    
    // Вспомогательные методы для преобразования DTO -> Entity
    private fun SyncClientDto.toEntity() = ClientEntity(
        id = id,
        name = name,
        legalName = legalName,
        contactPerson = contactPerson,
        phone = phone,
        phone2 = phone2,
        email = email,
        addressFull = addressFull,
        city = city,
        region = region,
        country = country,
        postalCode = postalCode,
        latitude = latitude,
        longitude = longitude,
        taxId = taxId,
        vatNumber = vatNumber,
        externalId = externalId,
        tagsJson = tagsJson,
        notes = notes,
        isCorporate = isCorporate,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false, // При получении с сервера - не грязная
        syncStatus = SyncStatus.SYNCED.value,
        sortOrder = sortOrder,
        clientGroupId = clientGroupId
    )
    
    private fun SyncSiteDto.toEntity() = SiteEntity(
        id = id,
        clientId = clientId,
        name = name,
        address = address,
        orderIndex = orderIndex,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value
    )
    
    private fun SyncInstallationDto.toEntity() = InstallationEntity(
        id = id,
        siteId = siteId,
        name = name,
        orderIndex = orderIndex,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value
    )
    
    private fun SyncComponentDto.toEntity() = ComponentEntity(
        id = id,
        installationId = installationId,
        name = name,
        type = ComponentType.valueOf(type),
        orderIndex = orderIndex,
        templateId = templateId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value
    )
    
    private fun SyncMaintenanceSessionDto.toEntity() = MaintenanceSessionEntity(
        id = id,
        siteId = siteId,
        installationId = installationId,
        startedAtEpoch = startedAtEpoch,
        finishedAtEpoch = finishedAtEpoch,
        technician = technician,
        notes = notes,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value,
        synced = true
    )
    
    private fun SyncMaintenanceValueDto.toEntity(): MaintenanceValueEntity {
        val valueId = id ?: java.util.UUID.randomUUID().toString()
        // Преобразуем valueNumber в valueText, если есть
        val finalValueText = valueText ?: valueNumber?.toString()
        return MaintenanceValueEntity(
            id = valueId,
            sessionId = sessionId,
            siteId = siteId,
            installationId = installationId,
            componentId = componentId,
            fieldKey = fieldKey,
            valueText = finalValueText,
            valueBool = valueBool,
            createdAtEpoch = createdAtEpoch ?: 0,
            updatedAtEpoch = updatedAtEpoch ?: 0,
            isArchived = false,
            archivedAtEpoch = null,
            deletedAtEpoch = null,
            dirtyFlag = false,
            syncStatus = SyncStatus.SYNCED.value
        )
    }
    
    private fun SyncChecklistTemplateDto.toEntity() = ChecklistTemplateEntity(
        id = id,
        title = title ?: "", // Используем пустую строку, если title null
        componentType = ComponentType.valueOf(componentType),
        componentTemplateId = componentTemplateId,
        createdAtEpoch = createdAtEpoch ?: 0,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value,
        sortOrder = sortOrder
    )
    
    private fun SyncChecklistFieldDto.toEntity() = ChecklistFieldEntity(
        id = id,
        templateId = templateId,
        key = key,
        label = label ?: "",
        type = FieldType.valueOf(type),
        unit = unit,
        min = minValue,
        max = maxValue,
        isForMaintenance = isForMaintenance,
        createdAtEpoch = createdAtEpoch ?: 0,
        updatedAtEpoch = updatedAtEpoch ?: 0,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value
    )
    
    private fun SyncComponentTemplateDto.toEntity() = ComponentTemplateEntity(
        id = id,
        name = name,
        category = category,
        sortOrder = sortOrder,
        defaultParamsJson = defaultParamsJson,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value
    )
    
    /**
     * Маппинг ComponentTemplateFieldEntity → SyncChecklistFieldDto (для обратной совместимости с API)
     */
    private fun ComponentTemplateFieldEntity.toChecklistFieldDto() = SyncChecklistFieldDto(
        id = id,
        templateId = templateId,
        key = key,
        label = label,
        type = type.name,
        unit = unit,
        minValue = min,
        maxValue = max,
        isForMaintenance = !isCharacteristic, // Маппинг: isCharacteristic = true → isForMaintenance = false
        required = isRequired,
        sortOrder = sortOrder,
        createdAtEpoch = createdAtEpoch.takeIf { it > 0 },
        updatedAtEpoch = updatedAtEpoch.takeIf { it > 0 }
    )
    
    /**
     * Маппинг SyncChecklistFieldDto → ComponentTemplateFieldEntity (при получении с сервера)
     */
    private fun SyncChecklistFieldDto.toComponentTemplateFieldEntity(componentTemplateId: String) = ComponentTemplateFieldEntity(
        id = id,
        templateId = componentTemplateId,
        key = key,
        label = label ?: "",
        type = FieldType.valueOf(type),
        unit = unit,
        isCharacteristic = !isForMaintenance, // Маппинг обратный: isForMaintenance = false → isCharacteristic = true
        isRequired = required,
        defaultValueText = null,
        defaultValueNumber = null,
        defaultValueBool = null,
        min = minValue,
        max = maxValue,
        sortOrder = sortOrder,
        createdAtEpoch = createdAtEpoch ?: 0,
        updatedAtEpoch = updatedAtEpoch ?: 0,
        isArchived = false,
        archivedAtEpoch = null,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value
    )
    
    private fun calculatePushStats(response: SyncPushResponse): SyncPushStats {
        val processed = response.processed
        if (processed == null) {
            return SyncPushStats(0, 0, 0)
        }
        
        // Упрощенная логика: считаем, что все processed - это inserted+updated
        val total = processed.clients + processed.sites + processed.installations +
            processed.components + processed.maintenance_sessions + processed.maintenance_values +
            processed.component_templates + processed.component_template_fields
        
        val skipped = response.errors.size
        
        return SyncPushStats(
            inserted = total - skipped,
            updated = 0, // TODO: различать inserted и updated если сервер возвращает отдельно
            skipped = skipped
        )
    }
    
    private fun calculatePullStats(response: SyncPullResponse): SyncPullStats {
        val componentTemplatesCount = response.component_templates.size
        val componentTemplateFieldsCount = response.component_template_fields.size
        if (componentTemplatesCount > 0 || componentTemplateFieldsCount > 0) {
            Log.d(TAG, "Получено шаблонов компонентов: component_templates=$componentTemplatesCount, component_template_fields=$componentTemplateFieldsCount")
        }
        return SyncPullStats(
            clients = response.clients.size,
            sites = response.sites.size,
            installations = response.installations.size,
            components = response.components.size,
            sessions = response.maintenance_sessions.size,
            values = response.maintenance_values.size,
            templates = componentTemplatesCount,
            deleted = response.deleted.size
        )
    }
}

/**
 * Результат синхронизации
 */
data class SyncResult(
    val success: Boolean,
    val message: String,
    val pushStats: SyncPushStats? = null,
    val pullStats: SyncPullStats? = null
)

/**
 * Статистика отправки
 */
data class SyncPushStats(
    val inserted: Int,
    val updated: Int,
    val skipped: Int
)

/**
 * Статистика получения
 */
data class SyncPullStats(
    val clients: Int,
    val sites: Int,
    val installations: Int,
    val components: Int,
    val sessions: Int,
    val values: Int,
    val templates: Int,
    val deleted: Int
)


