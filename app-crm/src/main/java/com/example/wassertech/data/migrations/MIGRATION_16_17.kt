package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 16 на версию 17
 * 
 * Добавление таблицы user_membership для контроля доступа пользователей к объектам и установкам.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_16_17", "Начало миграции: создание таблицы user_membership")
        
        // Создаём таблицу user_membership
        // ВАЖНО: не указываем DEFAULT значения, так как Room ожидает 'undefined'
        // Room автоматически создаст индексы на основе аннотаций @Index в Entity
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_membership (
                user_id TEXT NOT NULL,
                scope TEXT NOT NULL,
                target_id TEXT NOT NULL,
                created_at_epoch INTEGER NOT NULL,
                updated_at_epoch INTEGER NOT NULL,
                is_archived INTEGER NOT NULL,
                archived_at_epoch INTEGER,
                dirty_flag INTEGER NOT NULL,
                sync_status INTEGER NOT NULL,
                PRIMARY KEY (user_id, scope, target_id)
            )
        """.trimIndent())
        
        // Room автоматически создаст индексы на основе аннотаций @Index в Entity
        // Не создаём индексы вручную, чтобы избежать конфликтов имён
        
        Log.d("MIGRATION_16_17", "Миграция завершена: таблица user_membership создана")
    }
}

