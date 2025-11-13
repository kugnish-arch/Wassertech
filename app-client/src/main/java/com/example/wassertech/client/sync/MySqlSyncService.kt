package ru.wassertech.client.sync

import android.util.Log
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * УСТАРЕВШИЙ КОД: Прямое подключение к MySQL больше не используется для загрузки установок.
 * Установки теперь загружаются через HTTP API (см. InstallationsRepository).
 * 
 * Этот сервис оставлен для совместимости со старым кодом синхронизации в SettingsScreen,
 * но в будущем должен быть полностью заменен на API.
 */
object MySqlSyncService {
    
    // УСТАРЕВШИЙ КОД: Прямое подключение к MySQL
    // Теперь используется HTTP API через WassertechApi
    @Deprecated("Используйте HTTP API вместо прямого подключения к MySQL")
    private const val DB_URL = "jdbc:mysql://kugnis.beget.tech:3306/kugnis_app?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    
    @Deprecated("Используйте HTTP API вместо прямого подключения к MySQL")
    private const val DB_USER = "kugnis_app"
    
    @Deprecated("Используйте HTTP API вместо прямого подключения к MySQL")
    private const val DB_PASSWORD = "Umnik1985!"
    
    private const val TAG = "MySQLSync"
    
    /**
     * Безопасно получает значение isArchived из ResultSet.
     * Если столбец не существует, возвращает false (не архивирован).
     */
    private fun getIsArchivedSafe(rs: java.sql.ResultSet): Boolean {
        return try {
            rs.getBoolean("isArchived")
        } catch (e: SQLException) {
            Log.w(TAG, "Столбец isArchived не найден, используется значение по умолчанию false", e)
            false
        }
    }
    
    /**
     * Безопасно получает значение archivedAtEpoch из ResultSet.
     * Если столбец не существует, возвращает null.
     * Пробует различные варианты названия столбца, включая возможные опечатки.
     */
    private fun getArchivedAtEpochSafe(rs: java.sql.ResultSet): Long? {
        // Список возможных вариантов названия столбца
        val columnNames = listOf(
            "archivedAtEpoch",  // Правильное название
            "archievedEtEpoch", // Опечатка из сообщения пользователя
            "archived_at_epoch", // snake_case вариант
            "archivedAt"         // Сокращенный вариант
        )
        
        // Пробуем найти столбец по метаданным
        val metaData = rs.metaData
        val columnCount = metaData.columnCount
        var foundColumnName: String? = null
        
        // Ищем столбец, который похож на archivedAtEpoch
        for (i in 1..columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnNameLower = columnName.lowercase()
            if (columnNameLower.contains("archive") && 
                (columnNameLower.contains("epoch") || columnNameLower.contains("at"))) {
                foundColumnName = columnName
                break
            }
        }
        
        // Пробуем прочитать значение
        val namesToTry = if (foundColumnName != null) {
            listOf(foundColumnName) + columnNames
        } else {
            columnNames
        }
        
        for (columnName in namesToTry) {
            try {
                val value = rs.getObject(columnName)
                if (value != null) {
                    return when (value) {
                        is Long -> value
                        is Int -> value.toLong()
                        is Number -> value.toLong()
                        else -> null
                    }
                }
            } catch (e: SQLException) {
                // Продолжаем пробовать следующий вариант
                continue
            }
        }
        
        Log.w(TAG, "Столбец archivedAtEpoch не найден, используется значение null")
        return null
    }
    
    /**
     * УСТАРЕВШИЙ МЕТОД: Получает только клиентов из удалённой MySQL БД и сохраняет в локальную Room БД
     * 
     * @deprecated Используйте HTTP API вместо прямого подключения к MySQL
     */
    @Deprecated("Используйте HTTP API вместо прямого подключения к MySQL")
    suspend fun pullClientsFromRemote(db: AppDatabase): String {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Начало получения клиентов из MySQL")
            
            // Загружаем драйвер MySQL
            try {
                Class.forName("com.mysql.jdbc.Driver")
                Log.d(TAG, "MySQL драйвер успешно загружен")
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "Ошибка загрузки MySQL драйвера", e)
                throw RuntimeException("Не удалось загрузить MySQL драйвер: ${e.message}", e)
            }
            
            // Подключаемся к БД
            try {
                Log.d(TAG, "Попытка подключения к MySQL: $DB_URL")
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
                Log.d(TAG, "Успешное подключение к MySQL")
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка подключения к MySQL БД", e)
                throw RuntimeException("Не удалось подключиться к MySQL БД: ${e.message}", e)
            }
            
            // Получаем группы клиентов (перед клиентами, так как клиенты ссылаются на группы)
            val clientGroups = pullClientGroups(connection)
            Log.d(TAG, "Получено групп клиентов из MySQL: ${clientGroups.size}")
            clientGroups.forEach { group ->
                try {
                    db.clientDao().upsertGroup(group)
                    Log.d(TAG, "✓ Группа клиентов сохранена: ${group.title} (id: ${group.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении группы клиентов ${group.id}: ${e.message}", e)
                }
            }
            
            // Получаем всех клиентов
            val clients = pullClients(connection)
            Log.d(TAG, "Получено клиентов из MySQL: ${clients.size}")
            clients.forEach { client ->
                try {
                    db.clientDao().upsertClient(client)
                    Log.d(TAG, "✓ Клиент сохранен: ${client.name} (id: ${client.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении клиента ${client.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "Успешно получено клиентов: ${clients.size}")
            return "Успешно получено клиентов: ${clients.size}"
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении клиентов из MySQL", e)
            throw RuntimeException("Ошибка синхронизации: ${e.message}", e)
        } finally {
            try {
                connection?.close()
                Log.d(TAG, "Соединение с MySQL закрыто")
            } catch (closeEx: Exception) {
                Log.e(TAG, "Ошибка при закрытии соединения", closeEx)
            }
        }
    }
    
    /**
     * УСТАРЕВШИЙ МЕТОД: Получает клиентов из удалённой MySQL БД и сохраняет в локальную Room БД
     * Затем получает установки и сессии обслуживания для выбранного клиента
     * 
     * @deprecated Используйте HTTP API вместо прямого подключения к MySQL
     * Установки теперь загружаются через InstallationsRepository и WassertechApi
     */
    @Deprecated("Используйте HTTP API вместо прямого подключения к MySQL")
    suspend fun pullClientDataFromRemote(db: AppDatabase, clientId: String): String {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Начало получения данных из MySQL для клиента: $clientId")
            
            // Загружаем драйвер MySQL
            try {
                Class.forName("com.mysql.jdbc.Driver")
                Log.d(TAG, "MySQL драйвер успешно загружен")
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "Ошибка загрузки MySQL драйвера", e)
                throw RuntimeException("Не удалось загрузить MySQL драйвер: ${e.message}", e)
            }
            
            // Подключаемся к БД
            try {
                Log.d(TAG, "Попытка подключения к MySQL: $DB_URL")
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
                Log.d(TAG, "Успешное подключение к MySQL")
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка подключения к MySQL БД", e)
                throw RuntimeException("Не удалось подключиться к MySQL БД: ${e.message}", e)
            }
            
            var totalRecords = 0
            
            // Получаем группы клиентов (перед клиентами, так как клиенты ссылаются на группы)
            val clientGroups = pullClientGroups(connection)
            Log.d(TAG, "Получено групп клиентов из MySQL: ${clientGroups.size}")
            clientGroups.forEach { group ->
                try {
                    db.clientDao().upsertGroup(group)
                    Log.d(TAG, "✓ Группа клиентов сохранена: ${group.title} (id: ${group.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении группы клиентов ${group.id}: ${e.message}", e)
                }
            }
            
            // Получаем всех клиентов (чтобы заполнить список)
            val clients = pullClients(connection)
            Log.d(TAG, "Получено клиентов из MySQL: ${clients.size}")
            clients.forEach { client ->
                try {
                    db.clientDao().upsertClient(client)
                    Log.d(TAG, "✓ Клиент сохранен: ${client.name} (id: ${client.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении клиента ${client.id}: ${e.message}", e)
                }
            }
            totalRecords += clients.size
            
            // Получаем объекты (sites) для выбранного клиента
            val sites = pullSitesForClient(connection, clientId)
            Log.d(TAG, "Получено объектов для клиента: ${sites.size}")
            val siteIds = sites.map { it.id }
            
            sites.forEach { site ->
                try {
                    db.hierarchyDao().insertSite(site)
                    Log.d(TAG, "✓ Объект сохранен: ${site.name} (id: ${site.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении объекта ${site.id}: ${e.message}", e)
                }
            }
            totalRecords += sites.size
            
            // Получаем установки для объектов выбранного клиента
            val installations = if (siteIds.isNotEmpty()) {
                pullInstallationsForSites(connection, siteIds)
            } else {
                emptyList()
            }
            Log.d(TAG, "Получено установок для клиента: ${installations.size}")
            
            installations.forEach { installation ->
                try {
                    db.hierarchyDao().insertInstallation(installation)
                    Log.d(TAG, "✓ Установка сохранена: ${installation.name} (id: ${installation.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении установки ${installation.id}: ${e.message}", e)
                }
            }
            totalRecords += installations.size
            
            // Получаем сессии обслуживания для объектов выбранного клиента
            val sessions = if (siteIds.isNotEmpty()) {
                pullSessionsForSites(connection, siteIds)
            } else {
                emptyList()
            }
            Log.d(TAG, "Получено сессий обслуживания для клиента: ${sessions.size}")
            
            sessions.forEach { session ->
                try {
                    db.sessionsDao().insertSession(session)
                    Log.d(TAG, "✓ Сессия сохранена: ${session.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении сессии ${session.id}: ${e.message}", e)
                }
            }
            totalRecords += sessions.size
            
            Log.d(TAG, "Успешно получено записей: $totalRecords")
            return "Успешно получено записей: $totalRecords (клиентов: ${clients.size}, объектов: ${sites.size}, установок: ${installations.size}, сессий: ${sessions.size})"
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных из MySQL", e)
            throw RuntimeException("Ошибка синхронизации: ${e.message}", e)
        } finally {
            try {
                connection?.close()
                Log.d(TAG, "Соединение с MySQL закрыто")
            } catch (closeEx: Exception) {
                Log.e(TAG, "Ошибка при закрытии соединения", closeEx)
            }
        }
    }
    
    private fun pullClients(conn: Connection): List<ClientEntity> {
        val sql = "SELECT * FROM clients"
        val rs = conn.createStatement().executeQuery(sql)
        val clients = mutableListOf<ClientEntity>()
        
        Log.d(TAG, "Выполнение запроса: SELECT * FROM clients")
        while (rs.next()) {
            clients.add(
                ClientEntity(
                    id = rs.getString("id"),
                    name = rs.getString("name"),
                    legalName = rs.getString("legalName"),
                    contactPerson = rs.getString("contactPerson"),
                    phone = rs.getString("phone"),
                    phone2 = rs.getString("phone2"),
                    email = rs.getString("email"),
                    addressFull = rs.getString("addressFull"),
                    city = rs.getString("city"),
                    region = rs.getString("region"),
                    country = rs.getString("country"),
                    postalCode = rs.getString("postalCode"),
                    latitude = rs.getObject("latitude") as? Double,
                    longitude = rs.getObject("longitude") as? Double,
                    taxId = rs.getString("taxId"),
                    vatNumber = rs.getString("vatNumber"),
                    externalId = rs.getString("externalId"),
                    tagsJson = rs.getString("tagsJson"),
                    notes = rs.getString("notes"),
                    isCorporate = rs.getBoolean("isCorporate"),
                    isArchived = getIsArchivedSafe(rs),
                    archivedAtEpoch = getArchivedAtEpochSafe(rs),
                    createdAtEpoch = rs.getLong("createdAtEpoch"),
                    updatedAtEpoch = rs.getLong("updatedAtEpoch"),
                    sortOrder = rs.getInt("sortOrder"),
                    clientGroupId = rs.getString("clientGroupId")
                )
            )
        }
        rs.close()
        Log.d(TAG, "Прочитано клиентов из MySQL: ${clients.size}")
        return clients
    }
    
    private fun pullSitesForClient(conn: Connection, clientId: String): List<SiteEntity> {
        val sql = "SELECT * FROM sites WHERE clientId = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setString(1, clientId)
        val rs = stmt.executeQuery()
        val sites = mutableListOf<SiteEntity>()
        
        while (rs.next()) {
            sites.add(
                SiteEntity(
                    id = rs.getString("id"),
                    clientId = rs.getString("clientId"),
                    name = rs.getString("name"),
                    address = rs.getString("address"),
                    orderIndex = rs.getInt("orderIndex"),
                    isArchived = getIsArchivedSafe(rs),
                    archivedAtEpoch = getArchivedAtEpochSafe(rs)
                )
            )
        }
        rs.close()
        stmt.close()
        return sites
    }
    
    private fun pullInstallationsForSites(conn: Connection, siteIds: List<String>): List<InstallationEntity> {
        if (siteIds.isEmpty()) return emptyList()
        
        val placeholders = siteIds.joinToString(",") { "?" }
        val sql = "SELECT * FROM installations WHERE siteId IN ($placeholders)"
        val stmt = conn.prepareStatement(sql)
        siteIds.forEachIndexed { index, siteId ->
            stmt.setString(index + 1, siteId)
        }
        val rs = stmt.executeQuery()
        val installations = mutableListOf<InstallationEntity>()
        
        while (rs.next()) {
            installations.add(
                InstallationEntity(
                    id = rs.getString("id"),
                    siteId = rs.getString("siteId"),
                    name = rs.getString("name"),
                    orderIndex = rs.getInt("orderIndex"),
                    isArchived = getIsArchivedSafe(rs),
                    archivedAtEpoch = getArchivedAtEpochSafe(rs)
                )
            )
        }
        rs.close()
        stmt.close()
        return installations
    }
    
    private fun pullSessionsForSites(conn: Connection, siteIds: List<String>): List<MaintenanceSessionEntity> {
        if (siteIds.isEmpty()) return emptyList()
        
        val placeholders = siteIds.joinToString(",") { "?" }
        val sql = "SELECT * FROM maintenance_sessions WHERE siteId IN ($placeholders)"
        val stmt = conn.prepareStatement(sql)
        siteIds.forEachIndexed { index, siteId ->
            stmt.setString(index + 1, siteId)
        }
        val rs = stmt.executeQuery()
        val sessions = mutableListOf<MaintenanceSessionEntity>()
        
        while (rs.next()) {
            sessions.add(
                MaintenanceSessionEntity(
                    id = rs.getString("id"),
                    siteId = rs.getString("siteId"),
                    installationId = rs.getString("installationId"),
                    startedAtEpoch = rs.getLong("startedAtEpoch"),
                    finishedAtEpoch = rs.getObject("finishedAtEpoch") as? Long,
                    technician = rs.getString("technician"),
                    notes = rs.getString("notes"),
                    synced = rs.getBoolean("synced")
                )
            )
        }
        rs.close()
        stmt.close()
        return sessions
    }
    
    private fun pullClientGroups(conn: Connection): List<ClientGroupEntity> {
        val sql = "SELECT * FROM client_groups"
        val rs = conn.createStatement().executeQuery(sql)
        val groups = mutableListOf<ClientGroupEntity>()
        
        Log.d(TAG, "Выполнение запроса: SELECT * FROM client_groups")
        while (rs.next()) {
            groups.add(
                ClientGroupEntity(
                    id = rs.getString("id"),
                    title = rs.getString("title"),
                    notes = rs.getString("notes"),
                    sortOrder = rs.getInt("sortOrder"),
                    isArchived = getIsArchivedSafe(rs),
                    archivedAtEpoch = getArchivedAtEpochSafe(rs),
                    createdAtEpoch = if (rs.getObject("createdAtEpoch") != null) rs.getLong("createdAtEpoch") else System.currentTimeMillis(),
                    updatedAtEpoch = if (rs.getObject("updatedAtEpoch") != null) rs.getLong("updatedAtEpoch") else System.currentTimeMillis()
                )
            )
        }
        rs.close()
        Log.d(TAG, "Прочитано групп клиентов из MySQL: ${groups.size}")
        return groups
    }
}

