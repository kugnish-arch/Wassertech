package ru.wassertech.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ComponentTemplateFieldEntity
import ru.wassertech.data.types.FieldType
import ru.wassertech.sync.DeletionTracker
import ru.wassertech.sync.markCreatedForSync
import ru.wassertech.sync.markUpdatedForSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TemplatesViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "TemplatesViewModel"
    }

    data class UiField(
        val id: String,
        val templateId: String,
        val key: String,
        val label: String,
        val type: FieldType,
        val isCharacteristic: Boolean, // true = характеристика, false = чек-лист ТО
        val unit: String?,
        val min: String?,   // UI-friendly (TextField)
        val max: String?,   // UI-friendly (TextField)
        val errors: List<String> = emptyList()
    )

    private val db = AppDatabase.getInstance(app)
    private val fieldsDao = db.componentTemplateFieldsDao()

    private val _fields = MutableStateFlow<List<UiField>>(emptyList())
    val fields: StateFlow<List<UiField>> = _fields

    private var templateId: String = ""
    private val pendingDelete = mutableSetOf<String>()

    fun load(templateId: String) {
        this.templateId = templateId
        viewModelScope.launch(Dispatchers.IO) {
            val raw = fieldsDao.getFieldsForTemplate(templateId)
            _fields.value = raw.map { it.toUi() }
        }
    }

    fun addField() {
        val f = UiField(
            id = UUID.randomUUID().toString(),
            templateId = templateId,
            key = "field_" + System.currentTimeMillis(),
            label = "Новое поле",
            type = ru.wassertech.data.types.FieldType.TEXT,
            isCharacteristic = false, // По умолчанию чек-лист ТО
            unit = null,
            min = null,
            max = null
        )
        _fields.value = _fields.value + f
    }

    fun update(id: String, transform: (UiField) -> UiField) {
        _fields.value = _fields.value.map { if (it.id == id) transform(it) else it }
    }

    fun setType(id: String, type: ru.wassertech.data.types.FieldType) {
        update(id) {
            it.copy(
                type = type,
                min = if (type == ru.wassertech.data.types.FieldType.NUMBER) it.min else null,
                max = if (type == ru.wassertech.data.types.FieldType.NUMBER) it.max else null
            )
        }
    }

    fun duplicate(id: String) {
        val src = _fields.value.firstOrNull { it.id == id } ?: return
        val copy = src.copy(
            id = java.util.UUID.randomUUID().toString(),
            key = src.key + "_copy"
        )
        _fields.value = _fields.value + copy
    }

    fun remove(id: String) {
        pendingDelete += id
        _fields.value = _fields.value.filterNot { it.id == id }
    }

    suspend fun saveAll(fieldOrder: List<String>? = null) {
        withContext(Dispatchers.IO) {
            // Удаляем помеченные поля
            val deletedCount = pendingDelete.size
            pendingDelete.forEach { fieldId ->
                Log.d(TAG, "Удаление поля шаблона: templateId=$templateId, fieldId=$fieldId")
                fieldsDao.deleteField(fieldId)
                DeletionTracker.markComponentTemplateFieldDeleted(db, fieldId)
            }
            pendingDelete.clear()
            
            // Если передан порядок полей, используем его, иначе используем текущий порядок
            val orderedFields = if (fieldOrder != null) {
                fieldOrder.mapNotNull { id -> _fields.value.find { it.id == id } }
            } else {
                _fields.value
            }
            
            // Получаем существующие поля для определения новых
            val existingFields = fieldsDao.getFieldsForTemplate(templateId)
            val existingIds = existingFields.map { it.id }.toSet()
            
            var createdCount = 0
            var updatedCount = 0
            
            orderedFields.forEachIndexed { index, ui ->
                val entity = ui.toEntity(sortOrder = index)
                val markedEntity = if (existingIds.contains(ui.id)) {
                    updatedCount++
                    val updated = entity.markUpdatedForSync()
                    Log.d(TAG, "Обновление поля шаблона: templateId=$templateId, fieldId=${ui.id}, " +
                            "label=${ui.label}, isCharacteristic=${ui.isCharacteristic}, " +
                            "dirtyFlag=${updated.dirtyFlag}, syncStatus=${updated.syncStatus}, " +
                            "updatedAtEpoch=${updated.updatedAtEpoch}")
                    updated
                } else {
                    createdCount++
                    val created = entity.markCreatedForSync()
                    Log.d(TAG, "Создание поля шаблона: templateId=$templateId, fieldId=${ui.id}, " +
                            "label=${ui.label}, isCharacteristic=${ui.isCharacteristic}, " +
                            "dirtyFlag=${created.dirtyFlag}, syncStatus=${created.syncStatus}, " +
                            "createdAtEpoch=${created.createdAtEpoch}, updatedAtEpoch=${created.updatedAtEpoch}")
                    created
                }
                fieldsDao.upsertField(markedEntity)
            }
            
            Log.d(TAG, "Сохранение полей шаблона завершено: templateId=$templateId, " +
                    "создано=$createdCount, обновлено=$updatedCount, удалено=$deletedCount")
        }
    }

    private fun ComponentTemplateFieldEntity.toUi(): UiField = UiField(
        id = id,
        templateId = templateId,
        key = key,
        label = label,
        type = type,
        isCharacteristic = isCharacteristic,
        unit = unit,
        min = min?.toString(),
        max = max?.toString()
    )

    private fun UiField.toEntity(sortOrder: Int): ComponentTemplateFieldEntity =
        ComponentTemplateFieldEntity(
            id = id,
            templateId = templateId,
            key = key,
            label = label,
            type = type,
            unit = unit,
            isCharacteristic = isCharacteristic,
            isRequired = false,
            defaultValueText = null,
            defaultValueNumber = null,
            defaultValueBool = null,
            min = min?.toDoubleOrNull(),
            max = max?.toDoubleOrNull(),
            sortOrder = sortOrder
        )
}
