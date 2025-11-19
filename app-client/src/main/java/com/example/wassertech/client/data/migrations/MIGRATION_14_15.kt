package ru.wassertech.client.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 14 на версию 15
 * 
 * Добавление таблицы user_membership для контроля доступа пользователей к объектам и установкам.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_14_15", "Начало миграции: создание таблицы user_membership")
        
        // Создаём таблицу user_membership
        // ВАЖНО: не указываем DEFAULT значения, так как Room ожидает 'undefined'
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
        
        // Создаём индексы вручную, так как Room не создаёт их автоматически при миграции
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_membership_user_id ON user_membership(user_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_membership_scope ON user_membership(scope)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_membership_target_id ON user_membership(target_id)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_membership_dirty_flag ON user_membership(dirty_flag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_user_membership_sync_status ON user_membership(sync_status)")
        
        Log.d("MIGRATION_14_15", "Миграция завершена: таблица user_membership создана с индексами")
    }
}

