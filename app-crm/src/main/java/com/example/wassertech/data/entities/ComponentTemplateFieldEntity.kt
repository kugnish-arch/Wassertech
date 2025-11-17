package ru.wassertech.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import ru.wassertech.data.types.FieldType

/**
 * Сущность поля шаблона компонента.
 * Объединяет характеристики железа (isCharacteristic = true) и пункты чек-листа ТО (isCharacteristic = false).
 * Реализует контракт SyncMetaEntity (все поля синхронизации присутствуют).
 */
@Entity(
    tableName = "component_template_fields",
    indices = [
        Index("templateId"),
        Index("dirtyFlag"),
        Index("syncStatus")
    ]
)
data class ComponentTemplateFieldEntity(
    @PrimaryKey val id: String,
    val templateId: String, // FK на component_templates.id
    val key: String, // внутренний ключ ("pressure_before", "hardness_after", …)
    val label: String, // подпись в UI
    val type: FieldType, // NUMBER / BOOL / TEXT / SELECT ...
    val unit: String? = null, // "бар", "°Ж", "м³/ч", …
    
    /**
     * Главный флаг разделения:
     * true → поле относится к характеристикам железа (паспортные константы)
     * false → поле относится к чек-листу ТО (то, что проверяем при каждом обслуживании)
     */
    @ColumnInfo(name = "isCharacteristic", defaultValue = "0")
    val isCharacteristic: Boolean = false,
    
    val isRequired: Boolean = false,
    val defaultValueText: String? = null,
    val defaultValueNumber: Double? = null,
    val defaultValueBool: Boolean? = null,
    val min: Double? = null,
    val max: Double? = null,
    val sortOrder: Int = 0, // порядок вывода
    
    // Поля синхронизации (SyncMetaEntity)
    val createdAtEpoch: Long = 0,
    val updatedAtEpoch: Long = 0,
    val isArchived: Boolean = false,
    val archivedAtEpoch: Long? = null,
    val deletedAtEpoch: Long? = null,
    // Локальные поля для оффлайн-очереди (не отправляются на сервер)
    val dirtyFlag: Boolean = false,
    val syncStatus: Int = 0 // 0 = SYNCED, 1 = QUEUED, 2 = CONFLICT
)


