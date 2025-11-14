package ru.wassertech.client.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграция с версии 9 на версию 10
 * Обновляет таблицу deleted_records:
 * - Изменяет id с INTEGER на TEXT (UUID)
 * - Переименовывает tableName в entity
 * - Добавляет dirtyFlag и syncStatus
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаем новую таблицу с правильной структурой
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS deleted_records_new (
                id TEXT PRIMARY KEY NOT NULL,
                entity TEXT NOT NULL,
                recordId TEXT NOT NULL,
                deletedAtEpoch INTEGER NOT NULL,
                dirtyFlag INTEGER NOT NULL DEFAULT 1,
                syncStatus INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        
        // Копируем данные из старой таблицы в новую
        // Используем простой формат UUID: recordId + timestamp для уникальности
        // Переименовываем tableName в entity
        database.execSQL("""
            INSERT INTO deleted_records_new (id, entity, recordId, deletedAtEpoch, dirtyFlag, syncStatus)
            SELECT 
                'migrated-' || recordId || '-' || deletedAtEpoch as id,
                tableName as entity,
                recordId,
                deletedAtEpoch,
                1 as dirtyFlag,
                1 as syncStatus
            FROM deleted_records
        """.trimIndent())
        
        // Удаляем старую таблицу
        database.execSQL("DROP TABLE deleted_records")
        
        // Переименовываем новую таблицу
        database.execSQL("ALTER TABLE deleted_records_new RENAME TO deleted_records")
    }
}

