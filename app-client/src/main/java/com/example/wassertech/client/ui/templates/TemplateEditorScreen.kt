package ru.wassertech.client.ui.templates

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.core.screens.templates.TemplateEditorScreenShared
import ru.wassertech.core.screens.templates.ui.TemplateEditorUiState
import ru.wassertech.core.screens.templates.ui.TemplateFieldUi
import java.util.UUID

private const val TAG = "TemplateEditorScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TemplateEditorScreen(
    templateId: String,
    onNavigateBack: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.getInstance(ctx) }
    val dao = remember { db.componentTemplatesDao() }
    val scope = rememberCoroutineScope()

    var templateName by remember { mutableStateOf<String>("Шаблон") }
    var isHeadComponent by remember { mutableStateOf<Boolean>(false) }
    var templateCategory by remember { mutableStateOf<String?>("COMPONENT") } // "COMPONENT" или "SENSOR"
    var sensorCode by remember { mutableStateOf<String>("") } // Код датчика для SENSOR шаблонов
    var isLoading by remember { mutableStateOf(true) }

    // Поля шаблона (пока пустой список, так как в app-client нет DAO для полей)
    var fields by remember { mutableStateOf<List<TemplateFieldUi>>(emptyList()) }
    var localFieldOrder by remember { mutableStateOf<List<String>>(emptyList()) }

    // Загружаем шаблон при первом запуске
    LaunchedEffect(templateId) {
        Log.d(TAG, "=== ПЕРЕХОД НА ЭКРАН РЕДАКТИРОВАНИЯ ШАБЛОНА ===")
        Log.d(TAG, "templateId=$templateId")
        withContext(Dispatchers.IO) {
            try {
                val template = dao.getById(templateId)
                template?.let {
                    templateName = it.name
                    // В app-client нет поля isHeadComponent, используем false
                    isHeadComponent = false
                    Log.d(TAG, "Шаблон успешно загружен из БД: name=${it.name}")
                } ?: run {
                    Log.w(TAG, "Шаблон не найден в БД: templateId=$templateId")
                }
                // В app-client пока нет DAO для полей шаблона, оставляем пустой список
                fields = emptyList()
                localFieldOrder = emptyList()
            } catch (e: Throwable) {
                Log.e(TAG, "Ошибка при загрузке шаблона из БД", e)
            } finally {
                isLoading = false
            }
        }
    }

    val uiState = remember(templateId, templateName, isHeadComponent, templateCategory, sensorCode, fields, localFieldOrder) {
        TemplateEditorUiState(
            templateId = templateId,
            templateName = templateName,
            isHeadComponent = isHeadComponent,
            category = templateCategory,
            sensorCode = sensorCode,
            fields = fields,
            localFieldOrder = localFieldOrder
        )
    }

    TemplateEditorScreenShared(
        state = uiState,
        onNameChange = { templateName = it },
        onIsHeadComponentChange = { isHeadComponent = it },
        externalPaddingValues = paddingValues,
        onFieldLabelChange = { fieldId, newLabel, autoKey ->
            Log.d(TAG, "Изменение label поля: fieldId=$fieldId, newLabel=$newLabel, autoKey=$autoKey")
            // Обновляем поле в списке
            fields = fields.map { field ->
                if (field.id == fieldId) {
                    field.copy(
                        label = newLabel,
                        key = if (field.key.isBlank()) autoKey else field.key // Обновляем key только если он был пустым
                    )
                } else {
                    field
                }
            }
        },
        onFieldTypeChange = { fieldId, newType ->
            Log.d(TAG, "Изменение типа поля: fieldId=$fieldId, newType=$newType")
            fields = fields.map { field ->
                if (field.id == fieldId) {
                    field.copy(type = newType)
                } else {
                    field
                }
            }
        },
        onFieldIsCharacteristicChange = { fieldId, isChar ->
            Log.d(TAG, "Изменение флага характеристики: fieldId=$fieldId, isChar=$isChar")
            fields = fields.map { field ->
                if (field.id == fieldId) {
                    field.copy(isCharacteristic = isChar)
                } else {
                    field
                }
            }
        },
        onFieldUnitChange = { fieldId, newUnit ->
            Log.d(TAG, "Изменение единицы измерения: fieldId=$fieldId, newUnit=$newUnit")
            fields = fields.map { field ->
                if (field.id == fieldId) {
                    field.copy(unit = newUnit.ifBlank { null })
                } else {
                    field
                }
            }
        },
        onFieldMinChange = { fieldId, newMin ->
            Log.d(TAG, "Изменение минимального значения: fieldId=$fieldId, newMin=$newMin")
            fields = fields.map { field ->
                if (field.id == fieldId) {
                    field.copy(min = newMin.ifBlank { null })
                } else {
                    field
                }
            }
        },
        onFieldMaxChange = { fieldId, newMax ->
            Log.d(TAG, "Изменение максимального значения: fieldId=$fieldId, newMax=$newMax")
            fields = fields.map { field ->
                if (field.id == fieldId) {
                    field.copy(max = newMax.ifBlank { null })
                } else {
                    field
                }
            }
        },
        onFieldRemove = { fieldId ->
            Log.d(TAG, "Удаление поля: fieldId=$fieldId")
            fields = fields.filter { it.id != fieldId }
            localFieldOrder = localFieldOrder.filter { it != fieldId }
        },
        onFieldAdd = {
            Log.d(TAG, "Добавление нового поля")
            // Создаем новое поле с уникальным ID
            val newFieldId = UUID.randomUUID().toString()
            val newField = TemplateFieldUi(
                id = newFieldId,
                templateId = templateId,
                key = "", // Пустой ключ, будет сгенерирован автоматически при сохранении
                label = "", // Пустой label, пользователь заполнит
                type = "TEXT", // По умолчанию TEXT
                isCharacteristic = false, // По умолчанию не характеристика
                unit = null,
                min = null,
                max = null,
                errors = emptyList()
            )
            // Добавляем поле в список и в порядок
            fields = fields + newField
            localFieldOrder = localFieldOrder + newFieldId
            Log.d(TAG, "Поле добавлено: id=$newFieldId")
        },
        onFieldsReordered = { newOrder ->
            localFieldOrder = newOrder
        },
        onSaveClick = {
            scope.launch {
                Log.d(TAG, "Сохранение шаблона: templateId=$templateId, name=$templateName")
                withContext(Dispatchers.IO) {
                    try {
                        val template = dao.getById(templateId)
                        template?.let { currentTemplate ->
                            if (currentTemplate.name != templateName) {
                                val updatedTemplate = currentTemplate.copy(
                                    name = templateName,
                                    updatedAtEpoch = System.currentTimeMillis(),
                                    dirtyFlag = true, // Помечаем как dirty для синхронизации
                                    syncStatus = 1 // QUEUED
                                )
                                Log.d(TAG, "Обновление шаблона: templateId=$templateId, " +
                                        "oldName=${currentTemplate.name}, newName=$templateName, " +
                                        "dirtyFlag=${updatedTemplate.dirtyFlag}, syncStatus=${updatedTemplate.syncStatus}")
                                dao.upsert(updatedTemplate)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Ошибка при сохранении шаблона", e)
                    }
                }
                Toast.makeText(ctx, "Шаблон сохранён", Toast.LENGTH_SHORT).show()
                // Возвращаемся на экран "Шаблоны" после сохранения
                onNavigateBack()
            }
        },
        translitFunction = null // В app-client нет функции транслитерации
    )
}

