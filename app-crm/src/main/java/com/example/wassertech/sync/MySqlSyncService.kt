package ru.wassertech.sync

import android.util.Log
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * @deprecated Используйте REST API вместо прямого доступа к MySQL.
 * - Авторизация: используйте AuthRepository.login() и AuthRepository.loadCurrentUser()
 * - Синхронизация: используйте SyncEngine.syncFull(), SyncEngine.syncPush(), SyncEngine.syncPull()
 * 
 * Этот класс оставлен в проекте для обратной совместимости, но больше не должен использоваться.
 */
@Deprecated(
    message = "Используйте REST API через AuthRepository (авторизация) и SyncEngine (синхронизация)",
    replaceWith = ReplaceWith("ru.wassertech.auth.AuthRepository или ru.wassertech.sync.SyncEngine")
)
object MySqlSyncService {
    
    private const val DB_URL = "jdbc:mysql://kugnis.beget.tech:3306/kugnis_app?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    private const val DB_USER = "kugnis_app"
    private const val DB_PASSWORD = "Umnik1985!"
    
    private const val TAG = "MySQLSync"
    
    /**
     * Отправляет данные из локальной Room БД в удалённую MySQL БД
     * ВАЖНО: Этот метод должен вызываться из suspend-функции или корутины
     */
    suspend fun pushToRemote(db: AppDatabase): String {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Начало отправки данных в MySQL")
            
            // Загружаем драйвер MySQL (старая версия использует com.mysql.jdbc.Driver)
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
                connection.autoCommit = false
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка подключения к MySQL БД", e)
                Log.e(TAG, "URL: $DB_URL")
                Log.e(TAG, "Пользователь: $DB_USER")
                throw RuntimeException("Не удалось подключиться к MySQL БД: ${e.message}", e)
            }
            
            var totalRecords = 0
            
            // Создаём таблицы, если их нет
            createTablesIfNotExist(connection)
            
            // Синхронизируем группы клиентов (перед клиентами, так как клиенты ссылаются на группы)
            val clientGroups = db.clientDao().getAllGroupsNow()
            totalRecords += syncClientGroups(connection, clientGroups)
            
            // Синхронизируем клиентов
            val clients = db.clientDao().getAllClientsNow()
            totalRecords += syncClients(connection, clients)
            
            // Синхронизируем объекты
            val sites = db.hierarchyDao().getAllSitesNow()
            totalRecords += syncSites(connection, sites)
            
            // Синхронизируем установки
            val installations = db.hierarchyDao().getAllInstallationsNow()
            totalRecords += syncInstallations(connection, installations)
            
            // Синхронизируем компоненты
            val components = db.hierarchyDao().getAllComponentsNow()
            totalRecords += syncComponents(connection, components)
            
            // Синхронизируем сессии ТО
            val sessions = db.sessionsDao().getAllSessionsNow()
            totalRecords += syncSessions(connection, sessions)
            
            // Синхронизируем значения ТО
            val values = db.sessionsDao().getAllValuesNow()
            totalRecords += syncMaintenanceValues(connection, values)
            
            // Синхронизируем шаблоны
            val templates = db.templatesDao().getAllTemplatesNow()
            totalRecords += syncTemplates(connection, templates)
            
            // Синхронизируем поля шаблонов
            val fields = db.templatesDao().getAllFieldsNow()
            totalRecords += syncTemplateFields(connection, fields)
            
            // Синхронизируем удаления
            val deletedRecords = db.deletedRecordsDao().getAllDeletedRecordsNow()
            val deletedCount = syncDeletions(connection, deletedRecords)
            Log.d(TAG, "Удалено записей из MySQL: $deletedCount")
            
            // Очищаем записи об удалениях после успешной синхронизации
            if (deletedCount > 0) {
                db.deletedRecordsDao().clearAllDeletedRecords()
                Log.d(TAG, "Записи об удалениях очищены после синхронизации")
            }
            
            connection.commit()
            
            Log.d(TAG, "Успешно отправлено записей: $totalRecords, удалено: $deletedCount")
            return "Успешно отправлено записей: $totalRecords, удалено: $deletedCount"
            
        } catch (e: Exception) {
            try {
                connection?.rollback()
                Log.d(TAG, "Транзакция откачена")
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "Ошибка при откате транзакции", rollbackEx)
            }
            Log.e(TAG, "Ошибка при отправке данных в MySQL", e)
            Log.e(TAG, "Тип ошибки: ${e.javaClass.simpleName}")
            Log.e(TAG, "Сообщение: ${e.message}")
            if (e.cause != null) {
                Log.e(TAG, "Причина ошибки: ${e.cause?.javaClass?.simpleName} - ${e.cause?.message}")
            }
            e.printStackTrace()
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
     * Получает данные из удалённой MySQL БД и сохраняет в локальную Room БД
     * ВАЖНО: Этот метод должен вызываться из suspend-функции или корутины
     */
    suspend fun pullFromRemote(db: AppDatabase): String {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Начало получения данных из MySQL")
            
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
                Log.e(TAG, "URL: $DB_URL")
                Log.e(TAG, "Пользователь: $DB_USER")
                throw RuntimeException("Не удалось подключиться к MySQL БД: ${e.message}", e)
            }
            
            var totalRecords = 0
            
            // Получаем группы клиентов (перед клиентами, так как клиенты ссылаются на группы)
            val clientGroups = pullClientGroups(connection)
            Log.d(TAG, "Получено групп клиентов из MySQL: ${clientGroups.size}")
            if (clientGroups.isNotEmpty()) {
                Log.d(TAG, "Список групп клиентов:")
                clientGroups.forEachIndexed { index, group ->
                    Log.d(TAG, "  ${index + 1}. ${group.title} (id: ${group.id})")
                }
            }
            clientGroups.forEach { group ->
                try {
                    db.clientDao().upsertGroup(group)
                    Log.d(TAG, "✓ Группа клиентов сохранена: ${group.title} (id: ${group.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении группы клиентов ${group.id}: ${e.message}", e)
                }
            }
            totalRecords += clientGroups.size
            
            // Получаем клиентов
            val clients = pullClients(connection)
            Log.d(TAG, "Получено клиентов из MySQL: ${clients.size}")
            if (clients.isNotEmpty()) {
                Log.d(TAG, "Список клиентов:")
                clients.forEachIndexed { index, client ->
                    Log.d(TAG, "  ${index + 1}. ${client.name} (id: ${client.id})")
                }
            }
            clients.forEach { client ->
                try {
                    db.clientDao().upsertClient(client)
                    Log.d(TAG, "✓ Клиент сохранен: ${client.name} (id: ${client.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении клиента ${client.id}: ${e.message}", e)
                }
            }
            totalRecords += clients.size
            
            // Получаем объекты
            val sites = pullSites(connection)
            Log.d(TAG, "Получено объектов из MySQL: ${sites.size}")
            if (sites.isNotEmpty()) {
                Log.d(TAG, "Список объектов:")
                sites.forEachIndexed { index, site ->
                    Log.d(TAG, "  ${index + 1}. ${site.name} (id: ${site.id}, clientId: ${site.clientId})")
                }
            }
            sites.forEach { site ->
                try {
                    db.hierarchyDao().insertSite(site)
                    Log.d(TAG, "✓ Объект сохранен: ${site.name} (id: ${site.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении объекта ${site.id}: ${e.message}", e)
                }
            }
            totalRecords += sites.size
            
            // Получаем установки
            val installations = pullInstallations(connection)
            Log.d(TAG, "Получено установок из MySQL: ${installations.size}")
            if (installations.isNotEmpty()) {
                Log.d(TAG, "Список установок:")
                installations.forEachIndexed { index, inst ->
                    Log.d(TAG, "  ${index + 1}. ${inst.name} (id: ${inst.id}, siteId: ${inst.siteId})")
                }
            }
            installations.forEach { inst ->
                try {
                    db.hierarchyDao().insertInstallation(inst)
                    Log.d(TAG, "✓ Установка сохранена: ${inst.name} (id: ${inst.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении установки ${inst.id}: ${e.message}", e)
                }
            }
            totalRecords += installations.size
            
            // Получаем компоненты
            val components = pullComponents(connection)
            Log.d(TAG, "Получено компонентов из MySQL: ${components.size}")
            if (components.isNotEmpty()) {
                Log.d(TAG, "Список компонентов:")
                components.forEachIndexed { index, comp ->
                    Log.d(TAG, "  ${index + 1}. ${comp.name} (id: ${comp.id}, installationId: ${comp.installationId})")
                }
            }
            components.forEach { comp ->
                try {
                    db.hierarchyDao().insertComponent(comp)
                    Log.d(TAG, "✓ Компонент сохранен: ${comp.name} (id: ${comp.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении компонента ${comp.id}: ${e.message}", e)
                }
            }
            totalRecords += components.size
            
            // Получаем сессии ТО
            val sessions = pullSessions(connection)
            Log.d(TAG, "Получено сессий ТО из MySQL: ${sessions.size}")
            if (sessions.isNotEmpty()) {
                Log.d(TAG, "Список сессий ТО:")
                sessions.forEachIndexed { index, session ->
                    val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(session.startedAtEpoch))
                    Log.d(TAG, "  ${index + 1}. Сессия от $dateStr (id: ${session.id}, siteId: ${session.siteId})")
                }
            }
            sessions.forEach { session ->
                try {
                    db.sessionsDao().insertSession(session)
                    Log.d(TAG, "✓ Сессия ТО сохранена: ${session.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении сессии ${session.id}: ${e.message}", e)
                }
            }
            totalRecords += sessions.size
            
            // Получаем значения ТО
            val values = pullMaintenanceValues(connection)
            Log.d(TAG, "Получено значений ТО из MySQL: ${values.size}")
            values.forEach { value ->
                try {
                    db.sessionsDao().insertValue(value)
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении значения ТО ${value.id}: ${e.message}", e)
                }
            }
            totalRecords += values.size
            
            // Получаем шаблоны
            val templates = pullTemplates(connection)
            Log.d(TAG, "Получено шаблонов из MySQL: ${templates.size}")
            if (templates.isNotEmpty()) {
                Log.d(TAG, "Список шаблонов:")
                templates.forEachIndexed { index, template ->
                    Log.d(TAG, "  ${index + 1}. ${template.title} (id: ${template.id}, type: ${template.componentType})")
                }
            }
            templates.forEach { template ->
                try {
                    db.templatesDao().insertTemplate(template)
                    Log.d(TAG, "✓ Шаблон сохранен: ${template.title} (id: ${template.id})")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении шаблона ${template.id}: ${e.message}", e)
                }
            }
            totalRecords += templates.size
            
            // Получаем поля шаблонов
            val fields = pullTemplateFields(connection)
            Log.d(TAG, "Получено полей шаблонов из MySQL: ${fields.size}")
            fields.forEach { field ->
                try {
                    db.templatesDao().insertField(field)
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Ошибка при сохранении поля шаблона ${field.id}: ${e.message}", e)
                }
            }
            totalRecords += fields.size
            
            val summary = buildString {
                appendLine("Загружено записей: $totalRecords")
                appendLine()
                
                if (clientGroups.isNotEmpty()) {
                    appendLine("Групп клиентов: ${clientGroups.size}")
                    clientGroups.forEachIndexed { index, group ->
                        appendLine("  ${index + 1}. ${group.title}")
                    }
                    appendLine()
                }
                
                if (clients.isNotEmpty()) {
                    appendLine("Клиентов: ${clients.size}")
                    clients.forEachIndexed { index, client ->
                        appendLine("  ${index + 1}. ${client.name}")
                    }
                    appendLine()
                }
                
                if (sites.isNotEmpty()) {
                    appendLine("Объектов: ${sites.size}")
                    sites.forEachIndexed { index, site ->
                        appendLine("  ${index + 1}. ${site.name}")
                    }
                    appendLine()
                }
                
                if (installations.isNotEmpty()) {
                    appendLine("Установок: ${installations.size}")
                    installations.forEachIndexed { index, inst ->
                        appendLine("  ${index + 1}. ${inst.name}")
                    }
                    appendLine()
                }
                
                if (components.isNotEmpty()) {
                    appendLine("Компонентов: ${components.size}")
                    components.forEachIndexed { index, comp ->
                        appendLine("  ${index + 1}. ${comp.name}")
                    }
                    appendLine()
                }
                
                if (sessions.isNotEmpty()) {
                    appendLine("Сессий ТО: ${sessions.size}")
                    sessions.forEachIndexed { index, session ->
                        val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(session.startedAtEpoch))
                        appendLine("  ${index + 1}. Сессия от $dateStr")
                    }
                    appendLine()
                }
                
                if (values.isNotEmpty()) {
                    appendLine("Значений ТО: ${values.size}")
                    appendLine()
                }
                
                if (templates.isNotEmpty()) {
                    appendLine("Шаблонов: ${templates.size}")
                    templates.forEachIndexed { index, template ->
                        appendLine("  ${index + 1}. ${template.title}")
                    }
                    appendLine()
                }
                
                if (fields.isNotEmpty()) {
                    appendLine("Полей шаблонов: ${fields.size}")
                }
            }
            
            Log.d(TAG, "Успешно получено и сохранено записей: $totalRecords")
            Log.d(TAG, "Итоговая сводка:\n$summary")
            return summary.trim()
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении данных из MySQL", e)
            Log.e(TAG, "Тип ошибки: ${e.javaClass.simpleName}")
            Log.e(TAG, "Сообщение: ${e.message}")
            if (e.cause != null) {
                Log.e(TAG, "Причина ошибки: ${e.cause?.javaClass?.simpleName} - ${e.cause?.message}")
            }
            e.printStackTrace()
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
     * Мигрирует удалённую MySQL БД, создавая таблицы, если они не существуют.
     */
    suspend fun migrateRemoteDatabase(): String {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Начало миграции удалённой БД")
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
            connection.autoCommit = false
            
            // Создаём таблицы, если их нет (включая новые поля в users)
            createTablesIfNotExist(connection)
            
            // Миграция таблицы users: добавляем новые поля, если их нет
            try {
                // Проверяем наличие колонки role
                val checkRoleSql = "SHOW COLUMNS FROM users LIKE 'role'"
                val checkRoleRs = connection.createStatement().executeQuery(checkRoleSql)
                if (!checkRoleRs.next()) {
                    // Колонки нет, добавляем
                    val alterRoleSql = "ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'USER'"
                    connection.createStatement().executeUpdate(alterRoleSql)
                    Log.d(TAG, "Добавлена колонка 'role' в таблицу users")
                }
                checkRoleRs.close()
                
                // Проверяем наличие колонки permissions
                val checkPermissionsSql = "SHOW COLUMNS FROM users LIKE 'permissions'"
                val checkPermissionsRs = connection.createStatement().executeQuery(checkPermissionsSql)
                if (!checkPermissionsRs.next()) {
                    // Колонки нет, добавляем
                    val alterPermissionsSql = "ALTER TABLE users ADD COLUMN permissions TEXT"
                    connection.createStatement().executeUpdate(alterPermissionsSql)
                    Log.d(TAG, "Добавлена колонка 'permissions' в таблицу users")
                }
                checkPermissionsRs.close()
                
                // Проверяем наличие колонки lastLoginAtEpoch
                val checkLastLoginSql = "SHOW COLUMNS FROM users LIKE 'lastLoginAtEpoch'"
                val checkLastLoginRs = connection.createStatement().executeQuery(checkLastLoginSql)
                if (!checkLastLoginRs.next()) {
                    // Колонки нет, добавляем
                    val alterLastLoginSql = "ALTER TABLE users ADD COLUMN lastLoginAtEpoch BIGINT"
                    connection.createStatement().executeUpdate(alterLastLoginSql)
                    Log.d(TAG, "Добавлена колонка 'lastLoginAtEpoch' в таблицу users")
                }
                checkLastLoginRs.close()
                
                // Обновляем существующих пользователей: устанавливаем значения по умолчанию для новых полей
                val defaultPermissions = ru.wassertech.auth.UserPermissions()
                val permissionsJson = ru.wassertech.auth.UserPermissions.toJson(defaultPermissions)
                val updateUsersSql = """
                    UPDATE users 
                    SET role = COALESCE(role, 'USER'),
                        permissions = COALESCE(NULLIF(permissions, ''), ?)
                    WHERE role IS NULL OR permissions IS NULL OR permissions = ''
                """.trimIndent()
                val updatePs = connection.prepareStatement(updateUsersSql)
                updatePs.setString(1, permissionsJson)
                val updatedRows = updatePs.executeUpdate()
                updatePs.close()
                if (updatedRows > 0) {
                    Log.d(TAG, "Обновлено пользователей: $updatedRows")
                }
                
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка при миграции таблицы users", e)
                // Продолжаем выполнение, так как поля могут уже существовать или таблица может не существовать
            }
            
            connection.commit()
            Log.d(TAG, "Удалённая БД успешно мигрирована.")
            return "Удалённая БД успешно мигрирована."
        } catch (e: Exception) {
            try {
                connection?.rollback()
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "Ошибка при откате транзакции", rollbackEx)
            }
            Log.e(TAG, "Ошибка при миграции удалённой БД", e)
            return "Ошибка при миграции: ${e.message}"
        } finally {
            try {
                connection?.close()
                Log.d(TAG, "Соединение с MySQL закрыто после миграции")
            } catch (closeEx: Exception) {
                Log.e(TAG, "Ошибка при закрытии соединения после миграции", closeEx)
            }
        }
    }
    
    private fun createTablesIfNotExist(conn: Connection) {
        val statements = listOf(
            """
            CREATE TABLE IF NOT EXISTS client_groups (
                id VARCHAR(255) PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                notes TEXT,
                sortOrder INT DEFAULT 0,
                isArchived BOOLEAN DEFAULT FALSE,
                archivedAtEpoch BIGINT,
                createdAtEpoch BIGINT,
                updatedAtEpoch BIGINT,
                INDEX idx_title (title),
                INDEX idx_isArchived (isArchived),
                INDEX idx_sortOrder (sortOrder)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS clients (
                id VARCHAR(255) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                legalName VARCHAR(255),
                contactPerson VARCHAR(255),
                phone VARCHAR(50),
                phone2 VARCHAR(50),
                email VARCHAR(255),
                addressFull TEXT,
                city VARCHAR(100),
                region VARCHAR(100),
                country VARCHAR(100),
                postalCode VARCHAR(20),
                latitude DOUBLE,
                longitude DOUBLE,
                taxId VARCHAR(50),
                vatNumber VARCHAR(50),
                externalId VARCHAR(255),
                tagsJson TEXT,
                notes TEXT,
                isCorporate BOOLEAN DEFAULT FALSE,
                isArchived BOOLEAN DEFAULT FALSE,
                archivedAtEpoch BIGINT,
                createdAtEpoch BIGINT,
                updatedAtEpoch BIGINT,
                sortOrder INT DEFAULT 0,
                clientGroupId VARCHAR(255),
                INDEX idx_name (name),
                INDEX idx_phone (phone),
                INDEX idx_email (email),
                INDEX idx_isArchived (isArchived)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS sites (
                id VARCHAR(255) PRIMARY KEY,
                clientId VARCHAR(255) NOT NULL,
                name VARCHAR(255) NOT NULL,
                address TEXT,
                orderIndex INT DEFAULT 0,
                INDEX idx_clientId (clientId)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS installations (
                id VARCHAR(255) PRIMARY KEY,
                siteId VARCHAR(255) NOT NULL,
                name VARCHAR(255) NOT NULL,
                orderIndex INT DEFAULT 0,
                INDEX idx_siteId (siteId)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS components (
                id VARCHAR(255) PRIMARY KEY,
                installationId VARCHAR(255) NOT NULL,
                name VARCHAR(255) NOT NULL,
                type VARCHAR(50) NOT NULL,
                orderIndex INT DEFAULT 0,
                templateId VARCHAR(255),
                INDEX idx_installationId (installationId)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS maintenance_sessions (
                id VARCHAR(255) PRIMARY KEY,
                siteId VARCHAR(255) NOT NULL,
                installationId VARCHAR(255),
                startedAtEpoch BIGINT NOT NULL,
                finishedAtEpoch BIGINT,
                technician VARCHAR(255),
                notes TEXT,
                synced BOOLEAN DEFAULT FALSE,
                INDEX idx_siteId (siteId),
                INDEX idx_installationId (installationId),
                INDEX idx_startedAtEpoch (startedAtEpoch)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS maintenance_values (
                id VARCHAR(255) PRIMARY KEY,
                sessionId VARCHAR(255) NOT NULL,
                siteId VARCHAR(255) NOT NULL,
                installationId VARCHAR(255),
                componentId VARCHAR(255) NOT NULL,
                fieldKey VARCHAR(255) NOT NULL,
                valueText TEXT,
                valueBool BOOLEAN,
                INDEX idx_sessionId (sessionId),
                INDEX idx_componentId (componentId),
                INDEX idx_fieldKey (fieldKey)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS checklist_templates (
                id VARCHAR(255) PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                componentType VARCHAR(50) NOT NULL,
                componentTemplateId VARCHAR(255),
                sortOrder INT DEFAULT 0,
                isArchived BOOLEAN DEFAULT FALSE,
                updatedAtEpoch BIGINT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS checklist_fields (
                id VARCHAR(255) PRIMARY KEY,
                templateId VARCHAR(255) NOT NULL,
                `key` VARCHAR(255) NOT NULL,
                label VARCHAR(255),
                `type` VARCHAR(50) NOT NULL,
                unit VARCHAR(50),
                `minValue` DOUBLE,
                `maxValue` DOUBLE,
                INDEX idx_templateId (templateId)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(255) PRIMARY KEY,
                login VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                name VARCHAR(255),
                email VARCHAR(255),
                phone VARCHAR(50),
                role VARCHAR(50) DEFAULT 'USER',
                permissions TEXT,
                lastLoginAtEpoch BIGINT,
                createdAtEpoch BIGINT NOT NULL,
                updatedAtEpoch BIGINT NOT NULL,
                INDEX idx_login (login)
            )
            """
        )
        
        statements.forEach { sql ->
            try {
                Log.d(TAG, "Выполнение SQL: ${sql.take(200)}...")
                conn.createStatement().execute(sql)
                Log.d(TAG, "Таблица успешно создана")
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка при создании таблицы", e)
                Log.e(TAG, "SQL запрос: $sql")
                // Не пробрасываем ошибку дальше, так как таблица может уже существовать
                // Но логируем для отладки
            }
        }
    }
    
    private fun syncClientGroups(conn: Connection, groups: List<ClientGroupEntity>): Int {
        if (groups.isEmpty()) return 0
        
        val sql = """
            INSERT INTO client_groups (id, title, notes, sortOrder, isArchived, archivedAtEpoch, 
                createdAtEpoch, updatedAtEpoch)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                notes = VALUES(notes),
                sortOrder = VALUES(sortOrder),
                isArchived = VALUES(isArchived),
                archivedAtEpoch = VALUES(archivedAtEpoch),
                updatedAtEpoch = VALUES(updatedAtEpoch)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        groups.forEach { group ->
            ps.setString(1, group.id?.take(255))
            ps.setString(2, group.title?.take(255))
            ps.setString(3, group.notes) // TEXT - без ограничения
            ps.setInt(4, group.sortOrder)
            ps.setBoolean(5, group.isArchived)
            ps.setObject(6, group.archivedAtEpoch)
            ps.setLong(7, group.createdAtEpoch)
            ps.setLong(8, group.updatedAtEpoch)
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        Log.d(TAG, "Отправлено групп клиентов: ${groups.size}")
        return groups.size
    }
    
    private fun syncClients(conn: Connection, clients: List<ClientEntity>): Int {
        if (clients.isEmpty()) return 0
        
        val sql = """
            INSERT INTO clients (id, name, legalName, contactPerson, phone, phone2, email, 
                addressFull, city, region, country, postalCode, latitude, longitude, taxId, 
                vatNumber, externalId, tagsJson, notes, isCorporate, isArchived, archivedAtEpoch, 
                createdAtEpoch, updatedAtEpoch, sortOrder, clientGroupId)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                legalName = VALUES(legalName),
                contactPerson = VALUES(contactPerson),
                phone = VALUES(phone),
                phone2 = VALUES(phone2),
                email = VALUES(email),
                addressFull = VALUES(addressFull),
                city = VALUES(city),
                region = VALUES(region),
                country = VALUES(country),
                postalCode = VALUES(postalCode),
                latitude = VALUES(latitude),
                longitude = VALUES(longitude),
                taxId = VALUES(taxId),
                vatNumber = VALUES(vatNumber),
                externalId = VALUES(externalId),
                tagsJson = VALUES(tagsJson),
                notes = VALUES(notes),
                isCorporate = VALUES(isCorporate),
                isArchived = VALUES(isArchived),
                archivedAtEpoch = VALUES(archivedAtEpoch),
                updatedAtEpoch = VALUES(updatedAtEpoch),
                sortOrder = VALUES(sortOrder),
                clientGroupId = VALUES(clientGroupId)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        clients.forEach { client ->
            // Обрезаем строки до максимальной длины колонок
            ps.setString(1, client.id?.take(255))
            ps.setString(2, client.name?.take(255))
            ps.setString(3, client.legalName?.take(255))
            ps.setString(4, client.contactPerson?.take(255))
            ps.setString(5, client.phone?.take(50))
            ps.setString(6, client.phone2?.take(50))
            ps.setString(7, client.email?.take(255))
            ps.setString(8, client.addressFull) // TEXT - без ограничения
            ps.setString(9, client.city?.take(100))
            ps.setString(10, client.region?.take(100))
            ps.setString(11, client.country?.take(100))
            ps.setString(12, client.postalCode?.take(20))
            ps.setObject(13, client.latitude)
            ps.setObject(14, client.longitude)
            ps.setString(15, client.taxId?.take(50))
            ps.setString(16, client.vatNumber?.take(50))
            ps.setString(17, client.externalId?.take(255))
            ps.setString(18, client.tagsJson) // TEXT - без ограничения
            ps.setString(19, client.notes) // TEXT - без ограничения
            ps.setBoolean(20, client.isCorporate)
            ps.setBoolean(21, client.isArchived)
            ps.setObject(22, client.archivedAtEpoch)
            ps.setLong(23, client.createdAtEpoch)
            ps.setLong(24, client.updatedAtEpoch)
            ps.setInt(25, client.sortOrder)
            ps.setString(26, client.clientGroupId?.take(255))
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        return clients.size
    }
    
    private fun syncSites(conn: Connection, sites: List<SiteEntity>): Int {
        if (sites.isEmpty()) return 0
        
        val sql = """
            INSERT INTO sites (id, clientId, name, address, orderIndex)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                clientId = VALUES(clientId),
                name = VALUES(name),
                address = VALUES(address),
                orderIndex = VALUES(orderIndex)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        sites.forEach { site ->
            // Обрезаем строки до максимальной длины колонок
            val clientId = site.clientId?.take(255)
            if (clientId != null && clientId.length > 255) {
                Log.w(TAG, "clientId слишком длинный: ${clientId.length} символов")
            }
            ps.setString(1, site.id?.take(255))
            ps.setString(2, clientId) // Важно: clientId должен быть не более 255 символов
            ps.setString(3, site.name?.take(255))
            ps.setString(4, site.address) // TEXT - без ограничения
            ps.setInt(5, site.orderIndex)
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        return sites.size
    }
    
    private fun syncInstallations(conn: Connection, installations: List<InstallationEntity>): Int {
        if (installations.isEmpty()) return 0
        
        val sql = """
            INSERT INTO installations (id, siteId, name, orderIndex)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                siteId = VALUES(siteId),
                name = VALUES(name),
                orderIndex = VALUES(orderIndex)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        installations.forEach { inst ->
            ps.setString(1, inst.id?.take(255))
            ps.setString(2, inst.siteId?.take(255))
            ps.setString(3, inst.name?.take(255))
            ps.setInt(4, inst.orderIndex)
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        return installations.size
    }
    
    private fun syncComponents(conn: Connection, components: List<ComponentEntity>): Int {
        if (components.isEmpty()) return 0
        
        val sql = """
            INSERT INTO components (id, installationId, name, type, orderIndex, templateId)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                installationId = VALUES(installationId),
                name = VALUES(name),
                type = VALUES(type),
                orderIndex = VALUES(orderIndex),
                templateId = VALUES(templateId)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        components.forEach { comp ->
            ps.setString(1, comp.id?.take(255))
            ps.setString(2, comp.installationId?.take(255))
            ps.setString(3, comp.name?.take(255))
            ps.setString(4, comp.type.name?.take(50))
            ps.setInt(5, comp.orderIndex)
            ps.setString(6, comp.templateId?.take(255))
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        return components.size
    }
    
    private fun syncSessions(conn: Connection, sessions: List<MaintenanceSessionEntity>): Int {
        if (sessions.isEmpty()) return 0
        
        val sql = """
            INSERT INTO maintenance_sessions (id, siteId, installationId, startedAtEpoch, 
                finishedAtEpoch, technician, notes, synced)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                siteId = VALUES(siteId),
                installationId = VALUES(installationId),
                startedAtEpoch = VALUES(startedAtEpoch),
                finishedAtEpoch = VALUES(finishedAtEpoch),
                technician = VALUES(technician),
                notes = VALUES(notes),
                synced = VALUES(synced)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        sessions.forEach { session ->
            ps.setString(1, session.id?.take(255))
            ps.setString(2, session.siteId?.take(255))
            ps.setString(3, session.installationId?.take(255))
            ps.setLong(4, session.startedAtEpoch)
            ps.setObject(5, session.finishedAtEpoch)
            ps.setString(6, session.technician?.take(255))
            ps.setString(7, session.notes)
            ps.setBoolean(8, session.synced)
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        return sessions.size
    }
    
    private fun syncMaintenanceValues(conn: Connection, values: List<MaintenanceValueEntity>): Int {
        if (values.isEmpty()) return 0
        
        val sql = """
            INSERT INTO maintenance_values (id, sessionId, siteId, installationId, componentId, 
                fieldKey, valueText, valueBool)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                sessionId = VALUES(sessionId),
                siteId = VALUES(siteId),
                installationId = VALUES(installationId),
                componentId = VALUES(componentId),
                fieldKey = VALUES(fieldKey),
                valueText = VALUES(valueText),
                valueBool = VALUES(valueBool)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        values.forEach { value ->
            ps.setString(1, value.id?.take(255))
            ps.setString(2, value.sessionId?.take(255))
            ps.setString(3, value.siteId?.take(255))
            ps.setString(4, value.installationId?.take(255))
            ps.setString(5, value.componentId?.take(255))
            ps.setString(6, value.fieldKey?.take(255))
            ps.setString(7, value.valueText)
            ps.setObject(8, value.valueBool)
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        return values.size
    }
    
    private fun syncTemplates(conn: Connection, templates: List<ChecklistTemplateEntity>): Int {
        if (templates.isEmpty()) return 0
        
        val sql = """
            INSERT INTO checklist_templates (id, title, componentType, componentTemplateId, 
                sortOrder, isArchived, updatedAtEpoch)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                componentType = VALUES(componentType),
                componentTemplateId = VALUES(componentTemplateId),
                sortOrder = VALUES(sortOrder),
                isArchived = VALUES(isArchived),
                updatedAtEpoch = VALUES(updatedAtEpoch)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        templates.forEach { template ->
            ps.setString(1, template.id?.take(255))
            ps.setString(2, template.title?.take(255))
            ps.setString(3, template.componentType.name?.take(50))
            ps.setString(4, template.componentTemplateId?.take(255))
            ps.setInt(5, template.sortOrder ?: 0)
            ps.setBoolean(6, template.isArchived)
            ps.setObject(7, template.updatedAtEpoch)
            ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
        
        return templates.size
    }
    
    private fun syncTemplateFields(conn: Connection, fields: List<ChecklistFieldEntity>): Int {
        if (fields.isEmpty()) return 0
        
        val sql = """
            INSERT INTO checklist_fields (id, templateId, `key`, label, `type`, unit, `minValue`, `maxValue`)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                templateId = VALUES(templateId),
                `key` = VALUES(`key`),
                label = VALUES(label),
                `type` = VALUES(`type`),
                unit = VALUES(unit),
                `minValue` = VALUES(`minValue`),
                `maxValue` = VALUES(`maxValue`)
        """.trimIndent()
        
        val ps = conn.prepareStatement(sql)
        fields.forEach { field ->
            ps.setString(1, field.id?.take(255))
            ps.setString(2, field.templateId?.take(255))
            ps.setString(3, field.key?.take(255))
            ps.setString(4, field.label?.take(255))
            ps.setString(5, field.type.name?.take(50))
            ps.setString(6, field.unit?.take(50))
            ps.setObject(7, field.min)
            ps.setObject(8, field.max)
            ps.addBatch()
        }
        try {
            ps.executeBatch()
        } catch (e: SQLException) {
            Log.e(TAG, "Ошибка при выполнении batch для checklist_fields", e)
            Log.e(TAG, "SQL запрос: $sql")
            throw e
        } finally {
            ps.close()
        }
        
        return fields.size
    }
    
    /**
     * Синхронизирует удаления: удаляет записи из MySQL БД
     */
    private fun syncDeletions(conn: Connection, deletedRecords: List<ru.wassertech.data.entities.DeletedRecordEntity>): Int {
        if (deletedRecords.isEmpty()) return 0
        
        var deletedCount = 0
        
        // Группируем удаления по таблицам для эффективности
        val byTable = deletedRecords.groupBy { it.tableName }
        
        byTable.forEach { (tableName, records) ->
            val recordIds = records.map { it.recordId }
            
            try {
                val deleteSql = when (tableName) {
                    "client_groups" -> "DELETE FROM client_groups WHERE id = ?"
                    "clients" -> "DELETE FROM clients WHERE id = ?"
                    "sites" -> "DELETE FROM sites WHERE id = ?"
                    "installations" -> "DELETE FROM installations WHERE id = ?"
                    "components" -> "DELETE FROM components WHERE id = ?"
                    "checklist_templates" -> "DELETE FROM checklist_templates WHERE id = ?"
                    "checklist_fields" -> "DELETE FROM checklist_fields WHERE id = ?"
                    "maintenance_sessions" -> "DELETE FROM maintenance_sessions WHERE id = ?"
                    "maintenance_values" -> "DELETE FROM maintenance_values WHERE id = ?"
                    else -> {
                        Log.w(TAG, "Неизвестная таблица для удаления: $tableName")
                        null
                    }
                }
                
                if (deleteSql != null) {
                    val ps = conn.prepareStatement(deleteSql)
                    recordIds.forEach { id ->
                        ps.setString(1, id)
                        ps.addBatch()
                    }
                    
                    val batchResults = ps.executeBatch()
                    val successful = batchResults.count { it >= 0 }
                    deletedCount += successful
                    
                    Log.d(TAG, "Удалено $successful записей из таблицы $tableName")
                    ps.close()
                }
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка при удалении записей из таблицы $tableName", e)
                // Продолжаем работу с другими таблицами
            }
        }
        
        return deletedCount
    }
    
    // Методы для получения данных из MySQL
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
                    isArchived = rs.getBoolean("isArchived"),
                    archivedAtEpoch = rs.getObject("archivedAtEpoch") as? Long,
                    createdAtEpoch = rs.getLong("createdAtEpoch"),
                    updatedAtEpoch = rs.getLong("updatedAtEpoch")
                )
            )
        }
        rs.close()
        Log.d(TAG, "Прочитано групп клиентов из MySQL: ${groups.size}")
        return groups
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
                    isArchived = rs.getBoolean("isArchived"),
                    archivedAtEpoch = rs.getObject("archivedAtEpoch") as? Long,
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
    
    private fun pullSites(conn: Connection): List<SiteEntity> {
        val sql = "SELECT * FROM sites"
        val rs = conn.createStatement().executeQuery(sql)
        val sites = mutableListOf<SiteEntity>()
        
        while (rs.next()) {
            sites.add(
                SiteEntity(
                    id = rs.getString("id"),
                    clientId = rs.getString("clientId"),
                    name = rs.getString("name"),
                    address = rs.getString("address"),
                    orderIndex = rs.getInt("orderIndex")
                )
            )
        }
        rs.close()
        return sites
    }
    
    private fun pullInstallations(conn: Connection): List<InstallationEntity> {
        val sql = "SELECT * FROM installations"
        val rs = conn.createStatement().executeQuery(sql)
        val installations = mutableListOf<InstallationEntity>()
        
        while (rs.next()) {
            installations.add(
                InstallationEntity(
                    id = rs.getString("id"),
                    siteId = rs.getString("siteId"),
                    name = rs.getString("name"),
                    orderIndex = rs.getInt("orderIndex")
                )
            )
        }
        rs.close()
        return installations
    }
    
    private fun pullComponents(conn: Connection): List<ComponentEntity> {
        val sql = "SELECT * FROM components"
        val rs = conn.createStatement().executeQuery(sql)
        val components = mutableListOf<ComponentEntity>()
        
        while (rs.next()) {
            val typeStr = rs.getString("type")
            val componentType = try {
                ru.wassertech.data.types.ComponentType.valueOf(typeStr)
            } catch (e: Exception) {
                // Если тип не найден, используем FILTER как дефолтный
                ru.wassertech.data.types.ComponentType.COMMON
            }
            
            components.add(
                ComponentEntity(
                    id = rs.getString("id"),
                    installationId = rs.getString("installationId"),
                    name = rs.getString("name"),
                    type = componentType,
                    orderIndex = rs.getInt("orderIndex"),
                    templateId = rs.getString("templateId")
                )
            )
        }
        rs.close()
        return components
    }
    
    private fun pullSessions(conn: Connection): List<MaintenanceSessionEntity> {
        val sql = "SELECT * FROM maintenance_sessions"
        val rs = conn.createStatement().executeQuery(sql)
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
        return sessions
    }
    
    private fun pullMaintenanceValues(conn: Connection): List<MaintenanceValueEntity> {
        val sql = "SELECT * FROM maintenance_values"
        val rs = conn.createStatement().executeQuery(sql)
        val values = mutableListOf<MaintenanceValueEntity>()
        
        while (rs.next()) {
            values.add(
                MaintenanceValueEntity(
                    id = rs.getString("id"),
                    sessionId = rs.getString("sessionId"),
                    siteId = rs.getString("siteId"),
                    installationId = rs.getString("installationId"),
                    componentId = rs.getString("componentId"),
                    fieldKey = rs.getString("fieldKey"),
                    valueText = rs.getString("valueText"),
                    valueBool = rs.getObject("valueBool") as? Boolean
                )
            )
        }
        rs.close()
        return values
    }
    
    private fun pullTemplates(conn: Connection): List<ChecklistTemplateEntity> {
        val sql = "SELECT * FROM checklist_templates"
        val rs = conn.createStatement().executeQuery(sql)
        val templates = mutableListOf<ChecklistTemplateEntity>()
        
        while (rs.next()) {
            val typeStr = rs.getString("componentType")
            val componentType = try {
                ru.wassertech.data.types.ComponentType.valueOf(typeStr)
            } catch (e: Exception) {
                // Если тип не найден, используем FILTER как дефолтный
                ru.wassertech.data.types.ComponentType.COMMON
            }
            
            templates.add(
                ChecklistTemplateEntity(
                    id = rs.getString("id"),
                    title = rs.getString("title"),
                    componentType = componentType,
                    componentTemplateId = rs.getString("componentTemplateId"),
                    sortOrder = rs.getInt("sortOrder"),
                    isArchived = rs.getBoolean("isArchived"),
                    updatedAtEpoch = rs.getObject("updatedAtEpoch") as? Long
                )
            )
        }
        rs.close()
        return templates
    }
    
    private fun pullTemplateFields(conn: Connection): List<ChecklistFieldEntity> {
        val sql = "SELECT * FROM checklist_fields"
        val rs = conn.createStatement().executeQuery(sql)
        val fields = mutableListOf<ChecklistFieldEntity>()
        
        while (rs.next()) {
            val typeStr = rs.getString("type")
            val fieldType = try {
                ru.wassertech.data.types.FieldType.valueOf(typeStr)
            } catch (e: Exception) {
                ru.wassertech.data.types.FieldType.TEXT
            }
            
            fields.add(
                ChecklistFieldEntity(
                    id = rs.getString("id"),
                    templateId = rs.getString("templateId"),
                    key = rs.getString("key"),
                    label = rs.getString("label"),
                    type = fieldType,
                    unit = rs.getString("unit"),
                    min = rs.getObject("minValue") as? Double,
                    max = rs.getObject("maxValue") as? Double
                )
            )
        }
        rs.close()
        return fields
    }
    
    /**
     * Регистрирует нового пользователя в удаленной БД
     * @return Pair<Boolean, String> - (успех, сообщение)
     * @deprecated Используйте REST API через AuthRepository. Этот метод будет удален.
     */
    @Deprecated("Используйте REST API через AuthRepository", ReplaceWith("ru.wassertech.auth.AuthRepository"))
    suspend fun registerUser(login: String, password: String, name: String? = null, email: String? = null, phone: String? = null): Pair<Boolean, String> {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Начало регистрации пользователя: $login")
            
            // Загружаем драйвер MySQL
            try {
                Class.forName("com.mysql.jdbc.Driver")
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "Ошибка загрузки MySQL драйвера", e)
                return false to "Ошибка загрузки драйвера БД: ${e.message}"
            }
            
            // Подключаемся к БД
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
                connection.autoCommit = false
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка подключения к MySQL БД", e)
                return false to "Ошибка подключения к БД: ${e.message}"
            }
            
            // Создаём таблицу пользователей, если её нет
            createTablesIfNotExist(connection)
            
            // Проверяем, существует ли пользователь с таким логином (регистронезависимый поиск)
            val checkSql = "SELECT COUNT(*) FROM users WHERE LOWER(login) = LOWER(?)"
            val checkPs = connection.prepareStatement(checkSql)
            checkPs.setString(1, login)
            val rs = checkPs.executeQuery()
            rs.next()
            val count = rs.getInt(1)
            rs.close()
            checkPs.close()
            
            if (count > 0) {
                connection.rollback()
                return false to "Пользователь с логином \"$login\" уже существует"
            }
            
            // Создаём нового пользователя (по умолчанию роль USER и права по умолчанию)
            val userId = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val defaultPermissions = ru.wassertech.auth.UserPermissions()
            val permissionsJson = ru.wassertech.auth.UserPermissions.toJson(defaultPermissions)
            
            val insertSql = """
                INSERT INTO users (id, login, password, name, email, phone, role, permissions, createdAtEpoch, updatedAtEpoch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            
            val insertPs = connection.prepareStatement(insertSql)
            insertPs.setString(1, userId)
            insertPs.setString(2, login)
            insertPs.setString(3, password)
            insertPs.setString(4, name)
            insertPs.setString(5, email)
            insertPs.setString(6, phone)
            insertPs.setString(7, ru.wassertech.auth.UserRole.USER.name)
            insertPs.setString(8, permissionsJson)
            insertPs.setLong(9, now)
            insertPs.setLong(10, now)
            
            insertPs.executeUpdate()
            insertPs.close()
            
            connection.commit()
            Log.d(TAG, "Пользователь успешно зарегистрирован: $login")
            return true to "Пользователь успешно зарегистрирован"
            
        } catch (e: Exception) {
            try {
                connection?.rollback()
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "Ошибка при откате транзакции", rollbackEx)
            }
            Log.e(TAG, "Ошибка при регистрации пользователя", e)
            return false to "Ошибка при регистрации: ${e.message}"
        } finally {
            try {
                connection?.close()
            } catch (closeEx: Exception) {
                Log.e(TAG, "Ошибка при закрытии соединения", closeEx)
            }
        }
    }
    
    /**
     * Проверяет логин и пароль пользователя и возвращает полную информацию о пользователе
     * @return Triple<Boolean, UserEntity?, String?> - (успех, пользователь или null, сообщение об ошибке)
     * @deprecated Используйте REST API через AuthRepository.login() и AuthRepository.loadCurrentUser()
     */
    @Deprecated("Используйте REST API через AuthRepository", ReplaceWith("ru.wassertech.auth.AuthRepository.login() и loadCurrentUser()"))
    suspend fun loginUser(login: String, password: String): Triple<Boolean, ru.wassertech.data.entities.UserEntity?, String?> {
        var connection: Connection? = null
        try {
            Log.d(TAG, "Попытка входа пользователя: $login")
            
            // Загружаем драйвер MySQL
            try {
                Class.forName("com.mysql.jdbc.Driver")
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "Ошибка загрузки MySQL драйвера", e)
                return Triple(false, null, "Ошибка загрузки драйвера БД")
            }
            
            // Подключаемся к БД
            try {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
                connection.autoCommit = false
            } catch (e: SQLException) {
                Log.e(TAG, "Ошибка подключения к MySQL БД", e)
                return Triple(false, null, "Ошибка подключения к БД")
            }
            
            // Создаём таблицу пользователей, если её нет
            createTablesIfNotExist(connection)
            
            // Получаем полную информацию о пользователе (регистронезависимый поиск)
            val sql = "SELECT id, login, password, name, email, phone, role, permissions, lastLoginAtEpoch, createdAtEpoch, updatedAtEpoch FROM users WHERE LOWER(login) = LOWER(?)"
            val ps = connection.prepareStatement(sql)
            ps.setString(1, login)
            val rs = ps.executeQuery()
            
            if (!rs.next()) {
                rs.close()
                ps.close()
                Log.d(TAG, "Пользователь не найден: $login")
                return Triple(false, null, "Пользователь не найден")
            }
            
            val userId = rs.getString("id")
            val storedPassword = rs.getString("password")
            
            if (storedPassword != password) {
                rs.close()
                ps.close()
                Log.d(TAG, "Неверный пароль для пользователя: $login")
                return Triple(false, null, "Неверный пароль")
            }
            
            // Читаем данные пользователя
            val user = ru.wassertech.data.entities.UserEntity(
                id = userId,
                login = rs.getString("login"),
                password = storedPassword, // Возвращаем пароль (в реальном приложении лучше не возвращать)
                name = rs.getString("name"),
                email = rs.getString("email"),
                phone = rs.getString("phone"),
                role = rs.getString("role") ?: ru.wassertech.auth.UserRole.USER.name,
                permissions = rs.getString("permissions"),
                lastLoginAtEpoch = rs.getObject("lastLoginAtEpoch") as? Long,
                createdAtEpoch = rs.getLong("createdAtEpoch"),
                updatedAtEpoch = rs.getLong("updatedAtEpoch")
            )
            
            rs.close()
            ps.close()
            
            // Обновляем время последнего входа
            val now = System.currentTimeMillis()
            val updateSql = "UPDATE users SET lastLoginAtEpoch = ?, updatedAtEpoch = ? WHERE id = ?"
            val updatePs = connection.prepareStatement(updateSql)
            updatePs.setLong(1, now)
            updatePs.setLong(2, now)
            updatePs.setString(3, userId)
            updatePs.executeUpdate()
            updatePs.close()
            
            connection.commit()
            
            Log.d(TAG, "Пользователь успешно авторизован: $login")
            return Triple(true, user.copy(lastLoginAtEpoch = now), null)
            
        } catch (e: Exception) {
            try {
                connection?.rollback()
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "Ошибка при откате транзакции", rollbackEx)
            }
            Log.e(TAG, "Ошибка при входе пользователя", e)
            return Triple(false, null, "Ошибка при входе: ${e.message}")
        } finally {
            try {
                connection?.close()
            } catch (closeEx: Exception) {
                Log.e(TAG, "Ошибка при закрытии соединения", closeEx)
            }
        }
    }
}

