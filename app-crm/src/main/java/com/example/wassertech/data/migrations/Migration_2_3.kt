// data/migrations/Migration_2_3.kt
// MIGRATION_2_3
//
// Цель: добавить таблицу maintenance_values — новое хранилище значений ТО.
// Таблица связана с maintenance_sessions по sessionId (ON DELETE CASCADE).
// Поля соответствуют MaintenanceValueEntity:
//   - id (PRIMARY KEY)
//   - sessionId (FK -> maintenance_sessions.id)
//   - siteId (NOT NULL) — объект, к которому относится установка
//   - installationId (NULLABLE) — установка (может быть NULL)
//   - componentId (NOT NULL) — компонент
//   - fieldKey (NOT NULL) — ключ поля из шаблона
//   - valueText (TEXT, NULLABLE) — текст/число (хранится строкой)
//   - valueBool (INTEGER, NULLABLE) — чекбокс
//
// Важно: здесь НЕ создаём/НЕ меняем maintenance_sessions, предполагаем,
// что она уже создана и соответствует MaintenanceSessionEntity.
//
package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // Создаём таблицу maintenance_values с правильной схемой
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS maintenance_values (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                siteId TEXT NOT NULL,
                installationId TEXT,
                componentId TEXT NOT NULL,
                fieldKey TEXT NOT NULL,
                valueText TEXT,
                valueBool INTEGER,
                FOREIGN KEY(sessionId) REFERENCES maintenance_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Индексы, которые ожидает Room (по именам index_...)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_values_sessionId ON maintenance_values(sessionId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_values_siteId ON maintenance_values(siteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_values_installationId ON maintenance_values(installationId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_values_componentId ON maintenance_values(componentId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_values_fieldKey ON maintenance_values(fieldKey)")
    }
}
