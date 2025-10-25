package com.example.wassertech.data.seed

import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.data.types.FieldType
import java.util.UUID

object TemplateSeeder {
    private var done = false

    suspend fun seedOnce(db: AppDatabase) {
        if (done) return
        done = true

        suspend fun seed(type: ComponentType, title: String, fields: List<ChecklistFieldEntity>) {
            val t = ChecklistTemplateEntity(id = UUID.randomUUID().toString(), title = title, componentType = type)
            db.templatesDao().upsertTemplate(t)
            val actual = fields.map { it.copy(templateId = t.id) }
            db.templatesDao().upsertFields(actual)
        }

        seed(
            ComponentType.DOSING,
            "Дозирование — чек-лист",
            listOf(
                field("concentration", "Концентрация реагента", FieldType.NUMBER, unit = "г/л", min = 0.0),
                field("flow_rate", "Расход", FieldType.NUMBER, unit = "л/ч", min = 0.0),
                field("level", "Уровень в баке", FieldType.NUMBER, unit = "%", min = 0.0, max = 100.0),
                field("injector_ok", "Инжектор OK", FieldType.CHECKBOX),
                field("backflow", "Подпор", FieldType.CHECKBOX),
                field("suction", "Подсос воздуха", FieldType.CHECKBOX),
                field("pulsation", "Пульсации", FieldType.CHECKBOX),
                field("unexpected_drain", "Неожиданный слив", FieldType.CHECKBOX),
                field("damage", "Повреждения", FieldType.TEXT)
            )
        )

        seed(
            ComponentType.AERATION,
            "Аэрация/компрессор — чек-лист",
            listOf(
                field("pressure", "Давление", FieldType.NUMBER, unit = "бар", min = 0.0),
                field("air_flow", "Расход воздуха", FieldType.NUMBER, unit = "л/мин", min = 0.0),
                field("temperature", "Температура", FieldType.NUMBER, unit = "°C"),
                field("noise_vibration", "Шум/вибрация", FieldType.CHECKBOX),
                field("check_valve", "Обратный клапан", FieldType.CHECKBOX),
                field("delta_p", "Перепад", FieldType.NUMBER, unit = "бар", min = 0.0),
                field("drain", "Слив", FieldType.CHECKBOX),
                field("damage", "Повреждения", FieldType.TEXT)
            )
        )

        seed(
            ComponentType.FILTER,
            "Обезжелезиватель — чек-лист",
            listOf(
                field("p_before", "Давление ДО", FieldType.NUMBER, unit = "бар", min = 0.0),
                field("p_after", "Давление ПОСЛЕ", FieldType.NUMBER, unit = "бар", min = 0.0),
                field("turbidity", "Мутность", FieldType.NUMBER, unit = "NTU", min = 0.0),
                field("media_level", "Уровень загрузки", FieldType.NUMBER, unit = "%", min = 0.0, max = 100.0),
                field("rinse_schedule", "График промывок", FieldType.TEXT),
                field("color_odor", "Цвет/запах", FieldType.TEXT),
                field("drain", "Слив", FieldType.CHECKBOX),
                field("damage", "Повреждения", FieldType.TEXT)
            )
        )

        seed(
            ComponentType.SOFTENER,
            "Умягчитель — чек-лист",
            listOf(
                field("brine_draw_rate", "Скорость забора соли", FieldType.NUMBER, unit = "л/мин", min = 0.0),
                field("brine_level", "Уровень рассола", FieldType.NUMBER, unit = "%", min = 0.0, max = 100.0),
                field("resin_expansion", "Расширение смолы", FieldType.NUMBER, unit = "%", min = 0.0, max = 100.0),
                field("hardness_out", "Жёсткость на выходе", FieldType.NUMBER, unit = "мг-экв/л", min = 0.0),
                field("p_before", "Давление ДО", FieldType.NUMBER, unit = "бар", min = 0.0),
                field("p_after", "Давление ПОСЛЕ", FieldType.NUMBER, unit = "бар", min = 0.0),
                field("vacuum", "Вакуум", FieldType.CHECKBOX),
                field("valve_leaks", "Течи клапана", FieldType.CHECKBOX),
                field("drain", "Слив", FieldType.CHECKBOX),
                field("damage", "Повреждения", FieldType.TEXT)
            )
        )
    }

    private fun field(
        key: String,
        label: String,
        type: FieldType,
        unit: String? = null,
        min: Double? = null,
        max: Double? = null
    ): ChecklistFieldEntity = ChecklistFieldEntity(
        id = UUID.randomUUID().toString(),
        templateId = "", // will be replaced during seed
        key = key,
        label = label,
        type = type,
        unit = unit,
        min = min,
        max = max
    )
}
