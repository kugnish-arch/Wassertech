package ru.wassertech.client.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 13 на версию 14
 * 
 * Добавление таблицы reports для хранения PDF-отчётов.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_13_14", "Начало миграции: добавление таблицы reports")
        
        // Создаём таблицу reports
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS reports (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT,
                clientId TEXT,
                siteId TEXT,
                installationId TEXT,
                fileName TEXT NOT NULL,
                fileUrl TEXT,
                createdAtEpoch INTEGER NOT NULL,
                updatedAtEpoch INTEGER,
                isArchived INTEGER NOT NULL DEFAULT 0,
                localFilePath TEXT,
                isDownloaded INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        
        // Создаём индексы
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_clientId ON reports(clientId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_sessionId ON reports(sessionId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_installationId ON reports(installationId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_reports_isArchived ON reports(isArchived)")
        
        Log.d("MIGRATION_13_14", "Миграция завершена: таблица reports создана")
    }
}


