package com.example.wassertech.data.migrations
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// --- МИГРАЦИЯ 7 → 8 ---
// 1) clients: добавляем колонку groupName (TEXT NOT NULL DEFAULT 'ROOT')
// 2) checklist_fields: добавляем колонку isForMaintenance (INTEGER NOT NULL DEFAULT 1)
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Табличные имена проверь по своим @Entity(tableName = "...")!
        db.execSQL("ALTER TABLE clients ADD COLUMN groupName TEXT NOT NULL DEFAULT 'ROOT'")
        db.execSQL("ALTER TABLE checklist_fields ADD COLUMN isForMaintenance INTEGER NOT NULL DEFAULT 1")
    }
}