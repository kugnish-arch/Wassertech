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
import ru.wassertech.data.entities.UserMembershipEntity
import ru.wassertech.data.types.ComponentType
import ru.wassertech.data.types.FieldType
import ru.wassertech.data.types.SyncStatus
import ru.wassertech.core.network.dto.SyncIconPackDto
import ru.wassertech.core.network.dto.SyncIconDto
import ru.wassertech.core.network.dto.sync.SyncUserMembershipDto
import ru.wassertech.api.ReportsApi
import ru.wassertech.core.network.dto.ReportDto
import ru.wassertech.data.entities.ReportEntity
import ru.wassertech.data.entities.SettingsEntity

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
    
    private val reportsApi: ReportsApi by lazy {
        ApiClient.createService<ReportsApi>(
            tokenStorage = tokenStorage,
            baseUrl = ApiConfig.getBaseUrl(),
            enableLogging = true
        )
    }
    
    companion object {
        private const val TAG = "SyncEngine"
        private const val SETTINGS_KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val SETTINGS_KEY_LAST_REPORTS_SYNC_EPOCH = "last_reports_sync_epoch"
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
                // ВАЖНО: сохраняем в локальную переменную, чтобы избежать smart cast проблем
                // Никаких повторных обращений pushResponse.processed.xxx — только через локальный processed
                val processed = pushResponse.processed
                Log.d(TAG, "Ответ sync/push: success=${pushResponse.success}, " +
                        "processed=$processed, errors=${pushResponse.errors.size}")
                if (processed != null) {
                    val templatesCount = processed.component_templates
                    val fieldsCount = processed.component_template_fields
                    Log.d(TAG, "Обработано сервером: component_templates=$templatesCount, " +
                            "component_template_fields=$fieldsCount")
                } else {
                    Log.w(TAG, "pushResponse.processed is null, nothing to mark as synced")
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
                // При первом запуске используем 1 вместо 0
                val lastSyncTimestampSec = if (lastSyncTimestampMs == 0L) 1L else lastSyncTimestampMs / 1000
                Log.d(TAG, "Последняя синхронизация: ${lastSyncTimestampMs}ms = ${lastSyncTimestampSec}s (${if (lastSyncTimestampMs == 0L) "первый запуск, используем since=1" else "timestamp=$lastSyncTimestampSec"})")
                
                // Указываем, какие сущности нужно получить с сервера
                // Включаем икон-паки и иконки для загрузки метаданных
                val entities = listOf(
                    "clients", "sites", "installations", "components",
                    "maintenance_sessions", "maintenance_values",
                    "component_templates", "component_template_fields",
                    "icon_packs", "icons", // Добавляем икон-паки и иконки
                    "user_membership" // Добавляем user_membership для контроля доступа
                )
                
                Log.d(TAG, "Вызываю syncPull(since=$lastSyncTimestampSec, entities=$entities)")
                
                // Запрашиваем изменения (since в секундах, всегда > 0)
                val response = syncApi.syncPull(since = lastSyncTimestampSec, entities = entities)
                
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
                // Сервер возвращает timestamp в миллисекундах, сохраняем как есть
                val timestampMs = pullResponse.timestamp
                saveLastSyncTimestamp(timestampMs)
                Log.d(TAG, "Сохранён новый timestamp последней синхронизации: ${timestampMs}ms (${timestampMs / 1000}s)")
                
                val stats = calculatePullStats(pullResponse)
                val iconPacksCount = pullResponse.iconPacks.size
                val iconsCount = pullResponse.icons.size
                
                val message = buildString {
                    append("Загружено: ")
                    append("клиентов=${stats.clients}, ")
                    append("объектов=${stats.sites}, ")
                    append("установок=${stats.installations}, ")
                    append("компонентов=${stats.components}, ")
                    append("сессий ТО=${stats.sessions}, ")
                    append("значений ТО=${stats.values}, ")
                    append("шаблонов=${stats.templates}, ")
                    if (iconPacksCount > 0 || iconsCount > 0) {
                        append("икон-паков=$iconPacksCount, ")
                        append("иконок=$iconsCount, ")
                    }
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
        
        // Собираем dirty user_membership записи
        val dirtyMemberships = database.userMembershipDao().getDirtyMembershipsNow()
        val userMembership = dirtyMemberships.map { it.toSyncDto() }
        
        if (userMembership.isNotEmpty()) {
            Log.d(TAG, "Найдено dirty user_membership записей для отправки: ${userMembership.size}")
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
            userMembership = userMembership,
            deleted = deleted
        )
    }
    
    private suspend fun processPushResponse(response: SyncPushResponse, request: SyncPushRequest) {
        // Обрабатываем ответ и обновляем статусы в Room
        // Для успешно обработанных записей - помечаем как SYNCED
        // Для записей с ошибками - помечаем как CONFLICT
        
        if (!response.success) {
            Log.e(TAG, "Push failed: success=false")
            return
        }
        
        val errors = response.errors.orEmpty()
        
        // Собираем ID записей с ошибками по типам сущностей
        val errorIdsByType = errors.groupBy { it.entityType }
            .mapValues { (_, errorList) -> errorList.map { it.entityId }.toSet() }
        
        // Обрабатываем каждую сущность
        // Clients
        val clientIds = request.clients.map { it.id }
        val clientErrorIds = errorIdsByType["clients"] ?: emptySet()
        val clientSuccessIds = clientIds.filter { it !in clientErrorIds }
        if (clientSuccessIds.isNotEmpty()) {
            database.clientDao().markClientsAsSynced(clientSuccessIds)
        }
        if (clientErrorIds.isNotEmpty()) {
            database.clientDao().markClientsAsConflict(clientErrorIds.toList())
        }
        
        // Sites
        val siteIds = request.sites.map { it.id }
        val siteErrorIds = errorIdsByType["sites"] ?: emptySet()
        val siteSuccessIds = siteIds.filter { it !in siteErrorIds }
        if (siteSuccessIds.isNotEmpty()) {
            database.hierarchyDao().markSitesAsSynced(siteSuccessIds)
        }
        if (siteErrorIds.isNotEmpty()) {
            database.hierarchyDao().markSitesAsConflict(siteErrorIds.toList())
        }
        
        // Installations
        val installationIds = request.installations.map { it.id }
        val installationErrorIds = errorIdsByType["installations"] ?: emptySet()
        val installationSuccessIds = installationIds.filter { it !in installationErrorIds }
        if (installationSuccessIds.isNotEmpty()) {
            database.hierarchyDao().markInstallationsAsSynced(installationSuccessIds)
        }
        if (installationErrorIds.isNotEmpty()) {
            database.hierarchyDao().markInstallationsAsConflict(installationErrorIds.toList())
        }
        
        // Components
        val componentIds = request.components.map { it.id }
        val componentErrorIds = errorIdsByType["components"] ?: emptySet()
        val componentSuccessIds = componentIds.filter { it !in componentErrorIds }
        if (componentSuccessIds.isNotEmpty()) {
            database.hierarchyDao().markComponentsAsSynced(componentSuccessIds)
        }
        if (componentErrorIds.isNotEmpty()) {
            database.hierarchyDao().markComponentsAsConflict(componentErrorIds.toList())
        }
        
        // Maintenance sessions
        val sessionIds = request.maintenance_sessions.map { it.id }
        val sessionErrorIds = errorIdsByType["maintenance_sessions"] ?: emptySet()
        val sessionSuccessIds = sessionIds.filter { it !in sessionErrorIds }
        if (sessionSuccessIds.isNotEmpty()) {
            database.sessionsDao().markSessionsAsSynced(sessionSuccessIds)
        }
        if (sessionErrorIds.isNotEmpty()) {
            database.sessionsDao().markSessionsAsConflict(sessionErrorIds.toList())
        }
        
        // Maintenance values
        val valueIds = request.maintenance_values.mapNotNull { it.id }
        val valueErrorIds = errorIdsByType["maintenance_values"] ?: emptySet()
        val valueSuccessIds = valueIds.filter { it !in valueErrorIds }
        if (valueSuccessIds.isNotEmpty()) {
            database.sessionsDao().markValuesAsSynced(valueSuccessIds)
        }
        if (valueErrorIds.isNotEmpty()) {
            database.sessionsDao().markValuesAsConflict(valueErrorIds.toList())
        }
        
        // Component templates
        val templateIds = request.component_templates.map { it.id }
        val templateErrorIds = errorIdsByType["component_templates"] ?: emptySet()
        val templateSuccessIds = templateIds.filter { it !in templateErrorIds }
        if (templateSuccessIds.isNotEmpty()) {
            database.componentTemplatesDao().markComponentTemplatesAsSynced(templateSuccessIds)
            Log.d(TAG, "Помечено как синхронизировано шаблонов компонентов: ${templateSuccessIds.size}")
        }
        if (templateErrorIds.isNotEmpty()) {
            database.componentTemplatesDao().markComponentTemplatesAsConflict(templateErrorIds.toList())
            Log.w(TAG, "Помечено как конфликт шаблонов компонентов: ${templateErrorIds.size}")
        }
        
        // Component template fields
        val fieldIds = request.component_template_fields.map { it.id }
        val fieldErrorIds = errorIdsByType["component_template_fields"] ?: emptySet()
        val fieldSuccessIds = fieldIds.filter { it !in fieldErrorIds }
        if (fieldSuccessIds.isNotEmpty()) {
            database.componentTemplateFieldsDao().markComponentTemplateFieldsAsSynced(fieldSuccessIds)
            Log.d(TAG, "Помечено как синхронизировано полей шаблонов компонентов: ${fieldSuccessIds.size}")
        }
        if (fieldErrorIds.isNotEmpty()) {
            database.componentTemplateFieldsDao().markComponentTemplateFieldsAsConflict(fieldErrorIds.toList())
            Log.w(TAG, "Помечено как конфликт полей шаблонов компонентов: ${fieldErrorIds.size}")
        }
        
        // User membership
        request.userMembership.forEach { dto ->
            val errorIds = errorIdsByType["user_membership"] ?: emptySet()
            // Для user_membership используем составной ключ, поэтому проверяем по всем трём полям
            val membershipKey = "${dto.userId}:${dto.scope}:${dto.targetId}"
            val hasError = errorIds.any { it == membershipKey || it == dto.targetId }
            
            if (!hasError) {
                database.userMembershipDao().markAsSynced(dto.userId, dto.scope, dto.targetId)
            } else {
                database.userMembershipDao().markAsConflict(dto.userId, dto.scope, dto.targetId)
            }
        }
        if (request.userMembership.isNotEmpty()) {
            val successCount = request.userMembership.count { dto ->
                val errorIds = errorIdsByType["user_membership"] ?: emptySet()
                val membershipKey = "${dto.userId}:${dto.scope}:${dto.targetId}"
                !errorIds.any { it == membershipKey || it == dto.targetId }
            }
            val errorCount = request.userMembership.size - successCount
            Log.d(TAG, "Обработано user_membership: успешно=$successCount, ошибок=$errorCount")
        }
        
        // Обрабатываем удаления: помечаем как синхронизированные после успешного push
        val deletedRecords = database.deletedRecordsDao().getDirtyDeletedRecordsNow()
        val deletedRecordIds = deletedRecords.map { it.id }
        if (deletedRecordIds.isNotEmpty()) {
            database.deletedRecordsDao().markAsSynced(deletedRecordIds)
            database.deletedRecordsDao().deleteAllSynced()
            Log.d(TAG, "Помечено как синхронизировано удалений: ${deletedRecordIds.size}")
        }
        
        if (errors.isNotEmpty()) {
            Log.w(TAG, "Ошибки при синхронизации: ${errors.size}")
            errors.forEach { error ->
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
                // Пропускаем иконки без packId
                if (dto.packId.isNullOrBlank()) {
                    Log.w(TAG, "Пропуск иконки ${dto.id} (${dto.label}): packId отсутствует или пустой")
                    skippedCount++
                    return@forEach
                }
                try {
                    val entity = dto.toEntity()
                    database.iconDao().upsert(entity)
                    appliedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обработке иконки ${dto.id} (${dto.label}): ${e.message}", e)
                    skippedCount++
                }
            }
            Log.d(TAG, "Обработано иконок: применено=$appliedCount, пропущено=$skippedCount")
            
            // Загружаем миниатюры для новых/обновлённых иконок в фоне
            try {
                val iconRepository = ru.wassertech.data.repository.IconRepository(context)
                val downloadResult = iconRepository.downloadMissingThumbnails()
                if (downloadResult.isSuccess) {
                    Log.d(TAG, "Загружено миниатюр: ${downloadResult.getOrNull()}")
                } else {
                    Log.w(TAG, "Ошибка при загрузке миниатюр: ${downloadResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке миниатюр после синхронизации", e)
                // Не прерываем синхронизацию из-за ошибок загрузки миниатюр
            }
        }
        
        // Обрабатываем user_membership
        val userMembershipList = response.userMembership
        if (userMembershipList != null && userMembershipList.isNotEmpty()) {
            Log.d(TAG, "Получено user_membership записей с сервера: ${userMembershipList.size}")
            var appliedCount = 0
            userMembershipList.forEach { dto ->
                try {
                    val entity = dto.toEntity()
                    database.userMembershipDao().upsert(entity)
                    appliedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при обработке user_membership: userId=${dto.userId}, scope=${dto.scope}, targetId=${dto.targetId}, error=${e.message}", e)
                }
            }
            Log.d(TAG, "Обработано user_membership записей: применено=$appliedCount")
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
            response.iconPacks.forEach { existingIds.add(it.id) }
            response.icons.forEach { existingIds.add(it.id) }
            // Для user_membership используем составной ключ (userId, scope, targetId)
            // поэтому не добавляем в existingIds
            
            var deletedCount = 0
            var skippedCount = 0
            response.deleted.forEach { deleted ->
                val entityName = deleted.getEntityName()
                val recordId = deleted.recordId
                
                // Проверяем, что recordId не пустой
                if (recordId.isNullOrBlank()) {
                    Log.w(TAG, "Пропускаем удаление $entityName: пустой id")
                    skippedCount++
                    return@forEach
                }
                
                // Проверяем, не присутствует ли запись в основных списках
                // Если присутствует - это не удаление, а архивирование (сервер отправляет неправильно)
                if (existingIds.contains(recordId)) {
                    Log.w(TAG, "Пропущено удаление $entityName/$recordId: запись присутствует в основном списке (вероятно, архивирована, а не удалена). Сервер не должен отправлять архивные записи в секции deleted.")
                    skippedCount++
                } else {
                    // Запись действительно удалена на сервере - удаляем локально
                    Log.d(TAG, "Обработка удаления $entityName/$recordId: запись отсутствует в основном списке, удаляем локально")
                    handleDeletedRecord(entityName, recordId)
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
            origin = origin,
            created_by_user_id = createdByUserId,
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
            updatedAtEpoch = updatedAtEpoch.takeIf { it > 0 },
            origin = origin,
            created_by_user_id = createdByUserId
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
        archivedAtEpoch = archivedAtEpoch,
        origin = origin,
        created_by_user_id = createdByUserId
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
    
    private suspend fun handleDeletedRecord(entity: String?, recordId: String?) {
        // Защита от null/пустого id
        if (recordId.isNullOrBlank()) {
            Log.w(TAG, "Пропускаем удаление $entity: пустой id")
            return
        }
        
        // Защита от null entity
        if (entity == null) {
            Log.w(TAG, "Пропускаем удаление: не удалось определить тип сущности для recordId=$recordId")
            return
        }
        
        when (entity) {
            "clients" -> {
                database.clientDao().deleteClient(recordId)
                Log.d(TAG, "Удалён клиент: $recordId")
            }
            "client_groups" -> {
                database.clientDao().deleteGroup(recordId)
                Log.d(TAG, "Удалена группа клиентов: $recordId")
            }
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
            "maintenance_sessions" -> {
                database.sessionsDao().deleteSession(recordId)
                Log.d(TAG, "Удалена сессия ТО: $recordId")
            }
            "maintenance_values" -> {
                database.sessionsDao().deleteValue(recordId)
                Log.d(TAG, "Удалено значение ТО: $recordId")
            }
            "component_template_fields" -> {
                val field = database.componentTemplateFieldsDao().getFieldById(recordId)
                if (field != null) {
                    database.componentTemplateFieldsDao().deleteField(recordId)
                    Log.d(TAG, "Удалено поле шаблона компонента: id=$recordId, templateId=${field.templateId}, label=${field.label}")
                } else {
                    Log.w(TAG, "Попытка удалить несуществующее поле шаблона: id=$recordId")
                }
            }
            "component_templates" -> {
                val template = database.componentTemplatesDao().getById(recordId)
                if (template != null) {
                    database.componentTemplatesDao().delete(template)
                    Log.d(TAG, "Удалён шаблон компонента: id=$recordId, name=${template.name}")
                } else {
                    Log.w(TAG, "Попытка удалить несуществующий шаблон компонента: id=$recordId")
                }
            }
            else -> Log.w(TAG, "Неизвестная сущность для удаления: $entity")
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
        iconId = iconId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value,
        origin = origin ?: "CRM", // По умолчанию CRM для старых данных
        createdByUserId = created_by_user_id
    )
    
    private fun SyncInstallationDto.toEntity() = InstallationEntity(
        id = id,
        siteId = siteId,
        name = name,
        orderIndex = orderIndex,
        iconId = iconId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value,
        origin = origin ?: "CRM", // По умолчанию CRM для старых данных
        createdByUserId = created_by_user_id
    )
    
    private fun SyncComponentDto.toEntity() = ComponentEntity(
        id = id,
        installationId = installationId,
        name = name,
        type = ComponentType.valueOf(type),
        orderIndex = orderIndex,
        templateId = templateId,
        iconId = iconId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        deletedAtEpoch = null,
        dirtyFlag = false,
        syncStatus = SyncStatus.SYNCED.value,
        origin = origin ?: "CRM", // По умолчанию CRM для старых данных
        createdByUserId = created_by_user_id
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
        synced = true,
        origin = origin ?: "CRM", // По умолчанию CRM для старых данных
        createdByUserId = created_by_user_id
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
            origin = origin ?: "CRM", // По умолчанию CRM для старых данных
            createdByUserId = created_by_user_id,
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
        syncStatus = SyncStatus.SYNCED.value,
        origin = origin ?: "CRM", // По умолчанию CRM для старых данных
        createdByUserId = created_by_user_id
    )
    
    private fun SyncIconPackDto.toEntity() = IconPackEntity(
        id = id,
        code = code,
        name = name,
        description = description,
        folder = folder,
        isBuiltin = isBuiltin,
        isPremium = isPremium,
        origin = origin ?: "CRM", // По умолчанию CRM для старых данных
        createdByUserId = createdByUserId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch
    )
    
    private fun SyncIconDto.toEntity(): IconEntity {
        // packId должен быть не null, проверка уже выполнена в processPullResponse
        val packIdValue = packId ?: throw IllegalArgumentException("packId не может быть null для иконки $id")
        
        // entityType может быть null на сервере, используем "ANY" по умолчанию
        val entityTypeValue = entityType ?: "ANY"
        
        return IconEntity(
            id = id,
            packId = packIdValue,
            code = code,
            label = label,
            entityType = entityTypeValue,
            imageUrl = imageUrl,
            thumbnailUrl = thumbnailUrl,
            thumbnailLocalPath = null, // Будет заполнено после загрузки миниатюры
            androidResName = androidResName,
            isActive = isActive,
            origin = origin ?: "CRM", // По умолчанию CRM для старых данных
            createdByUserId = createdByUserId,
            createdAtEpoch = createdAtEpoch,
            updatedAtEpoch = updatedAtEpoch
        )
    }
    
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
        // Упрощённая статистика: считаем только ошибки как пропущенные
        // inserted и updated считаем как 0, так как мы не парсим result
        val skipped = response.errors.size
        return SyncPushStats(
            inserted = 0,
            updated = 0,
            skipped = skipped
        )
    }
    
    private fun calculatePullStats(response: SyncPullResponse): SyncPullStats {
        val componentTemplatesCount = response.component_templates.size
        val componentTemplateFieldsCount = response.component_template_fields.size
        val iconPacksCount = response.iconPacks.size
        val iconsCount = response.icons.size
        
        if (componentTemplatesCount > 0 || componentTemplateFieldsCount > 0) {
            Log.d(TAG, "Получено шаблонов компонентов: component_templates=$componentTemplatesCount, component_template_fields=$componentTemplateFieldsCount")
        }
        
        if (iconPacksCount > 0 || iconsCount > 0) {
            Log.d(TAG, "Получено иконок: icon_packs=$iconPacksCount, icons=$iconsCount")
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
    
    // Маппинг UserMembershipEntity ↔ SyncUserMembershipDto
    private fun UserMembershipEntity.toSyncDto() = ru.wassertech.core.network.dto.sync.SyncUserMembershipDto(
        id = null, // Составной ключ не требует id
        userId = userId,
        scope = scope,
        targetId = targetId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch
    )
    
    private fun ru.wassertech.core.network.dto.sync.SyncUserMembershipDto.toEntity() = UserMembershipEntity(
        userId = userId,
        scope = scope,
        targetId = targetId,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch,
        isArchived = isArchived,
        archivedAtEpoch = archivedAtEpoch,
        dirtyFlag = false, // При получении с сервера - не dirty
        syncStatus = SyncStatus.SYNCED.value
    )
    
    /**
     * Синхронизация отчётов с сервером.
     * Отчёты синхронизируются через отдельный API GET /reports с инкрементальной загрузкой.
     */
    suspend fun syncReports(): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало синхронизации отчётов")
                
                // Получаем timestamp последней синхронизации отчётов
                val lastSyncEpoch = settingsDao.getValueSync(SETTINGS_KEY_LAST_REPORTS_SYNC_EPOCH)?.toLongOrNull() ?: 0L
                
                // Если в БД есть отчёты, используем максимальный updatedAtEpoch
                val maxUpdatedAtEpoch = database.reportDao().getMaxUpdatedAtEpoch()
                val sinceUpdatedAtEpoch = if (maxUpdatedAtEpoch != null && maxUpdatedAtEpoch > lastSyncEpoch) {
                    maxUpdatedAtEpoch
                } else {
                    lastSyncEpoch
                }
                
                Log.d(TAG, "Синхронизация отчётов с sinceUpdatedAtEpoch=$sinceUpdatedAtEpoch")
                
                // Запрашиваем отчёты с сервера
                // Для app-crm используем GET /reports без фильтров (получаем все доступные отчёты)
                val response = reportsApi.getReports(
                    sessionId = null,
                    installationId = null,
                    limit = null,
                    offset = null
                )
                
                if (!response.isSuccessful) {
                    val error = HttpException(response)
                    Log.e(TAG, "Ошибка получения отчётов: HTTP ${response.code()}")
                    return@withContext Result.failure(error)
                }
                
                val reportsDto = response.body()
                if (reportsDto == null) {
                    Log.w(TAG, "Пустой ответ при получении отчётов")
                    return@withContext Result.success(0)
                }
                
                // Фильтруем отчёты по sinceUpdatedAtEpoch (на клиенте, так как сервер может не поддерживать этот параметр в GET /reports)
                val filteredReports = if (sinceUpdatedAtEpoch > 0) {
                    reportsDto.filter { report ->
                        val updatedAt = report.updatedAtEpoch ?: report.createdAtEpoch
                        updatedAt > sinceUpdatedAtEpoch
                    }
                } else {
                    reportsDto
                }
                
                Log.d(TAG, "Получено ${filteredReports.size} отчётов (из ${reportsDto.size} всего)")
                
                if (filteredReports.isEmpty()) {
                    Log.d(TAG, "Нет новых отчётов для синхронизации")
                    return@withContext Result.success(0)
                }
                
                // Преобразуем DTO в Entity и сохраняем в БД
                val reportsEntity = filteredReports.map { dto ->
                    ReportEntity(
                        id = dto.id,
                        sessionId = dto.sessionId,
                        clientId = dto.clientId,
                        siteId = dto.siteId,
                        installationId = dto.installationId,
                        fileName = dto.fileName,
                        fileUrl = dto.fileUrl,
                        filePath = dto.filePath,
                        fileSize = dto.fileSize,
                        mimeType = dto.mimeType,
                        createdAtEpoch = dto.createdAtEpoch,
                        updatedAtEpoch = dto.updatedAtEpoch ?: dto.createdAtEpoch,
                        createdByUserId = dto.createdByUserId,
                        isArchived = dto.isArchived != 0
                    )
                }
                
                database.reportDao().insertOrUpdateReports(reportsEntity)
                Log.d(TAG, "Сохранено ${reportsEntity.size} отчётов в БД")
                
                // Обновляем timestamp последней синхронизации
                val maxUpdatedAt = reportsEntity.maxOfOrNull { it.updatedAtEpoch ?: it.createdAtEpoch } ?: sinceUpdatedAtEpoch
                settingsDao.setValue(
                    SettingsEntity(
                        key = SETTINGS_KEY_LAST_REPORTS_SYNC_EPOCH,
                        value = maxUpdatedAt.toString()
                    )
                )
                Log.d(TAG, "Обновлён timestamp последней синхронизации отчётов: $maxUpdatedAt")
                
                Result.success(reportsEntity.size)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при синхронизации отчётов", e)
                Result.failure(e)
            }
        }
    }
}


