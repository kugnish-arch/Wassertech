package ru.wassertech.client.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 10 на версию 11
 * 
 * Добавление полей origin и created_by_user_id для системы ролей и владения данными:
 * - sites: origin, created_by_user_id
 * - installations: origin, created_by_user_id
 * - components: origin, created_by_user_id
 * - component_templates: origin, created_by_user_id
 * - maintenance_sessions: origin, created_by_user_id
 * - maintenance_values: origin, created_by_user_id
 * 
 * Все существующие записи получают origin = 'CRM' (по умолчанию).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_10_11", "Начало миграции: добавление полей origin и created_by_user_id")
        
        // Таблица sites
        // Проверяем, существует ли уже поле origin (может быть добавлено ранее)
        try {
            database.execSQL("""
                ALTER TABLE sites 
                ADD COLUMN origin TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле origin уже существует в sites, пропускаем")
        }
        try {
            database.execSQL("""
                ALTER TABLE sites 
                ADD COLUMN created_by_user_id TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле created_by_user_id уже существует в sites, пропускаем")
        }
        database.execSQL("""
            UPDATE sites 
            SET origin = 'CRM' 
            WHERE origin IS NULL
        """.trimIndent())
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_sites_origin ON sites(origin)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_sites_origin уже существует, пропускаем")
        }
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_sites_created_by_user_id ON sites(created_by_user_id)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_sites_created_by_user_id уже существует, пропускаем")
        }
        
        // Таблица installations
        try {
            database.execSQL("""
                ALTER TABLE installations 
                ADD COLUMN origin TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле origin уже существует в installations, пропускаем")
        }
        try {
            database.execSQL("""
                ALTER TABLE installations 
                ADD COLUMN created_by_user_id TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле created_by_user_id уже существует в installations, пропускаем")
        }
        database.execSQL("""
            UPDATE installations 
            SET origin = 'CRM' 
            WHERE origin IS NULL
        """.trimIndent())
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_installations_origin ON installations(origin)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_installations_origin уже существует, пропускаем")
        }
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_installations_created_by_user_id ON installations(created_by_user_id)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_installations_created_by_user_id уже существует, пропускаем")
        }
        
        // Таблица components
        try {
            database.execSQL("""
                ALTER TABLE components 
                ADD COLUMN origin TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле origin уже существует в components, пропускаем")
        }
        try {
            database.execSQL("""
                ALTER TABLE components 
                ADD COLUMN created_by_user_id TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле created_by_user_id уже существует в components, пропускаем")
        }
        database.execSQL("""
            UPDATE components 
            SET origin = 'CRM' 
            WHERE origin IS NULL
        """.trimIndent())
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_components_origin ON components(origin)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_components_origin уже существует, пропускаем")
        }
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_components_created_by_user_id ON components(created_by_user_id)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_components_created_by_user_id уже существует, пропускаем")
        }
        
        // Таблица component_templates
        try {
            database.execSQL("""
                ALTER TABLE component_templates 
                ADD COLUMN origin TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле origin уже существует в component_templates, пропускаем")
        }
        try {
            database.execSQL("""
                ALTER TABLE component_templates 
                ADD COLUMN created_by_user_id TEXT
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Поле created_by_user_id уже существует в component_templates, пропускаем")
        }
        database.execSQL("""
            UPDATE component_templates 
            SET origin = 'CRM' 
            WHERE origin IS NULL
        """.trimIndent())
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_component_templates_origin ON component_templates(origin)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_component_templates_origin уже существует, пропускаем")
        }
        try {
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_component_templates_created_by_user_id ON component_templates(created_by_user_id)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Индекс index_component_templates_created_by_user_id уже существует, пропускаем")
        }
        
        // Таблица maintenance_sessions (если существует)
        try {
            database.execSQL("""
                ALTER TABLE maintenance_sessions 
                ADD COLUMN origin TEXT
            """.trimIndent())
            database.execSQL("""
                ALTER TABLE maintenance_sessions 
                ADD COLUMN created_by_user_id TEXT
            """.trimIndent())
            database.execSQL("""
                UPDATE maintenance_sessions 
                SET origin = 'CRM' 
                WHERE origin IS NULL
            """.trimIndent())
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_maintenance_sessions_origin ON maintenance_sessions(origin)
            """.trimIndent())
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_maintenance_sessions_created_by_user_id ON maintenance_sessions(created_by_user_id)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Таблица maintenance_sessions не существует или поля уже добавлены: ${e.message}")
        }
        
        // Таблица maintenance_values (если существует)
        try {
            database.execSQL("""
                ALTER TABLE maintenance_values 
                ADD COLUMN origin TEXT
            """.trimIndent())
            database.execSQL("""
                ALTER TABLE maintenance_values 
                ADD COLUMN created_by_user_id TEXT
            """.trimIndent())
            database.execSQL("""
                UPDATE maintenance_values 
                SET origin = 'CRM' 
                WHERE origin IS NULL
            """.trimIndent())
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_maintenance_values_origin ON maintenance_values(origin)
            """.trimIndent())
            database.execSQL("""
                CREATE INDEX IF NOT EXISTS index_maintenance_values_created_by_user_id ON maintenance_values(created_by_user_id)
            """.trimIndent())
        } catch (e: Exception) {
            Log.w("MIGRATION_10_11", "Таблица maintenance_values не существует или поля уже добавлены: ${e.message}")
        }
        
        Log.d("MIGRATION_10_11", "Миграция завершена: поля origin и created_by_user_id добавлены")
    }
}





