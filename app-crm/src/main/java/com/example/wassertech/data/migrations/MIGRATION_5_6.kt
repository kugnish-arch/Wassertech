package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграция с версии 5 на версию 6
 * Добавляет поля isArchived и archivedAtEpoch в таблицы sites и installations
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Добавляем поля в таблицу sites
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN archivedAtEpoch INTEGER
        """.trimIndent())
        
        // Добавляем поля в таблицу installations
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0
        """.trimIndent())
        
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN archivedAtEpoch INTEGER
        """.trimIndent())
        
        // Создаем индексы
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_sites_isArchived 
            ON sites(isArchived)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_installations_isArchived 
            ON installations(isArchived)
        """.trimIndent())
    }
}

