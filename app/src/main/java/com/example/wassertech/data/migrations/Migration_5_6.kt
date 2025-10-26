
package com.example.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * DB v5 -> v6: introduces component_templates + columns for components & checklist_templates.
 * - Creates table component_templates
 * - Adds columns to components: templateId, paramsJson, nameOverride
 * - Adds column to checklist_templates: componentTemplateId
 * NOTE: We keep logical FK without enforcing constraints for smoother migration.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS component_templates(
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT,
                isArchived INTEGER NOT NULL DEFAULT 0,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                defaultParamsJson TEXT,
                createdAtEpoch INTEGER NOT NULL,
                updatedAtEpoch INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("""ALTER TABLE components ADD COLUMN templateId TEXT""".trimIndent())
        db.execSQL("""ALTER TABLE components ADD COLUMN paramsJson TEXT""".trimIndent())
        db.execSQL("""ALTER TABLE components ADD COLUMN nameOverride TEXT""".trimIndent())
        db.execSQL("""CREATE INDEX IF NOT EXISTS index_components_templateId ON components(templateId)""".trimIndent())

        db.execSQL("""ALTER TABLE checklist_templates ADD COLUMN componentTemplateId TEXT""".trimIndent())
    }
}
