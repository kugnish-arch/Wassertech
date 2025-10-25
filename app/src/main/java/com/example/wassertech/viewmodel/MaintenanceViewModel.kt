package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.MaintenanceSessionEntity
import com.example.wassertech.data.entities.ObservationEntity
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.data.entities.ChecklistFieldEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChecklistUiField(
    val key: String,
    val label: String,
    val type: com.example.wassertech.data.types.FieldType,
    val unit: String?,
    val min: Double?,
    val max: Double?,
    var boolValue: Boolean? = null,
    var numberValue: String = "",
    var textValue: String = ""
)

class MaintenanceViewModel(app: Application): AndroidViewModel(app) {
    private val db by lazy { AppDatabase.get(app) }
    private val templates by lazy { db.templatesDao() }
    private val hierarchy by lazy { db.hierarchyDao() }
    private val sessions by lazy { db.sessionsDao() }

    private val _fields = MutableStateFlow<List<ChecklistUiField>>(emptyList())
    val fields = _fields.asStateFlow()

    private var currentComponentId: String? = null

    fun loadForComponent(componentId: String) {
        if (componentId == currentComponentId) return
        currentComponentId = componentId
        viewModelScope.launch {
            val component = hierarchy.getComponent(componentId) ?: return@launch
            val template = templates.getTemplateByType(component.type) ?: return@launch
            val raw = templates.getFieldsForTemplate(template.id)
            _fields.value = raw.map {
                ChecklistUiField(
                    key = it.key,
                    label = it.label,
                    type = it.type,
                    unit = it.unit,
                    min = it.min,
                    max = it.max
                )
            }
        }
    }

    fun saveSession(siteId: String, installationId: String?, technician: String? = null, notes: String? = null) {
        val compId = currentComponentId ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = MaintenanceSessionEntity(
                id = UUID.randomUUID().toString(),
                siteId = siteId,
                installationId = installationId,
                startedAtEpoch = now,
                finishedAtEpoch = now,
                technician = technician,
                notes = notes,
                synced = false
            )
            sessions.upsertSession(session)

            val observations = fields.value.map { f ->
                ObservationEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    componentId = compId,
                    fieldKey = f.key,
                    valueBool = f.boolValue,
                    valueNumber = f.numberValue.toDoubleOrNull(),
                    valueText = f.textValue.ifBlank { null }
                )
            }
            sessions.upsertObservations(observations)
        }
    }
}
