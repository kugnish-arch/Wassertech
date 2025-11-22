package ru.wassertech.data.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Миграция с версии 10 на версию 11
 * 
 * Радикальное упрощение модели шаблонов:
 * - Удаляем таблицы checklist_templates и checklist_fields
 * - Переходим на единую модель: component_templates + component_template_fields
 * - Поля шаблона компонента теперь объединяют характеристики (isCharacteristic = true) 
 *   и пункты чек-листа ТО (isCharacteristic = false)
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("MIGRATION_10_11", "Начало миграции: объединение шаблонов")
        
        // 1. Создаем таблицу component_template_fields
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS component_template_fields (
                id TEXT PRIMARY KEY NOT NULL,
                templateId TEXT NOT NULL,
                key TEXT NOT NULL,
                label TEXT NOT NULL,
                type TEXT NOT NULL,
                unit TEXT,
                isCharacteristic INTEGER NOT NULL DEFAULT 0,
                isRequired INTEGER NOT NULL DEFAULT 0,
                defaultValueText TEXT,
                defaultValueNumber REAL,
                defaultValueBool INTEGER,
                min REAL,
                max REAL,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAtEpoch INTEGER NOT NULL DEFAULT 0,
                updatedAtEpoch INTEGER NOT NULL DEFAULT 0,
                isArchived INTEGER NOT NULL DEFAULT 0,
                archivedAtEpoch INTEGER,
                deletedAtEpoch INTEGER,
                dirtyFlag INTEGER NOT NULL DEFAULT 1,
                syncStatus INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())
        
        // Создаем индексы
        database.execSQL("CREATE INDEX IF NOT EXISTS index_component_template_fields_templateId ON component_template_fields(templateId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_component_template_fields_dirtyFlag ON component_template_fields(dirtyFlag)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_component_template_fields_syncStatus ON component_template_fields(syncStatus)")
        
        Log.d("MIGRATION_10_11", "Создана таблица component_template_fields")
        
        // 2. Переносим данные из checklist_templates и checklist_fields в новую модель
        
        // Сначала получаем все checklist_templates
        val checklistTemplatesCursor = database.query("SELECT * FROM checklist_templates")
        val checklistTemplates = mutableListOf<Map<String, Any?>>()
        
        while (checklistTemplatesCursor.moveToNext()) {
            val templateMap = mutableMapOf<String, Any?>()
            for (i in 0 until checklistTemplatesCursor.columnCount) {
                val columnName = checklistTemplatesCursor.getColumnName(i)
                when (checklistTemplatesCursor.getType(i)) {
                    android.database.Cursor.FIELD_TYPE_NULL -> templateMap[columnName] = null
                    android.database.Cursor.FIELD_TYPE_INTEGER -> templateMap[columnName] = checklistTemplatesCursor.getLong(i)
                    android.database.Cursor.FIELD_TYPE_FLOAT -> templateMap[columnName] = checklistTemplatesCursor.getDouble(i)
                    android.database.Cursor.FIELD_TYPE_STRING -> templateMap[columnName] = checklistTemplatesCursor.getString(i)
                    android.database.Cursor.FIELD_TYPE_BLOB -> templateMap[columnName] = checklistTemplatesCursor.getBlob(i)
                }
            }
            checklistTemplates.add(templateMap)
        }
        checklistTemplatesCursor.close()
        
        Log.d("MIGRATION_10_11", "Найдено checklist_templates: ${checklistTemplates.size}")
        
        // Для каждого checklist_template:
        // - Если есть связанный component_template (по componentTemplateId) - используем его
        // - Если нет - создаем новый component_template на основе checklist_template
        for (checklistTemplate in checklistTemplates) {
            val checklistTemplateId = checklistTemplate["id"] as? String ?: continue
            val checklistTitle = checklistTemplate["title"] as? String ?: "Шаблон"
            val componentTemplateIdFromChecklist = checklistTemplate["componentTemplateId"] as? String
            
            // Определяем, какой component_template использовать
            var targetComponentTemplateId: String? = componentTemplateIdFromChecklist
            
            // Проверяем, существует ли component_template с таким ID
            if (targetComponentTemplateId != null) {
                val existsCursor = database.query(
                    "SELECT COUNT(*) FROM component_templates WHERE id = ?",
                    arrayOf(targetComponentTemplateId)
                )
                val exists = existsCursor.moveToFirst() && existsCursor.getInt(0) > 0
                existsCursor.close()
                
                if (!exists) {
                    targetComponentTemplateId = null
                }
            }
            
            // Если component_template не найден, создаем новый
            if (targetComponentTemplateId == null) {
                // Используем ID из checklist_template как ID для component_template
                // Это сохранит связь с существующими данными
                targetComponentTemplateId = checklistTemplateId
                
                val createdAtEpoch = (checklistTemplate["createdAtEpoch"] as? Long) ?: System.currentTimeMillis()
                val updatedAtEpoch = (checklistTemplate["updatedAtEpoch"] as? Long) ?: createdAtEpoch
                val isArchived = (checklistTemplate["isArchived"] as? Long ?: 0L) != 0L
                val archivedAtEpoch = checklistTemplate["archivedAtEpoch"] as? Long
                val sortOrder = (checklistTemplate["sortOrder"] as? Long)?.toInt() ?: 0
                
                // Создаем component_template
                database.execSQL("""
                    INSERT OR REPLACE INTO component_templates (
                        id, name, category, defaultParamsJson,
                        createdAtEpoch, updatedAtEpoch, isArchived, archivedAtEpoch, deletedAtEpoch,
                        dirtyFlag, syncStatus, sortOrder
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(
                    targetComponentTemplateId,
                    checklistTitle,
                    null, // category
                    null, // defaultParamsJson
                    createdAtEpoch,
                    updatedAtEpoch,
                    if (isArchived) 1 else 0,
                    archivedAtEpoch,
                    null, // deletedAtEpoch
                    1, // dirtyFlag = true для синхронизации
                    1, // syncStatus = QUEUED
                    sortOrder
                ))
                
                Log.d("MIGRATION_10_11", "Создан component_template: id=$targetComponentTemplateId, name=$checklistTitle")
            }
            
            // Теперь переносим все checklist_fields для этого шаблона в component_template_fields
            val fieldsCursor = database.query(
                "SELECT * FROM checklist_fields WHERE templateId = ?",
                arrayOf(checklistTemplateId)
            )
            
            var fieldIndex = 0
            while (fieldsCursor.moveToNext()) {
                val fieldId = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow("id"))
                val key = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow("key"))
                val label = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow("label")) ?: ""
                val type = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow("type"))
                val unit = fieldsCursor.getString(fieldsCursor.getColumnIndexOrThrow("unit"))
                val min = if (fieldsCursor.isNull(fieldsCursor.getColumnIndexOrThrow("min"))) null 
                         else fieldsCursor.getDouble(fieldsCursor.getColumnIndexOrThrow("min"))
                val max = if (fieldsCursor.isNull(fieldsCursor.getColumnIndexOrThrow("max"))) null 
                         else fieldsCursor.getDouble(fieldsCursor.getColumnIndexOrThrow("max"))
                val isForMaintenance = (fieldsCursor.getLong(fieldsCursor.getColumnIndexOrThrow("isForMaintenance"))) != 0L
                val createdAtEpoch = fieldsCursor.getLong(fieldsCursor.getColumnIndexOrThrow("createdAtEpoch"))
                val updatedAtEpoch = fieldsCursor.getLong(fieldsCursor.getColumnIndexOrThrow("updatedAtEpoch"))
                val isArchived = (fieldsCursor.getLong(fieldsCursor.getColumnIndexOrThrow("isArchived"))) != 0L
                val archivedAtEpoch = if (fieldsCursor.isNull(fieldsCursor.getColumnIndexOrThrow("archivedAtEpoch"))) null 
                                      else fieldsCursor.getLong(fieldsCursor.getColumnIndexOrThrow("archivedAtEpoch"))
                val deletedAtEpoch = if (fieldsCursor.isNull(fieldsCursor.getColumnIndexOrThrow("deletedAtEpoch"))) null 
                                    else fieldsCursor.getLong(fieldsCursor.getColumnIndexOrThrow("deletedAtEpoch"))
                
                // Маппинг: isForMaintenance = false означает характеристику (isCharacteristic = true)
                // isForMaintenance = true означает чек-лист (isCharacteristic = false)
                // Но по умолчанию ставим isCharacteristic = false (чек-лист), как указано в требованиях
                val isCharacteristic = !isForMaintenance
                
                database.execSQL("""
                    INSERT OR REPLACE INTO component_template_fields (
                        id, templateId, key, label, type, unit,
                        isCharacteristic, isRequired, defaultValueText, defaultValueNumber, defaultValueBool,
                        min, max, sortOrder,
                        createdAtEpoch, updatedAtEpoch, isArchived, archivedAtEpoch, deletedAtEpoch,
                        dirtyFlag, syncStatus
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(), arrayOf(
                    fieldId,
                    targetComponentTemplateId, // templateId теперь указывает на component_template
                    key,
                    label,
                    type,
                    unit,
                    if (isCharacteristic) 1 else 0,
                    0, // isRequired = false по умолчанию
                    null, // defaultValueText
                    null, // defaultValueNumber
                    null, // defaultValueBool
                    min,
                    max,
                    fieldIndex, // sortOrder
                    createdAtEpoch,
                    updatedAtEpoch,
                    if (isArchived) 1 else 0,
                    archivedAtEpoch,
                    deletedAtEpoch,
                    1, // dirtyFlag = true для синхронизации
                    1  // syncStatus = QUEUED
                ))
                
                fieldIndex++
            }
            fieldsCursor.close()
            
            Log.d("MIGRATION_10_11", "Перенесено полей для шаблона $checklistTemplateId: $fieldIndex")
        }
        
        Log.d("MIGRATION_10_11", "Завершен перенос данных")
        
        // 3. Удаляем старые таблицы
        database.execSQL("DROP TABLE IF EXISTS checklist_fields")
        database.execSQL("DROP TABLE IF EXISTS checklist_templates")
        
        Log.d("MIGRATION_10_11", "Удалены старые таблицы checklist_fields и checklist_templates")
        Log.d("MIGRATION_10_11", "Миграция завершена успешно")
    }
}





