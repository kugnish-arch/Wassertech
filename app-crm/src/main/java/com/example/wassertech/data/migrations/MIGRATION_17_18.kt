package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 17 на версию 18
 * 
 * Добавление поля thumbnailLocalPath в таблицу icons для хранения локального пути к миниатюрам.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_17_18", "Начало миграции: добавление поля thumbnailLocalPath в таблицу icons")
        
        // Добавление поля thumbnailLocalPath в таблицу icons
        database.execSQL("""
            ALTER TABLE icons 
            ADD COLUMN thumbnailLocalPath TEXT
        """.trimIndent())
        
        Log.d("MIGRATION_17_18", "Миграция завершена: поле thumbnailLocalPath добавлено в таблицу icons")
    }
}

