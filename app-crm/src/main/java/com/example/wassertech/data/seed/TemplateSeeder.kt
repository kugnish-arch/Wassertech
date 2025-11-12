package ru.wassertech.data.seed

import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ChecklistFieldEntity
import ru.wassertech.data.entities.ChecklistTemplateEntity
import ru.wassertech.data.types.ComponentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object TemplateSeeder {

    suspend fun seedOnce(db: AppDatabase) = withContext(Dispatchers.IO) {
        val templatesDao = db.templatesDao()

        for (type in ComponentType.values()) {
            val existing = templatesDao.getTemplateByType(type)
            if (existing == null) {
                val template = ChecklistTemplateEntity(
                    id = UUID.randomUUID().toString(),
                    title = when (type) {
                        ComponentType.COMMON -> "Компонент — базовый"
                        ComponentType.HEAD -> "Заглавный шаблон — базовый"
                    },
                    componentType = type
                )
                templatesDao.upsertTemplate(template)

                val fields: List<ChecklistFieldEntity> = when (type) {
                    ComponentType.COMMON -> listOf(
                        field(template.id, "pressure_in", "Давление до", "NUMBER"),
                        field(template.id, "pressure_out", "Давление после", "NUMBER"),
                        field(template.id, "status", "Состояние", "TEXT")
                    )
                    ComponentType.HEAD -> listOf(
                        field(template.id, "description", "Описание", "TEXT"),
                        field(template.id, "notes", "Заметки", "TEXT")
                    )
                }
                fields.forEach { templatesDao.upsertField(it) }
            }
        }
    }

    private fun field(templateId: String, key: String, label: String, type: String) =
        ChecklistFieldEntity(
            id = UUID.randomUUID().toString(),
            templateId = templateId,
            key = key,
            label = label,
            type = when (type) {
                "CHECKBOX" -> ru.wassertech.data.types.FieldType.CHECKBOX
                "NUMBER" -> ru.wassertech.data.types.FieldType.NUMBER
                else -> ru.wassertech.data.types.FieldType.TEXT
            },
            unit = null,
            min = null,
            max = null
        )
}