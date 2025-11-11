package com.example.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграция с версии 4 на версию 5
 * Добавляет таблицу deleted_records для отслеживания удалений
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS deleted_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tableName TEXT NOT NULL,
                recordId TEXT NOT NULL,
                deletedAtEpoch INTEGER NOT NULL
            )
        """.trimIndent())
    }
}


