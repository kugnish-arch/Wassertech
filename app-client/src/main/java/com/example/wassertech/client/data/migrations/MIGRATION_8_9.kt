package ru.wassertech.client.data.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.database.sqlite.SQLiteException

/**
 * Миграция с версии 8 на версию 9.
 * 
 * Добавляет поля синхронизации во все синхронизируемые таблицы:
 * - createdAtEpoch, updatedAtEpoch (временные метки)
 * - isArchived, archivedAtEpoch (архивирование)
 * - deletedAtEpoch (логическое удаление)
 * - dirtyFlag, syncStatus (локальные поля для оффлайн-очереди)
 * 
 * Все новые поля имеют разумные DEFAULT-значения, чтобы не сломать существующие данные.
 * 
 * ВАЖНО: Миграция устойчива к отсутствию таблиц component_templates и checklist_templates
 * в старых версиях БД (они могли быть добавлены позже через Room-схему).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    
    /**
     * Проверяет существование таблицы в базе данных.
     * @return true если таблица существует, false иначе
     */
    private fun tableExists(database: SupportSQLiteDatabase, tableName: String): Boolean {
        val cursor = database.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }
    
    /**
     * Безопасно выполняет ALTER TABLE, пропуская операцию если таблица не существует.
     * Логирует предупреждение при пропуске операции.
     */
    private fun safeAlterTable(
        database: SupportSQLiteDatabase,
        tableName: String,
        alterStatement: String,
        columnDescription: String
    ) {
        if (!tableExists(database, tableName)) {
            Log.w(
                "WassertechRoomMigration",
                "Table $tableName not found during MIGRATION_8_9, skipping ALTER COLUMN $columnDescription"
            )
            return
        }
        
        try {
            database.execSQL(alterStatement)
        } catch (e: SQLiteException) {
            val errorMsg = e.message?.lowercase() ?: ""
            // Если колонка уже существует - это нормально
            if (errorMsg.contains("duplicate column name") || errorMsg.contains("duplicate column")) {
                Log.w(
                    "WassertechRoomMigration",
                    "Column $columnDescription already exists in $tableName, skipping"
                )
            } 
            // Если таблицы нет (хотя мы проверили, но на всякий случай)
            else if (errorMsg.contains("no such table")) {
                Log.w(
                    "WassertechRoomMigration",
                    "Table $tableName not found during ALTER COLUMN $columnDescription, skipping",
                    e
                )
            } 
            // Другие ошибки - логируем и пробрасываем
            else {
                Log.e(
                    "WassertechRoomMigration",
                    "Error altering table $tableName: $columnDescription",
                    e
                )
                throw e
            }
        }
    }
    
    /**
     * Безопасно создаёт индекс, пропуская операцию если таблица не существует.
     */
    private fun safeCreateIndex(
        database: SupportSQLiteDatabase,
        tableName: String,
        indexStatement: String
    ) {
        if (!tableExists(database, tableName)) {
            Log.w(
                "WassertechRoomMigration",
                "Table $tableName not found during MIGRATION_8_9, skipping CREATE INDEX"
            )
            return
        }
        
        try {
            database.execSQL(indexStatement)
        } catch (e: SQLiteException) {
            val errorMsg = e.message?.lowercase() ?: ""
            // Если индекс уже существует - это нормально
            if (errorMsg.contains("already exists")) {
                Log.d(
                    "WassertechRoomMigration",
                    "Index already exists for table $tableName, skipping"
                )
            } 
            // Если таблицы нет (хотя мы проверили, но на всякий случай)
            else if (errorMsg.contains("no such table")) {
                Log.w(
                    "WassertechRoomMigration",
                    "Table $tableName not found during CREATE INDEX, skipping"
                )
            } 
            // Другие ошибки - логируем, но не пробрасываем (индексы не критичны)
            else {
                Log.w(
                    "WassertechRoomMigration",
                    "Error creating index for table $tableName (non-critical, continuing)",
                    e
                )
            }
        }
    }
    
    override fun migrate(database: SupportSQLiteDatabase) {
        // ========== clients ==========
        database.execSQL("""
            ALTER TABLE clients 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE clients 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE clients 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        // createdAtEpoch и updatedAtEpoch уже есть, не трогаем
        
        // ========== client_groups ==========
        database.execSQL("""
            ALTER TABLE client_groups 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE client_groups 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE client_groups 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        // createdAtEpoch и updatedAtEpoch уже есть, не трогаем
        
        // ========== sites ==========
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN createdAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN updatedAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        // isArchived и archivedAtEpoch уже есть, не трогаем
        
        // ========== installations ==========
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN createdAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN updatedAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        // isArchived и archivedAtEpoch уже есть, не трогаем
        
        // ========== components ==========
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN createdAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN updatedAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN archivedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        
        // ========== component_templates ==========
        // Таблица может отсутствовать в старых версиях БД
        safeAlterTable(
            database,
            "component_templates",
            "ALTER TABLE component_templates ADD COLUMN archivedAtEpoch INTEGER DEFAULT NULL",
            "archivedAtEpoch"
        )
        safeAlterTable(
            database,
            "component_templates",
            "ALTER TABLE component_templates ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL",
            "deletedAtEpoch"
        )
        safeAlterTable(
            database,
            "component_templates",
            "ALTER TABLE component_templates ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0",
            "dirtyFlag"
        )
        safeAlterTable(
            database,
            "component_templates",
            "ALTER TABLE component_templates ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0",
            "syncStatus"
        )
        // createdAtEpoch, updatedAtEpoch, isArchived уже есть, не трогаем
        
        // ========== checklist_templates ==========
        // Таблица может отсутствовать в старых версиях БД
        safeAlterTable(
            database,
            "checklist_templates",
            "ALTER TABLE checklist_templates ADD COLUMN createdAtEpoch INTEGER NOT NULL DEFAULT 0",
            "createdAtEpoch"
        )
        safeAlterTable(
            database,
            "checklist_templates",
            "ALTER TABLE checklist_templates ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL",
            "deletedAtEpoch"
        )
        safeAlterTable(
            database,
            "checklist_templates",
            "ALTER TABLE checklist_templates ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0",
            "dirtyFlag"
        )
        safeAlterTable(
            database,
            "checklist_templates",
            "ALTER TABLE checklist_templates ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0",
            "syncStatus"
        )
        // updatedAtEpoch, isArchived, archivedAtEpoch уже есть, не трогаем
        
        // ========== checklist_fields ==========
        database.execSQL("""
            ALTER TABLE checklist_fields 
            ADD COLUMN createdAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE checklist_fields 
            ADD COLUMN updatedAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE checklist_fields 
            ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE checklist_fields 
            ADD COLUMN archivedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE checklist_fields 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE checklist_fields 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE checklist_fields 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        
        // ========== maintenance_sessions ==========
        database.execSQL("""
            ALTER TABLE maintenance_sessions 
            ADD COLUMN createdAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_sessions 
            ADD COLUMN updatedAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_sessions 
            ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_sessions 
            ADD COLUMN archivedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_sessions 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_sessions 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_sessions 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        // synced уже есть, оставляем для обратной совместимости
        
        // ========== maintenance_values ==========
        database.execSQL("""
            ALTER TABLE maintenance_values 
            ADD COLUMN createdAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_values 
            ADD COLUMN updatedAtEpoch INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_values 
            ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_values 
            ADD COLUMN archivedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_values 
            ADD COLUMN deletedAtEpoch INTEGER DEFAULT NULL
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_values 
            ADD COLUMN dirtyFlag INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        database.execSQL("""
            ALTER TABLE maintenance_values 
            ADD COLUMN syncStatus INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        
        // Создаём индексы для новых полей (для быстрого поиска "грязных" записей)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clients_dirtyFlag ON clients(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_clients_syncStatus ON clients(syncStatus)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_client_groups_dirtyFlag ON client_groups(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_client_groups_syncStatus ON client_groups(syncStatus)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sites_dirtyFlag ON sites(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sites_syncStatus ON sites(syncStatus)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_installations_dirtyFlag ON installations(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_installations_syncStatus ON installations(syncStatus)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_components_dirtyFlag ON components(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_components_syncStatus ON components(syncStatus)")
        // Индексы для component_templates и checklist_templates создаём безопасно
        safeCreateIndex(
            database,
            "component_templates",
            "CREATE INDEX IF NOT EXISTS index_component_templates_dirtyFlag ON component_templates(dirtyFlag)"
        )
        safeCreateIndex(
            database,
            "component_templates",
            "CREATE INDEX IF NOT EXISTS index_component_templates_syncStatus ON component_templates(syncStatus)"
        )
        safeCreateIndex(
            database,
            "checklist_templates",
            "CREATE INDEX IF NOT EXISTS index_checklist_templates_dirtyFlag ON checklist_templates(dirtyFlag)"
        )
        safeCreateIndex(
            database,
            "checklist_templates",
            "CREATE INDEX IF NOT EXISTS index_checklist_templates_syncStatus ON checklist_templates(syncStatus)"
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_fields_dirtyFlag ON checklist_fields(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_fields_syncStatus ON checklist_fields(syncStatus)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_sessions_dirtyFlag ON maintenance_sessions(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_sessions_syncStatus ON maintenance_sessions(syncStatus)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_values_dirtyFlag ON maintenance_values(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_values_syncStatus ON maintenance_values(syncStatus)")
    }
}

