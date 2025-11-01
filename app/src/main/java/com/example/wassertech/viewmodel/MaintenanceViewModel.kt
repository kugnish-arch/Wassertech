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

// ---------- UI-модели ----------

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

/** Секция экрана ТО: один компонент и его поля */
data class ComponentSectionUi(
    val componentId: String,
    val componentName: String,
    val fields: List<ChecklistUiField>,
    val expanded: Boolean = true
)

// ---------- ViewModel ----------

class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()
    private val templatesDao = db.templatesDao()
    private val sessionsDao = db.sessionsDao()

    /** Старое поле для режима "ТО одного компонента" — можем оставить на всякий случай */
    private val _fields = MutableStateFlow<List<ChecklistUiField>>(emptyList())
    val fields: StateFlow<List<ChecklistUiField>> = _fields

    /** Новый основной источник данных для экрана — секции по всем компонентам установки */
    private val _sections = MutableStateFlow<List<ComponentSectionUi>>(emptyList())
    val sections: StateFlow<List<ComponentSectionUi>> = _sections

    // ---------- Загрузка данных ----------

    /** (Опционально) Загрузка полей для одного компонента — legacy режим */
    fun loadForComponent(componentId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val comp = hierarchyDao.getComponent(componentId) ?: run {
                _fields.value = emptyList()
                return@launch
            }
            val tmplId = comp.templateId ?: run {
                _fields.value = emptyList()
                return@launch
            }

            val maintFields = templatesDao.getMaintenanceFieldsForTemplate(tmplId)
            val ui = maintFields.map { f ->
                ChecklistUiField(
                    key = f.key,
                    label = f.label,
                    type = f.type,
                    unit = f.unit,
                    min = f.min,        // если у тебя в БД minValue/maxValue — замени тут
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
                val fieldList: List<ChecklistUiField> =
                    comp.templateId?.let { tmplId ->
                        val maintFields = templatesDao.getMaintenanceFieldsForTemplate(tmplId)
                        maintFields.map { f ->
                            ChecklistUiField(
                                key = f.key,
                                label = f.label,
                                type = f.type,
                                unit = f.unit,
                                min = f.min,    // при необходимости -> f.minValue
                                max = f.max     // при необходимости -> f.maxValue
                            )
                        }
                    } ?: emptyList()

                ComponentSectionUi(
                    componentId = comp.id,
                    componentName = comp.name,
                    fields = fieldList,
                    expanded = true
                )
            }

            _sections.value = built
        }
    }

    // ---------- Изменение UI-состояния секций/полей (для "правильного" реактивного обновления) ----------

    fun toggleSection(componentId: String) {
        _sections.value = _sections.value.map { s ->
            if (s.componentId == componentId) s.copy(expanded = !s.expanded) else s
        }
    }

    /** Отметить/снять чекбокс (поле компонента) */
    fun setCheckbox(componentId: String, fieldKey: String, checked: Boolean) {
        _sections.value = _sections.value.map { s ->
            if (s.componentId != componentId) s else {
                val newFields = s.fields.map { f ->
                    if (f.key == fieldKey && f.type == FieldType.CHECKBOX) {
                        f.copy(boolValue = checked)
                    } else f
                }
                s.copy(fields = newFields)
            }
        }
    }

    /** Ввести числовой текст (хранится строкой) */
    fun setNumber(componentId: String, fieldKey: String, value: String) {
        _sections.value = _sections.value.map { s ->
            if (s.componentId != componentId) s else {
                val newFields = s.fields.map { f ->
                    if (f.key == fieldKey && f.type == FieldType.NUMBER) {
                        f.copy(numberValue = value)
                    } else f
                }
                s.copy(fields = newFields)
            }
        }
    }

    /** Ввести обычный текст */
    fun setText(componentId: String, fieldKey: String, value: String) {
        _sections.value = _sections.value.map { s ->
            if (s.componentId != componentId) s else {
                val newFields = s.fields.map { f ->
                    if (f.key == fieldKey && f.type == FieldType.TEXT) {
                        f.copy(textValue = value)
                    } else f
                }
                s.copy(fields = newFields)
            }
        }
    }

    // ---------- Сохранение ТО на всю установку ----------

    /**
     * Сохраняем одну сессию ТО для установки и все значения полей по всем компонентам.
     * Пустые значения не пишем.
     */
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

            val values = mutableListOf<MaintenanceValueEntity>()
            _sections.value.forEach { sec ->
                sec.fields.forEach { f ->
                    val (textValue, boolValue) = when (f.type) {
                        FieldType.CHECKBOX -> null to (f.boolValue == true)
                        FieldType.NUMBER   -> f.numberValue.takeIf { it.isNotBlank() } to null
                        FieldType.TEXT     -> f.textValue.takeIf { it.isNotBlank() } to null
                    }
                    if (textValue != null || boolValue != null) {
                        values += MaintenanceValueEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            siteId = siteId,                 // есть в твоём Entity
                            installationId = installationId,
                            componentId = sec.componentId,
                            fieldKey = f.key,
                            valueText = textValue,
                            valueBool = boolValue
                        )
                    }
                }
            }

            // атомарно: и сессию, и значения
            sessionsDao.insertSessionWithValues(session, values)
        }
    }
}
