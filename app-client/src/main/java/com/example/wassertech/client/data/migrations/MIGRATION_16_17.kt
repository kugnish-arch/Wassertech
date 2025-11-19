package ru.wassertech.client.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 16 на версию 17
 * 
 * Добавление полей filePath, fileSize, mimeType, createdByUserId в таблицу reports
 * и индексов для siteId и updatedAtEpoch.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_16_17", "Начало миграции: добавление полей в таблицу reports")
        
        // Добавление новых полей
        database.execSQL("ALTER TABLE reports ADD COLUMN filePath TEXT")
        database.execSQL("ALTER TABLE reports ADD COLUMN fileSize INTEGER")
        database.execSQL("ALTER TABLE reports ADD COLUMN mimeType TEXT")
        database.execSQL("ALTER TABLE reports ADD COLUMN createdByUserId TEXT")
        
        // Добавление индексов
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_siteId ON reports(siteId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_updatedAtEpoch ON reports(updatedAtEpoch)")
        
        Log.d("MIGRATION_16_17", "Миграция завершена: поля добавлены в таблицу reports")
    }
}

