package com.example.wassertech.data.seed

import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
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
                        ComponentType.DOSING -> "Дозирование — базовый"
                        ComponentType.AERATION -> "Аэрация — базовый"
                        ComponentType.COMPRESSOR -> "Компрессор — базовый"
                        ComponentType.FILTER -> "Фильтр — базовый"
                        ComponentType.SOFTENER -> "Умягчитель — базовый"
                        ComponentType.RO -> "Обратный осмос — базовый"
                    },
                    componentType = type
                )
                templatesDao.upsertTemplate(template)

                val fields: List<ChecklistFieldEntity> = when (type) {
                    ComponentType.DOSING -> listOf(
                        field(template.id, "concentration", "Концентрация", "TEXT"),
                        field(template.id, "flow", "Расход", "NUMBER"),
                        field(template.id, "level", "Уровень", "NUMBER"),
                        field(template.id, "injector", "Инжектор", "CHECKBOX")
                    )
                    ComponentType.AERATION, ComponentType.COMPRESSOR -> listOf(
                        field(template.id, "pressure", "Давление", "NUMBER"),
                        field(template.id, "air_flow", "Расход воздуха", "NUMBER"),
                        field(template.id, "temperature", "Температура", "NUMBER")
                    )
                    ComponentType.FILTER -> listOf(
                        field(template.id, "pressure_in", "Давление до", "NUMBER"),
                        field(template.id, "pressure_out", "Давление после", "NUMBER"),
                        field(template.id, "turbidity", "Мутность", "NUMBER")
                    )
                    ComponentType.SOFTENER -> listOf(
                        field(template.id, "brine_level", "Уровень рассола", "NUMBER"),
                        field(template.id, "hardness_out", "Жёсткость на выходе", "NUMBER"),
                        field(template.id, "valve_leak", "Течи клапана", "CHECKBOX")
                    )
                    ComponentType.RO -> listOf(
                        field(template.id, "pressure_in", "Давление до", "NUMBER"),
                        field(template.id, "pressure_out", "Давление после", "NUMBER"),
                        field(template.id, "permeate", "Производительность", "NUMBER")
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
                "CHECKBOX" -> com.example.wassertech.data.types.FieldType.CHECKBOX
                "NUMBER" -> com.example.wassertech.data.types.FieldType.NUMBER
                else -> com.example.wassertech.data.types.FieldType.TEXT
            },
            unit = null,
            min = null,
            max = null
        )
}