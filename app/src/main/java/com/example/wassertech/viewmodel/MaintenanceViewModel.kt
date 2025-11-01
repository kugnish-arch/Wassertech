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
import java.util.UUID
import com.example.wassertech.data.dao.SessionsDao
import com.example.wassertech.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow


// viewmodel/MaintenanceViewModel.kt (вверху файла)
data class FieldValue(
    val installationId: String,
    val componentId: String,
    val fieldKey: String,
    val type: String,          // "TEXT" | "NUMBER" | "CHECKBOX"
    val value: Any?            // String/Double/Boolean
)

class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val hierarchyDao = db.hierarchyDao()
    private val templatesDao = db.templatesDao()
    private val sessionsDao = db.sessionsDao() // на будущее — сохранение сессий

    // Поля для UI (экран ТО): реактивное хранилище UI-представления полей
    private val _fields =
        MutableStateFlow<List<com.example.wassertech.ui.maintenance.ChecklistUiField>>(emptyList())
    val fields: StateFlow<List<com.example.wassertech.ui.maintenance.ChecklistUiField>> = _fields

    // --- Публичные методы ---

    /** Получить установку (для заголовка и т.п.) */
    suspend fun getInstallation(installationId: String): InstallationEntity? =
        hierarchyDao.getInstallation(installationId)

    /**
     * Загрузить поля для проведения ТО по конкретному компоненту.
     * Берём templateId компонента и подгружаем ТОЛЬКО поля, отмеченные isForMaintenance = TRUE.
     */
    fun loadForComponent(componentId: String) {
        viewModelScope.launch {
            val component: ComponentEntity? = hierarchyDao.getComponent(componentId)
            val templateId: String? = component?.templateId

            if (templateId.isNullOrBlank()) {
                _fields.value = emptyList()
                return@launch
            }

            // Берём только поля, участвующие в ТО (isForMaintenance = 1)
            val list: List<ChecklistFieldEntity> =
                templatesDao.getMaintenanceFieldsForTemplate(templateId)

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
        }
    }

    // --- Заглушки сохранения сессий ТО (оставляю как у тебя, чтобы не ломать текущее поведение) ---

    fun saveSession() {
        viewModelScope.launch { /* TODO implement */ }
    }

    fun saveSession(installationId: String) {
        viewModelScope.launch { /* TODO implement */ }
    }

    fun saveSession(installationId: String, notes: String?) {
        viewModelScope.launch { /* TODO implement */ }
    }

    fun saveSession(
        installationId: String,
        fields: List<out com.example.wassertech.ui.maintenance.ChecklistUiField>
    ) {
        viewModelScope.launch { /* TODO implement */ }
    }

    fun saveSession(
        installationId: String,
        fields: List<out com.example.wassertech.ui.maintenance.ChecklistUiField>,
        notes: String?
    ) {
        viewModelScope.launch { /* TODO implement */ }
    }

    // Используется MaintenanceScreen.kt (по твоим комментариям)
    fun saveSession(
        siteId: String,
        installationId: String?,
        technician: String?,
        notes: String?
    ) {
        viewModelScope.launch { /* TODO implement actual persistence */ }
    }


    //=========== История ТО
    fun observeHistory(installationId: String): Flow<List<MaintenanceSessionEntity>> =
        sessionsDao.observeSessionsByInstallation(installationId)

    fun saveSession(
        siteId: String,
        installationId: String?,           // может быть null
        author: String?,                   // technician
        notes: String?,
        values: List<FieldValue>,          // твоя UI-модель значений
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val sessionId = UUID.randomUUID().toString()

            val session = MaintenanceSessionEntity(
                id = sessionId,
                siteId = siteId,
                installationId = installationId,
                startedAtEpoch = now,
                finishedAtEpoch = now,     // фиксируем в момент сохранения
                technician = author,
                notes = notes,
                synced = false
            )

            val valueRows = values.map { fv ->
                MaintenanceValueEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    siteId = siteId,
                    installationId = installationId,        // может быть null
                    componentId = fv.componentId,
                    fieldKey = fv.fieldKey,
                    valueText = when (fv.type) {
                        "TEXT", "NUMBER" -> fv.value?.toString()
                        else -> null
                    },
                    valueBool = when (fv.type) {
                        "CHECKBOX" -> (fv.value as? Boolean)
                        else -> null
                    }
                )
            }

            sessionsDao.insertSessionWithValues(session, valueRows)
            onSaved(sessionId)
        }
    }
}
