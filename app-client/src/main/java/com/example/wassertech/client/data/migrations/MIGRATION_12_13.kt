package ru.wassertech.client.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 12 на версию 13
 * 
 * Добавление поля folder в таблицу icon_packs для поддержки подпапок иконок.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_12_13", "Начало миграции: добавление поля folder в icon_packs")
        
        // Добавляем поле folder (nullable TEXT)
        database.execSQL("""
            ALTER TABLE icon_packs ADD COLUMN folder TEXT
        """.trimIndent())
        
        Log.d("MIGRATION_12_13", "Миграция завершена: поле folder добавлено в icon_packs")
    }
}

