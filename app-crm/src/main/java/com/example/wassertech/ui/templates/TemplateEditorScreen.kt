package ru.wassertech.ui.templates

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.types.FieldType
import ru.wassertech.util.Translit
import ru.wassertech.viewmodel.TemplatesViewModel
import ru.wassertech.sync.markUpdatedForSync
import ru.wassertech.core.screens.templates.TemplateEditorScreenShared
import ru.wassertech.core.screens.templates.ui.TemplateEditorUiState
import ru.wassertech.core.screens.templates.ui.TemplateFieldUi

private const val TAG = "TemplateEditorScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TemplateEditorScreen(
    templateId: String,
    vm: TemplatesViewModel = viewModel(),
    onSaved: () -> Unit = {}
) {
    val fields by vm.fields.collectAsState()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val db = remember { AppDatabase.getInstance(ctx) }

    var templateName by remember { mutableStateOf<String>("Шаблон") }
    var isHeadComponent by remember { mutableStateOf<Boolean>(false) }

    // Локальный порядок полей для drag-and-drop
    var localFieldOrder by remember(fields.size) {
        mutableStateOf(fields.map { it.id })
    }

    // Обновляем локальный порядок при изменении списка полей
    LaunchedEffect(fields.size, fields.map { it.id }.toSet()) {
        val currentIds = fields.map { it.id }
        val newOrder = localFieldOrder.filter { it in currentIds } +
                currentIds.filter { it !in localFieldOrder }
        if (newOrder != localFieldOrder) {
            localFieldOrder = newOrder
        }
    }

    LaunchedEffect(templateId) {
        vm.load(templateId)
        withContext(Dispatchers.IO) {
            try {
                val template = db.componentTemplatesDao().getById(templateId)
                template?.let {
                    templateName = it.name
                    isHeadComponent = it.isHeadComponent
                }
            } catch (_: Throwable) {
            }
        }
    }

    // Преобразуем поля в UI State (преобразуем FieldType enum в строку)
    val fieldsUi = remember(fields) {
        fields.map { field ->
            TemplateFieldUi(
                id = field.id,
                templateId = field.templateId,
                key = field.key,
                label = field.label,
                type = field.type.name, // Преобразуем enum в строку
                isCharacteristic = field.isCharacteristic,
                unit = field.unit,
                min = field.min,
                max = field.max,
                errors = field.errors
            )
        }
    }

    val uiState = remember(templateId, templateName, isHeadComponent, fieldsUi, localFieldOrder) {
        TemplateEditorUiState(
            templateId = templateId,
            templateName = templateName,
            isHeadComponent = isHeadComponent,
            fields = fieldsUi,
            localFieldOrder = localFieldOrder
        )
    }

    TemplateEditorScreenShared(
        state = uiState,
        onNameChange = { templateName = it },
        onIsHeadComponentChange = { isHeadComponent = it },
        onFieldLabelChange = { fieldId, newLabel, autoKey ->
            val prevAuto = Translit.ruToEnKey(fields.find { it.id == fieldId }?.label ?: "")
            val field = fields.find { it.id == fieldId } ?: return@TemplateEditorScreenShared
            val looksAuto = field.key.isBlank() ||
                    field.key == prevAuto ||
                    field.key.startsWith("field_") ||
                    (field.key.any { it.isDigit() } && field.key.length >= 12)
            vm.update(fieldId) {
                it.copy(
                    label = newLabel,
                    key = if (looksAuto) autoKey else it.key
                )
            }
        },
        onFieldTypeChange = { fieldId, newTypeStr ->
            // Преобразуем строку обратно в FieldType enum
            val newType = try {
                FieldType.valueOf(newTypeStr)
            } catch (e: IllegalArgumentException) {
                FieldType.TEXT // Fallback
            }
            vm.setType(fieldId, newType)
        },
        onFieldIsCharacteristicChange = { fieldId, isChar ->
            vm.update(fieldId) { it.copy(isCharacteristic = isChar) }
        },
        onFieldUnitChange = { fieldId, newUnit ->
            vm.update(fieldId) { it.copy(unit = newUnit) }
        },
        onFieldMinChange = { fieldId, newMin ->
            vm.update(fieldId) { it.copy(min = newMin) }
        },
        onFieldMaxChange = { fieldId, newMax ->
            vm.update(fieldId) { it.copy(max = newMax) }
        },
        onFieldRemove = { fieldId ->
            vm.remove(fieldId)
        },
        onFieldAdd = {
            vm.addField()
        },
        onFieldsReordered = { newOrder ->
            localFieldOrder = newOrder
        },
        onSaveClick = {
            scope.launch {
                Log.d(TAG, "Сохранение шаблона: templateId=$templateId")
                vm.saveAll(localFieldOrder)
                withContext(Dispatchers.IO) {
                    try {
                        val template = db.componentTemplatesDao().getById(templateId)
                        template?.let { currentTemplate ->
                            if (currentTemplate.name != templateName || currentTemplate.isHeadComponent != isHeadComponent) {
                                val updatedTemplate = currentTemplate.copy(
                                    name = templateName,
                                    isHeadComponent = isHeadComponent
                                ).markUpdatedForSync()
                                Log.d(TAG, "Обновление шаблона: templateId=$templateId, " +
                                        "oldName=${currentTemplate.name}, newName=$templateName, " +
                                        "oldIsHeadComponent=${currentTemplate.isHeadComponent}, newIsHeadComponent=$isHeadComponent, " +
                                        "dirtyFlag=${updatedTemplate.dirtyFlag}, syncStatus=${updatedTemplate.syncStatus}, " +
                                        "updatedAtEpoch=${updatedTemplate.updatedAtEpoch}")
                                db.componentTemplatesDao().upsert(updatedTemplate)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Ошибка при сохранении шаблона", e)
                    }
                }
                Toast.makeText(ctx, "Шаблон сохранён", Toast.LENGTH_SHORT).show()
                onSaved()
            }
        },
        translitFunction = { text -> Translit.ruToEnKey(text) }
    )
}

