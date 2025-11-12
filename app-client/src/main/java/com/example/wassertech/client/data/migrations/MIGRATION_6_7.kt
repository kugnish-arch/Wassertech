package ru.wassertech.client.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 6 на версию 7
 * Обновляет ComponentType: заменяет старые типы (FILTER, RO, SOFTENER, COMPRESSOR, AERATION, DOSING)
 * и NULL значения на COMMON в таблицах checklist_templates и components
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_6_7", "Starting migration from version 6 to 7")
        
        try {
            // Обновляем checklist_templates
            // Заменяем NULL и старые типы на COMMON
            database.execSQL("""
                UPDATE checklist_templates 
                SET componentType = 'COMMON' 
                WHERE componentType IS NULL 
                   OR componentType NOT IN ('COMMON', 'HEAD')
            """.trimIndent())
            Log.d("MIGRATION_6_7", "Updated checklist_templates")
            
            // Обновляем components
            // Заменяем старые типы на COMMON
            database.execSQL("""
                UPDATE components 
                SET type = 'COMMON' 
                WHERE type IS NULL 
                   OR type NOT IN ('COMMON', 'HEAD')
            """.trimIndent())
            Log.d("MIGRATION_6_7", "Updated components")
            
            Log.d("MIGRATION_6_7", "Migration completed successfully")
        } catch (e: Exception) {
            Log.e("MIGRATION_6_7", "Error during migration", e)
            throw e
        }
    }
}

