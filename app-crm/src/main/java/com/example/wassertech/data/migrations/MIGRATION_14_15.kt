package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 14 на версию 15
 * 
 * Добавление таблицы icon_pack_sync_status для отслеживания статуса загрузки иконок.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_14_15", "Начало миграции: добавление таблицы icon_pack_sync_status")
        
        // Создание таблицы icon_pack_sync_status
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS icon_pack_sync_status (
                packId TEXT NOT NULL PRIMARY KEY,
                lastSyncEpoch INTEGER NOT NULL DEFAULT 0,
                isDownloaded INTEGER NOT NULL DEFAULT 0,
                totalIcons INTEGER NOT NULL DEFAULT 0,
                downloadedIcons INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icon_pack_sync_status_packId ON icon_pack_sync_status(packId)
        """.trimIndent())
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_icon_pack_sync_status_isDownloaded ON icon_pack_sync_status(isDownloaded)
        """.trimIndent())
        
        Log.d("MIGRATION_14_15", "Миграция завершена: таблица icon_pack_sync_status создана")
    }
}

