package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Добавляет поля сортировки/архива в таблицу checklist_templates
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // sortOrder: порядок сортировки (NULL = в конец)
        database.execSQL("ALTER TABLE checklist_templates ADD COLUMN sortOrder INTEGER;")
        // isArchived: 0/1 (NOT NULL, по умолчанию 0)
        database.execSQL("ALTER TABLE checklist_templates ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0;")
        // archivedAtEpoch: метка времени архивации (NULL если не в архиве)
        database.execSQL("ALTER TABLE checklist_templates ADD COLUMN archivedAtEpoch INTEGER;")
        // updatedAtEpoch: время последнего обновления (NULL допустимо)
        database.execSQL("ALTER TABLE checklist_templates ADD COLUMN updatedAtEpoch INTEGER;")
    }
}
