package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.dao.TemplatesDao
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlinx.coroutines.flow.map

class TemplatesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao: TemplatesDao = db.templatesDao()

    val templates: StateFlow<List<ChecklistTemplateEntity>> =
        dao.observeAllTemplates()
            .map { list -> list.sortedBy { it.title.lowercase() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createTemplateSimple(title: String, type: ComponentType) {
        viewModelScope.launch {
            val t = ChecklistTemplateEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                componentType = type
            )
            dao.upsertTemplate(t)
        }
    }
}
