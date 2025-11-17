package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 15 на версию 16
 * 
 * Добавление поля folder в таблицу icon_packs для поддержки подпапок иконок.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_15_16", "Начало миграции: добавление поля folder в icon_packs")
        
        // Добавляем поле folder (nullable TEXT)
        database.execSQL("""
            ALTER TABLE icon_packs ADD COLUMN folder TEXT
        """.trimIndent())
        
        Log.d("MIGRATION_15_16", "Миграция завершена: поле folder добавлено в icon_packs")
    }
}

