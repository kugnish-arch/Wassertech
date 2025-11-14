package ru.wassertech.data.seed

import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ComponentTemplateEntity
import ru.wassertech.data.entities.ComponentTemplateFieldEntity
import ru.wassertech.sync.markCreatedForSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

object TemplateSeeder {

    suspend fun seedOnce(db: AppDatabase) = withContext(Dispatchers.IO) {
        val templatesDao = db.componentTemplatesDao()
        val fieldsDao = db.componentTemplateFieldsDao()

        // Создаем базовые шаблоны компонентов
        val basicTemplateName = "Компонент — базовый"
        val allTemplates = templatesDao.observeAll().first()
        val existingBasic = allTemplates.firstOrNull { it.name == basicTemplateName }
        
        if (existingBasic == null) {
            val template = ComponentTemplateEntity(
                id = UUID.randomUUID().toString(),
                name = basicTemplateName,
                category = null,
                defaultParamsJson = null,
                sortOrder = 0
            ).markCreatedForSync()
            templatesDao.upsert(template)

            val fields = listOf(
                field(template.id, 0, "pressure_in", "Давление до", "NUMBER", "бар", false),
                field(template.id, 1, "pressure_out", "Давление после", "NUMBER", "бар", false),
                field(template.id, 2, "status", "Состояние", "TEXT", null, false)
            )
            fields.forEach { fieldsDao.upsertField(it) }
        }
    }

    private fun field(
        templateId: String,
        sortOrder: Int,
        key: String,
        label: String,
        type: String,
        unit: String?,
        isCharacteristic: Boolean
    ) = ComponentTemplateFieldEntity(
        id = UUID.randomUUID().toString(),
        templateId = templateId,
        key = key,
        label = label,
        type = when (type) {
            "CHECKBOX" -> ru.wassertech.data.types.FieldType.CHECKBOX
            "NUMBER" -> ru.wassertech.data.types.FieldType.NUMBER
            else -> ru.wassertech.data.types.FieldType.TEXT
        },
        unit = unit,
        isCharacteristic = isCharacteristic,
        isRequired = false,
        defaultValueText = null,
        defaultValueNumber = null,
        defaultValueBool = null,
        min = null,
        max = null,
        sortOrder = sortOrder
    ).markCreatedForSync()
}