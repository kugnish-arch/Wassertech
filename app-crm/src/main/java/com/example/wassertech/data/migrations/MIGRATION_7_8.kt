package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаем таблицу settings
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
            )
        """.trimIndent())
    }
}


