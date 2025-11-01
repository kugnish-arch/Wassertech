package com.example.wassertech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.MaintenanceSessionEntity
import com.example.wassertech.data.entities.MaintenanceValueEntity
import com.example.wassertech.data.types.FieldType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class ChecklistUiField(
    val key: String,
    val label: String,
    val type: FieldType,
    val unit: String? = null,
    val min: Double? = null,
    val max: Double? = null,
    var boolValue: Boolean? = null,
    var numberValue: String = "",
    var textValue: String = ""
)

data class ComponentSectionUi(
    val componentId: String,
    val componentName: String,
    val fields: MutableList<ChecklistUiField>
)

class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()
    private val templatesDao = db.templatesDao()
    private val sessionsDao = db.sessionsDao()

    private val _fields = MutableStateFlow<List<ChecklistUiField>>(emptyList())
    val fields: StateFlow<List<ChecklistUiField>> = _fields

    private val _sections = MutableStateFlow<List<ComponentSectionUi>>(emptyList())
    val sections: StateFlow<List<ComponentSectionUi>> = _sections

    /** Загрузка полей для одного компонента (старый режим, можно не использовать) */
    fun loadForComponent(componentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val comp = hierarchyDao.getComponent(componentId) ?: run {
                _fields.value = emptyList()
                return@launch
            }
            val tmplId = comp.templateId ?: return@launch

            val maintFields = templatesDao.getMaintenanceFieldsForTemplate(tmplId)
            val ui = maintFields.map { f ->
                ChecklistUiField(
                    key = f.key,
                    label = f.label,
                    type = f.type,
                    unit = f.unit,
                    min = f.min,
                    max = f.max
                )
            }
            _fields.value = ui
        }
    }

    /** ✅ Загрузка всех компонентов установки (каждый со своими полями ТО) */
    fun loadForInstallation(installationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val components = hierarchyDao.observeComponents(installationId).first()

            val built = components.map { comp ->
                val fields = if (comp.templateId != null) {
                    val maintFields = templatesDao.getMaintenanceFieldsForTemplate(comp.templateId!!)
                    maintFields.map { f ->
                        ChecklistUiField(
                            key = f.key,
                            label = f.label,
                            type = f.type,
                            unit = f.unit,
                            min = f.min,
                            max = f.max
                        )
                    }.toMutableList()
                } else mutableListOf()

                ComponentSectionUi(
                    componentId = comp.id,
                    componentName = comp.name,
                    fields = fields
                )
            }

            _sections.value = built
        }
    }

    /** ✅ Сохраняем одно ТО для всей установки */
    fun saveSession(
        siteId: String,
        installationId: String,
        technician: String?,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val sessionId = UUID.randomUUID().toString()

            val session = MaintenanceSessionEntity(
                id = sessionId,
                siteId = siteId,
                installationId = installationId,
                startedAtEpoch = System.currentTimeMillis(),
                finishedAtEpoch = null,
                technician = technician,
                notes = notes,
                synced = false
            )
            sessionsDao.upsertSession(session)

            // сохраняем значения всех полей
// ...после создания sessionId и session
            val values = mutableListOf<MaintenanceValueEntity>()

            _sections.value.forEach { sec ->
                sec.fields.forEach { f ->
                    val (textValue, boolValue) = when (f.type) {
                        FieldType.CHECKBOX -> null to (f.boolValue == true)
                        FieldType.NUMBER  -> (f.numberValue.takeIf { it.isNotBlank() }) to null
                        FieldType.TEXT    -> (f.textValue.takeIf { it.isNotBlank() }) to null
                    }
                    if (textValue != null || boolValue != null) {
                        values += MaintenanceValueEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            siteId = siteId,
                            installationId = installationId,
                            componentId = sec.componentId,
                            fieldKey = f.key,
                            valueText = textValue,
                            valueBool = boolValue
                        )
                    }
                }
            }

// атомарно: и сессию, и все значения
            sessionsDao.insertSessionWithValues(session, values)
        }
    }
}
