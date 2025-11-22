package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 18 на версию 19
 * 
 * Добавление таблицы reports для хранения PDF-отчётов.
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_18_19", "Начало миграции: добавление таблицы reports")
        
        // Создание таблицы reports
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reports (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT,
                clientId TEXT,
                siteId TEXT,
                installationId TEXT,
                fileName TEXT NOT NULL,
                fileUrl TEXT,
                filePath TEXT,
                fileSize INTEGER,
                mimeType TEXT,
                createdAtEpoch INTEGER NOT NULL,
                updatedAtEpoch INTEGER,
                createdByUserId TEXT,
                isArchived INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        
        // Создание индексов
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_clientId ON reports(clientId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_siteId ON reports(siteId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_sessionId ON reports(sessionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_installationId ON reports(installationId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_updatedAtEpoch ON reports(updatedAtEpoch)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_isArchived ON reports(isArchived)")
        
        Log.d("MIGRATION_18_19", "Миграция завершена: таблица reports создана")
    }
}




