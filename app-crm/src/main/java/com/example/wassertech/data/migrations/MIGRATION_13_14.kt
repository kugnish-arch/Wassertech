package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 13 на версию 14
 * 
 * Добавление поддержки иконок:
 * - Создание таблиц icon_packs и icons
 * - Добавление поля icon_id в таблицы sites, installations, components
 * 
 * Все существующие записи получают icon_id = NULL.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_13_14", "Начало миграции: добавление поддержки иконок")
        
        // Создание таблицы icon_packs
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS icon_packs (
                id TEXT NOT NULL PRIMARY KEY,
                code TEXT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                isBuiltin INTEGER NOT NULL DEFAULT 0,
                isPremium INTEGER NOT NULL DEFAULT 0,
                origin TEXT,
                created_by_user_id TEXT,
                createdAtEpoch INTEGER NOT NULL DEFAULT 0,
                updatedAtEpoch INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icon_packs_code ON icon_packs(code)
        """.trimIndent())
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icon_packs_origin ON icon_packs(origin)
        """.trimIndent())
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icon_packs_created_by_user_id ON icon_packs(created_by_user_id)
        """.trimIndent())
        
        // Создание таблицы icons
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS icons (
                id TEXT NOT NULL PRIMARY KEY,
                packId TEXT NOT NULL,
                code TEXT NOT NULL,
                label TEXT NOT NULL,
                entityType TEXT NOT NULL,
                imageUrl TEXT,
                thumbnailUrl TEXT,
                androidResName TEXT,
                isActive INTEGER NOT NULL DEFAULT 1,
                origin TEXT,
                created_by_user_id TEXT,
                createdAtEpoch INTEGER NOT NULL DEFAULT 0,
                updatedAtEpoch INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(packId) REFERENCES icon_packs(id) ON DELETE CASCADE
            )
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icons_packId ON icons(packId)
        """.trimIndent())
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icons_code ON icons(code)
        """.trimIndent())
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icons_entityType ON icons(entityType)
        """.trimIndent())
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icons_isActive ON icons(isActive)
        """.trimIndent())
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icons_origin ON icons(origin)
        """.trimIndent())
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icons_created_by_user_id ON icons(created_by_user_id)
        """.trimIndent())
        
        // Добавление поля icon_id в таблицу sites
        database.execSQL("""
            ALTER TABLE sites 
            ADD COLUMN icon_id TEXT
        """.trimIndent())
        
        // Добавление поля icon_id в таблицу installations
        database.execSQL("""
            ALTER TABLE installations 
            ADD COLUMN icon_id TEXT
        """.trimIndent())
        
        // Добавление поля icon_id в таблицу components
        database.execSQL("""
            ALTER TABLE components 
            ADD COLUMN icon_id TEXT
        """.trimIndent())
        
        Log.d("MIGRATION_13_14", "Миграция завершена: таблицы иконок созданы, поля icon_id добавлены")
    }
}


