
package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.ChecklistFieldEntity
import com.example.wassertech.data.entities.ComponentEntity
import com.example.wassertech.data.entities.InstallationEntity
import androidx.room.withTransaction
import com.example.wassertech.data.entities.MaintenanceSessionEntity
import com.example.wassertech.data.entities.ObservationEntity
import com.example.wassertech.data.types.FieldType
import com.example.wassertech.ui.maintenance.ChecklistUiField
import com.example.wassertech.ui.maintenance.ObservationDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()
    private val templatesDao = db.templatesDao()
    private val sessionsDao = db.sessionsDao()

    private val _installation = MutableStateFlow<InstallationEntity?>(null)
    val installation: StateFlow<InstallationEntity?> = _installation

    // UI model: componentId -> fields
    private val _uiFields = MutableStateFlow<Map<String, List<ChecklistUiField>>>(emptyMap())
    val uiFields: StateFlow<Map<String, List<ChecklistUiField>>> = _uiFields

    fun load(installationId: String) {
        viewModelScope.launch {
            val inst = hierarchyDao.getInstallation(installationId) ?: return@launch
            _installation.value = inst
            val components: List<ComponentEntity> =
                hierarchyDao.observeComponents(installationId).first() ?: emptyList()
            val byComponent = linkedMapOf<String, List<ChecklistUiField>>()
            for (c in components) {
                val tmplId = c.templateId ?: continue
                val fields: List<ChecklistFieldEntity> = templatesDao.getFieldsForTemplate(tmplId)
                val ui = fields.map { f ->
                    ChecklistUiField(
                        key = f.key,
                        label = f.label,
                        type = f.type,
                        unit = f.unit,
                        min = f.min,
                        max = f.max
                    )
                }
                byComponent[c.id] = ui
            }
            _uiFields.value = byComponent
        }
    }

    fun saveSession(
        installationId: String,
        dateEpochMillis: Long,
        valuesByComponent: Map<String, List<ChecklistUiField>>,
        notes: String?
    ) {
        viewModelScope.launch {
            val inst = hierarchyDao.getInstallation(installationId) ?: return@launch
            val sessionId = UUID.randomUUID().toString()
            val session = MaintenanceSessionEntity(
                id = sessionId,
                siteId = inst.siteId,
                installationId = installationId,
                startedAtEpoch = dateEpochMillis,
                finishedAtEpoch = dateEpochMillis,
                technician = null,
                notes = notes,
                synced = false
            )
            val observations = mutableListOf<ObservationEntity>()
            valuesByComponent.forEach { (componentId, fields) ->
                fields.forEach { f ->
                    val ob = when (f.type) {
                        FieldType.CHECKBOX -> ObservationEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            componentId = componentId,
                            fieldKey = f.key,
                            valueBool = f.boolValue
                        )

                        FieldType.NUMBER -> {
                            val num = f.numberValue.toDoubleOrNull()
                            if (num == null) null else ObservationEntity(
                                id = UUID.randomUUID().toString(),
                                sessionId = sessionId,
                                componentId = componentId,
                                fieldKey = f.key,
                                valueNumber = num
                            )
                        }

                        FieldType.TEXT -> if (f.textValue.isBlank()) null else ObservationEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            componentId = componentId,
                            fieldKey = f.key,
                            valueText = f.textValue
                        )
                    }
                    if (ob != null) observations.add(ob)
                }
            }
            db.withTransaction {
                sessionsDao.upsertSession(session)
                if (observations.isNotEmpty()) sessionsDao.upsertObservations(observations)
            }
        }
    }

    suspend fun getInstallationIdByComponent(componentId: String): String? {
        return hierarchyDao.getComponent(componentId)?.installationId
        suspend fun loadSessionDetails(sessionId: String): List<ObservationDetail> {
            val list = sessionsDao.getObservations(sessionId)
            val details = mutableListOf<ObservationDetail>()
            for (o in list) {
                val comp = hierarchyDao.getComponent(o.componentId)
                val componentName = comp?.name ?: o.componentId
                val value = when {
                    o.valueText != null -> o.valueText
                    o.valueNumber != null -> o.valueNumber.toString()
                    o.valueBool != null -> if (o.valueBool) "Да" else "Нет"
                    else -> ""
                }
                details.add(ObservationDetail(o.componentId, componentName, o.fieldKey, value))
            }
            return details
        }


        fun observeSessionsForInstallation(installationId: String): Flow<List<com.example.wassertech.data.entities.MaintenanceSessionEntity>> {
            return sessionsDao.observeSessionsByInstallation(installationId)
        }

    }
}
