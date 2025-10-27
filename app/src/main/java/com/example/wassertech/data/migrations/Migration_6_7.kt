// data/migrations/Migration_6_7.kt
package com.example.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Проверяем, есть ли уже колонка templateId
        val cursor = db.query("PRAGMA table_info(components)")
        var hasTemplateId = false
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                val colName = if (nameIndex >= 0) it.getString(nameIndex) else it.getString(1)
                if (colName == "templateId") {
                    hasTemplateId = true
                    break
                }
            }
        }
        if (!hasTemplateId) {
            db.execSQL("ALTER TABLE components ADD COLUMN templateId TEXT")
        }
    }
}
