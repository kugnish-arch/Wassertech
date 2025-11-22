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
import java.util.UUID
import ru.wassertech.data.AppDatabase
import ru.wassertech.data.entities.ComponentTemplateFieldEntity
import ru.wassertech.data.types.FieldType
import ru.wassertech.util.Translit
import ru.wassertech.viewmodel.TemplatesViewModel
import ru.wassertech.sync.markCreatedForSync
import ru.wassertech.sync.markUpdatedForSync
import ru.wassertech.core.screens.templates.TemplateEditorScreenShared
import ru.wassertech.core.screens.templates.ComponentTemplateCategory
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
    var templateCategory by remember { mutableStateOf<String?>(null) } // "COMPONENT" или "SENSOR"
    var sensorCode by remember { mutableStateOf<String>("") } // Код датчика для SENSOR шаблонов

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
                    // Определяем category из шаблона или используем COMPONENT по умолчанию
                    val category = ComponentTemplateCategory.fromString(it.category)
                    templateCategory = ComponentTemplateCategory.toString(category)
                    
                    // Для SENSOR шаблонов загружаем код датчика из полей
                    if (category == ComponentTemplateCategory.SENSOR) {
                        val fields = db.componentTemplateFieldsDao().getFieldsForTemplate(templateId)
                        // Ищем поле с key="Имя датчика" (поле "Имя датчика")
                        // Если не найдено, берем первое поле (для обратной совместимости)
                        val sensorField = fields.find { it.key == "Имя датчика" } 
                            ?: fields.firstOrNull()
                        sensorCode = sensorField?.label ?: ""
                    }
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

    val uiState = remember(templateId, templateName, isHeadComponent, templateCategory, sensorCode, fieldsUi, localFieldOrder) {
        TemplateEditorUiState(
            templateId = templateId,
            templateName = templateName,
            isHeadComponent = isHeadComponent,
            category = templateCategory,
            sensorCode = sensorCode,
            fields = fieldsUi,
            localFieldOrder = localFieldOrder
        )
    }

    TemplateEditorScreenShared(
        state = uiState,
        onNameChange = { templateName = it },
        onIsHeadComponentChange = { isHeadComponent = it },
        onSensorCodeChange = { sensorCode = it },
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
                val category = ComponentTemplateCategory.fromString(templateCategory)
                
                withContext(Dispatchers.IO) {
                    try {
                        val template = db.componentTemplatesDao().getById(templateId)
                        template?.let { currentTemplate ->
                            // Обновляем шаблон
                            val updatedTemplate = currentTemplate.copy(
                                name = templateName,
                                isHeadComponent = isHeadComponent,
                                category = ComponentTemplateCategory.toString(category)
                            ).markUpdatedForSync()
                            db.componentTemplatesDao().upsert(updatedTemplate)
                            
                            // Для SENSOR шаблонов - особая логика сохранения полей
                            if (category == ComponentTemplateCategory.SENSOR) {
                                val fieldsDao = db.componentTemplateFieldsDao()
                                
                                // Удаляем все существующие поля шаблона
                                val existingFields = fieldsDao.getFieldsForTemplate(templateId)
                                existingFields.forEach { field ->
                                    fieldsDao.deleteField(field.id)
                                }
                                
                                // Создаём единственное поле с name="Имя датчика" и label=код датчика
                                if (sensorCode.isNotBlank()) {
                                    val sensorField = ComponentTemplateFieldEntity(
                                        id = UUID.randomUUID().toString(),
                                        templateId = templateId,
                                        key = "Имя датчика", // name = "Имя датчика" (жёстко зашитое значение)
                                        label = sensorCode, // Код датчика из UI
                                        type = FieldType.TEXT,
                                        unit = null,
                                        isCharacteristic = false,
                                        isRequired = false,
                                        defaultValueText = null,
                                        defaultValueNumber = null,
                                        defaultValueBool = null,
                                        min = null,
                                        max = null,
                                        sortOrder = 0
                                    ).markCreatedForSync()
                                    
                                    fieldsDao.upsertField(sensorField)
                                    Log.d(TAG, "Создано поле датчика: key=Имя датчика, label=$sensorCode")
                                }
                            } else {
                                // Для COMPONENT шаблонов - обычная логика сохранения полей
                                vm.saveAll(localFieldOrder)
                            }
                            
                            Log.d(TAG, "Обновление шаблона: templateId=$templateId, " +
                                    "oldName=${currentTemplate.name}, newName=$templateName, " +
                                    "category=${updatedTemplate.category}, " +
                                    "dirtyFlag=${updatedTemplate.dirtyFlag}, syncStatus=${updatedTemplate.syncStatus}, " +
                                    "updatedAtEpoch=${updatedTemplate.updatedAtEpoch}")
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

