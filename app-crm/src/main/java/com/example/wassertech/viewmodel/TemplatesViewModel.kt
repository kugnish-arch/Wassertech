package ru.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ChecklistFieldEntity
import ru.wassertech.data.types.FieldType
import ru.wassertech.sync.DeletionTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TemplatesViewModel(app: Application) : AndroidViewModel(app) {

    data class UiField(
        val id: String,
        val templateId: String,
        val key: String,
        val label: String,
        val type: FieldType,
        val isForMaintenance: Boolean,
        val unit: String?,
        val min: String?,   // UI-friendly (TextField)
        val max: String?,   // UI-friendly (TextField)
        val errors: List<String> = emptyList()
    )

    private val db = AppDatabase.getInstance(app)
    private val templatesDao = db.templatesDao()

    private val _fields = MutableStateFlow<List<UiField>>(emptyList())
    val fields: StateFlow<List<UiField>> = _fields

    private var templateId: String = ""
    private val pendingDelete = mutableSetOf<String>()

    fun load(templateId: String) {
        this.templateId = templateId
        viewModelScope.launch(Dispatchers.IO) {
            val raw = templatesDao.getFieldsForTemplate(templateId)
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
            isForMaintenance = true,
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
            pendingDelete.forEach { 
                templatesDao.deleteField(it)
                DeletionTracker.markFieldDeleted(db, it)
            }
            pendingDelete.clear()
            // Если передан порядок полей, используем его, иначе используем текущий порядок
            val orderedFields = if (fieldOrder != null) {
                fieldOrder.mapNotNull { id -> _fields.value.find { it.id == id } }
            } else {
                _fields.value
            }
            orderedFields.forEachIndexed { index, ui ->
                templatesDao.upsertField(ui.toEntity(indexHint = index))
            }
        }
    }

    private fun ru.wassertech.data.entities.ChecklistFieldEntity.toUi(): UiField = UiField(
        id = id,
        templateId = templateId,
        key = key,
        label = label ?: "",
        type = type,
        isForMaintenance = isForMaintenance == true || (isForMaintenance == null),
        unit = unit,
        min = min?.toString(),
        max = max?.toString()
    )

    private fun UiField.toEntity(indexHint: Int): ru.wassertech.data.entities.ChecklistFieldEntity =
        ru.wassertech.data.entities.ChecklistFieldEntity(
            id = id,
            templateId = templateId,
            key = key,
            label = label,
            type = type,
            isForMaintenance = isForMaintenance,
            unit = unit,
            min = min?.toDoubleOrNull(),
            max = max?.toDoubleOrNull()
        )
}
