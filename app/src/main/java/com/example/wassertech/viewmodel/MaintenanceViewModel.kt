package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.ComponentEntity
import com.example.wassertech.data.entities.InstallationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()
    private val templatesDao = db.templatesDao()
    private val sessionsDao = db.sessionsDao()

    private val _fields = MutableStateFlow<List<com.example.wassertech.ui.maintenance.ChecklistUiField>>(emptyList())
    val fields: StateFlow<List<com.example.wassertech.ui.maintenance.ChecklistUiField>> = _fields

    suspend fun getInstallation(installationId: String): InstallationEntity? =
        hierarchyDao.getInstallation(installationId)

    fun loadForComponent(componentId: String) {
        viewModelScope.launch {
            val component: ComponentEntity? = hierarchyDao.getComponent(componentId)
            if (component != null) {
                val template = templatesDao.getTemplateByType(component.type)
                val list: List<ChecklistFieldEntity> = template?.let { templatesDao.getFieldsForTemplate(it.id) } ?: emptyList()
                _fields.value = list.map { f ->
                    com.example.wassertech.ui.maintenance.ChecklistUiField(
                        key = f.key,
                        label = f.label,
                        type = f.type,
                        unit = f.unit,
                        min = f.min,
                        max = f.max
                    )
                }
            } else {
                _fields.value = emptyList()
            }
        }
    }

    // Existing overloads
    fun saveSession() { viewModelScope.launch { /* TODO implement */ } }
    fun saveSession(installationId: String) { viewModelScope.launch { /* TODO implement */ } }
    fun saveSession(installationId: String, notes: String?) { viewModelScope.launch { /* TODO implement */ } }

    // Covariant overloads with UI fields list
    fun saveSession(installationId: String, fields: List<out com.example.wassertech.ui.maintenance.ChecklistUiField>) {
        viewModelScope.launch { /* TODO implement */ }
    }
    fun saveSession(
        installationId: String,
        fields: List<out com.example.wassertech.ui.maintenance.ChecklistUiField>,
        notes: String?
    ) {
        viewModelScope.launch { /* TODO implement */ }
    }

    // Named-args overload used by MaintenanceScreen.kt
    fun saveSession(siteId: String, installationId: String?, technician: String?, notes: String?) {
        viewModelScope.launch { /* TODO implement actual persistence */ }
    }
}