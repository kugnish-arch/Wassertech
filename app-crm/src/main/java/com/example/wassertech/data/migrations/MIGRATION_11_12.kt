package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 11 на версию 12
 * 
 * Добавление поля isHeadComponent в таблицу component_templates
 * для явного указания заглавных компонентов (вместо определения через category/name)
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_11_12", "Начало миграции: добавление поля isHeadComponent")
        
        // Добавляем поле isHeadComponent в таблицу component_templates
        database.execSQL("""
            ALTER TABLE component_templates 
            ADD COLUMN isHeadComponent INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        
        // Устанавливаем isHeadComponent = 1 для шаблонов, у которых category или name содержит "head"
        database.execSQL("""
            UPDATE component_templates 
            SET isHeadComponent = 1 
            WHERE LOWER(category) LIKE '%head%' 
               OR LOWER(name) LIKE '%head%'
        """.trimIndent())
        
        Log.d("MIGRATION_11_12", "Миграция завершена: поле isHeadComponent добавлено")
    }
}

