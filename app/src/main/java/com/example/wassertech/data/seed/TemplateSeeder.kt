package com.example.wassertech.data.seed

import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.data.types.FieldType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object TemplateSeeder {

    suspend fun seedOnce(db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.templatesDao()
        for (type in ComponentType.values()) {
            val existing = dao.getTemplateByType(type)
            if (existing == null) {
                val templateId = UUID.randomUUID().toString()
                val title = when (type) {
                    ComponentType.FILTER -> "Чек-лист: фильтр"
                    ComponentType.RO -> "Чек-лист: обратный осмос"
                    ComponentType.COMPRESSOR -> "Чек-лист: компрессор"
                    ComponentType.AERATION -> "Чек-лист: аэрация"
                    ComponentType.DOSING -> "Чек-лист: дозирование"
                    ComponentType.SOFTENER -> "Чек-лист: умягчитель"
                }
                val t = ChecklistTemplateEntity(id = templateId, title = title, componentType = type)
                dao.upsertTemplate(t)

                val fields = when (type) {
                    ComponentType.FILTER -> listOf(
                        cb("power_ok", "Питание присутствует"),
                        cb("leaks_absent", "Нет протечек"),
                        num("pressure_in", "Давление до", "бар", 0.0, 10.0),
                        num("pressure_out", "Давление после", "бар", 0.0, 10.0),
                        text("notes", "Примечания")
                    )
                    ComponentType.SOFTENER -> listOf(
                        cb("power_ok", "Питание присутствует"),
                        cb("valve_ok", "Клапан исправен"),
                        num("hardness_out", "Жёсткость на выходе", "°dH", 0.0, 30.0),
                        num("brine_draw_rate", "Скорость забора рассола", "л/мин", 0.0, 5.0),
                        text("notes", "Примечания")
                    )
                    ComponentType.RO -> listOf(
                        cb("power_ok", "Питание присутствует"),
                        num("permeate_flow", "Производительность пермеата", "л/ч", 0.0, 5000.0),
                        num("recovery", "Степень извлечения", "%", 0.0, 100.0),
                        num("pressure_feed", "Давление подачи", "бар", 0.0, 20.0),
                        text("notes", "Примечания")
                    )
                    ComponentType.DOSING -> listOf(
                        cb("pump_ok", "Насос исправен"),
                        num("dose_rate", "Расход реагента", "л/ч", 0.0, 10.0),
                        cb("suction_ok", "Подсос воздуха отсутствует"),
                        text("notes", "Примечания")
                    )
                    ComponentType.AERATION -> listOf(
                        cb("compressor_ok", "Компрессор исправен"),
                        num("air_flow", "Расход воздуха", "л/мин", 0.0, 1000.0),
                        num("pressure", "Давление", "бар", 0.0, 5.0),
                        text("notes", "Примечания")
                    )
                    ComponentType.COMPRESSOR -> listOf(
                        cb("power_ok", "Питание присутствует"),
                        num("pressure", "Давление", "бар", 0.0, 10.0),
                        num("temperature", "Температура", "°C", -20.0, 120.0),
                        text("notes", "Примечания")
                    )
                }

                fields.forEach { f -> dao.upsertField(f.copy(id = UUID.randomUUID().toString(), templateId = templateId)) }
            }
        }
    }

    private fun cb(key: String, label: String) =
        ChecklistFieldEntity(id = "", templateId = "", key = key, label = label, type = FieldType.CHECKBOX, unit = null, min = null, max = null)

    private fun num(key: String, label: String, unit: String, min: Double, max: Double) =
        ChecklistFieldEntity(id = "", templateId = "", key = key, label = label, type = FieldType.NUMBER, unit = unit, min = min, max = max)

    private fun text(key: String, label: String) =
        ChecklistFieldEntity(id = "", templateId = "", key = key, label = label, type = FieldType.TEXT, unit = null, min = null, max = null)
}
