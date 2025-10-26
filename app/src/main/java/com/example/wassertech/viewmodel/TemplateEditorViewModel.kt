
package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.dao.ChecklistDao
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.types.FieldType
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
        viewModelScope.launch { dao.upsertField(entity) }
    }

    fun delete(id: String) {
        viewModelScope.launch { dao.deleteFieldById(id) }
    }
}
