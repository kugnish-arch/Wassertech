package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Миграция схемы с v1 -> v2.
 *
 * Изменения:
 * 1) Новая таблица client_groups (группы клиентов).
 * 2) В таблице clients добавляем:
 *    - колонку clientGroupId (TEXT, NULL),
 *    - колонку name (TEXT, NULL) — если раньше было displayName.
 *    - переносим значения из displayName в name (только если name пуст).
 * 3) Создаём индексы, если их ещё нет.
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) Таблица групп клиентов
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `client_groups` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `title` TEXT NOT NULL,
                `notes` TEXT,
                `sortOrder` INTEGER NOT NULL DEFAULT 0,
                `isArchived` INTEGER NOT NULL DEFAULT 0,
                `archivedAtEpoch` INTEGER,
                `createdAtEpoch` INTEGER NOT NULL,
                `updatedAtEpoch` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Индексы для client_groups
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_client_groups_title` ON `client_groups` (`title`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_client_groups_isArchived` ON `client_groups` (`isArchived`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_client_groups_sortOrder` ON `client_groups` (`sortOrder`)"
        )

        // 2) Таблица clients: гарантируем наличие нужных колонок

        // 2.1 clientGroupId
        if (!hasColumn(db, "clients", "clientGroupId")) {
            db.execSQL("ALTER TABLE `clients` ADD COLUMN `clientGroupId` TEXT")
        }

        // 2.2 name (если раньше был displayName)
        val hasName = hasColumn(db, "clients", "name")
        val hasDisplayName = hasColumn(db, "clients", "displayName")

        if (!hasName) {
            db.execSQL("ALTER TABLE `clients` ADD COLUMN `name` TEXT")
        }

        // Если колонка displayName существует, а name либо отсутствовала/пуста — переносим значения
        if (hasDisplayName) {
            // Переносим только в те строки, где name IS NULL
            db.execSQL(
                """
                UPDATE `clients`
                SET `name` = COALESCE(`name`, `displayName`)
                WHERE `name` IS NULL
                """.trimIndent()
            )
        }

        // 3) Индексы для clients (минимум на name и clientGroupId; остальные оставляем как есть)
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_clients_name` ON `clients` (`name`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_clients_clientGroupId` ON `clients` (`clientGroupId`)"
        )
    }

    /**
     * Проверка наличия колонки через PRAGMA table_info(table)
     */
    private fun hasColumn(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex == -1) return false
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                if (name.equals(column, ignoreCase = false)) {
                    return true
                }
            }
        }
        return false
    }
}
