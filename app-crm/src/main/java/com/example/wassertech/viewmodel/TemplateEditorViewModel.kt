
package ru.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.dao.ChecklistDao
import ru.wassertech.data.entities.ChecklistFieldEntity
import ru.wassertech.data.types.FieldType
import ru.wassertech.sync.markCreatedForSync
import ru.wassertech.sync.markUpdatedForSync
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class FieldDraft(
    val id: String? = null,
    val key: String = "",
    val label: String = "",
    val type: FieldType = FieldType.TEXT,
    val unit: String? = null,
    val min: String = "",
    val max: String = ""
)

class TemplateEditorViewModel(app: Application, private val templateId: String) : AndroidViewModel(app) {

    private val dao: ChecklistDao = AppDatabase.getInstance(app).checklistDao()

    val fields: StateFlow<List<ChecklistFieldEntity>> =
        dao.observeFields(templateId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addOrUpdate(d: FieldDraft) {
        val isNew = d.id == null
        val entity = ChecklistFieldEntity(
            id = d.id ?: UUID.randomUUID().toString(),
            templateId = templateId,
            key = d.key.ifBlank { (d.label.ifBlank { "field" } + "_" + System.currentTimeMillis()) },
            label = d.label.ifBlank { d.key.ifBlank { "Поле" } },
            type = d.type,
            unit = d.unit?.ifBlank { null },
            min = d.min.toDoubleOrNull(),
            max = d.max.toDoubleOrNull()
        )
        val markedEntity = if (isNew) entity.markCreatedForSync() else entity.markUpdatedForSync()
        viewModelScope.launch { dao.upsertField(markedEntity) }
    }

    fun delete(id: String) {
        viewModelScope.launch { dao.deleteFieldById(id) }
    }
}
