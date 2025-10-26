
package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ComponentTemplateEntity
import com.example.wassertech.repository.ComponentTemplatesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TemplatesViewModel(app: Application): AndroidViewModel(app) {
    private val repo = ComponentTemplatesRepository(AppDatabase.getInstance(app).componentTemplatesDao())

    val templates: StateFlow<List<ComponentTemplateEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, category: String?, defaultParamsJson: String?) {
        val item = ComponentTemplateEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            category = category?.takeIf { it.isNotBlank() },
            defaultParamsJson = defaultParamsJson
        )
        viewModelScope.launch { repo.upsert(item) }
    }

    fun update(item: ComponentTemplateEntity) {
        viewModelScope.launch { repo.upsert(item.copy(updatedAtEpoch = System.currentTimeMillis())) }
    }

    fun archive(id: String, arch: Boolean) { viewModelScope.launch { repo.archive(id, arch) } }

    fun moveUp(list: List<ComponentTemplateEntity>, idx: Int) {
        if (idx <= 0) return
        val a = list[idx - 1]
        val b = list[idx]
        viewModelScope.launch {
            repo.setSort(a.id, b.sortOrder)
            repo.setSort(b.id, a.sortOrder)
        }
    }
    fun moveDown(list: List<ComponentTemplateEntity>, idx: Int) {
        if (idx >= list.lastIndex) return
        val a = list[idx]
        val b = list[idx + 1]
        viewModelScope.launch {
            repo.setSort(a.id, b.sortOrder)
            repo.setSort(b.id, a.sortOrder)
        }
    }
}
