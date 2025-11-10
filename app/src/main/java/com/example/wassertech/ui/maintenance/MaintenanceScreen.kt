// MaintenanceScreen.kt
package com.example.wassertech.ui.maintenance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.data.types.FieldType
import com.example.wassertech.viewmodel.ChecklistUiField
import com.example.wassertech.viewmodel.MaintenanceViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.key

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    siteId: String,
    installationId: String,
    installationName: String,
    onNavigateBack: () -> Unit,               // остаётся (кнопка «Отмена»)
    onNavigateToHistory: (installationId: String) -> Unit,
    sessionId: String? = null                 // Если указан, режим редактирования
) {
    val vm: MaintenanceViewModel = viewModel()

    // грузим все поля по всем компонентам установки или данные существующей сессии
    LaunchedEffect(installationId, sessionId) {
        if (sessionId != null) {
            vm.loadForSession(sessionId)
        } else {
            vm.loadForInstallation(installationId)
        }
    }

    val sections by vm.sections.collectAsState()
    val scroll = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var technician by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf<String?>(null) }

    // локальные свёртки по componentId (по умолчанию все свернуты)
    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(sections) {
        sections.forEach { sec -> expandedMap.putIfAbsent(sec.componentId, false) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
        ) {
            // плашка заголовка (подтянута к апбару, без подзаголовка)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Иконка шестеренки убрана по требованию
                    Text(
                        text = "Обслуживание установки $installationName",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // секции по компонентам
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (sections.isEmpty()) {
                    Text(
                        "В установке нет компонентов с полями для ТО.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    sections.forEach { sec ->
                        key(sec.componentId) {
                            // Заглавные компоненты используют тот же фон что и COMMON компоненты
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    // заголовок секции - для заглавных компонентов такой же фон как у COMMON, но с жирным текстом и иконкой
                                    val expanded = expandedMap[sec.componentId] == true
                                    ListItem(
                                        leadingContent = if (sec.isHeadComponent) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Filled.Category,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        } else null,
                                        headlineContent = { 
                                            Text(
                                                sec.componentName,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = if (sec.isHeadComponent) {
                                                    MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    MaterialTheme.typography.titleMedium
                                                }
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                expandedMap[sec.componentId] = !expanded
                                            }) {
                                                Icon(
                                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                    contentDescription = if (expanded) "Свернуть" else "Развернуть"
                                                )
                                            }
                                        }
                                    )
                                    // контент секции
                                    if (expandedMap[sec.componentId] == true) {
                                        Column(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (sec.fields.isEmpty()) {
                                                Text(
                                                    "Нет полей для ТО",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            } else {
                                                sec.fields.forEach { f ->
                                                    key("${sec.componentId}:${f.key}") {
                                                        FieldRow(
                                                            sectionComponentId = sec.componentId,
                                                            f = f,
                                                            onCheckbox = { checked ->
                                                                vm.setCheckbox(sec.componentId, f.key, checked)
                                                            },
                                                            onNumberChange = { text ->
                                                                vm.setNumber(sec.componentId, f.key, text)
                                                            },
                                                            onTextChange = { text ->
                                                                vm.setText(sec.componentId, f.key, text)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // кнопки
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onNavigateBack) { Text("Отмена") }
                    Button(
                        onClick = {
                            if (sessionId != null) {
                                // Режим редактирования
                                vm.updateSession(
                                    sessionId = sessionId,
                                    siteId = siteId,
                                    installationId = installationId,
                                    technician = technician,
                                    notes = notes
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar("ТО обновлено")
                                    onNavigateBack()  // ✅ сразу возвращаемся на прошлый экран
                                }
                            } else {
                                // Режим создания новой сессии
                                vm.saveSession(
                                    siteId = siteId,
                                    installationId = installationId,
                                    technician = technician,
                                    notes = notes
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar("ТО сохранено")
                                    onNavigateBack()  // ✅ сразу возвращаемся на прошлый экран
                                }
                            }
                        },
                        enabled = sections.isNotEmpty()
                    ) { Text(if (sessionId != null) "Обновить" else "Сохранить") }

                }
            }
        }
    }
}

@Composable
private fun FieldRow(
    sectionComponentId: String,
    f: ChecklistUiField,
    onCheckbox: (Boolean) -> Unit,
    onNumberChange: (String) -> Unit,
    onTextChange: (String) -> Unit
) {
    when (f.type) {
        FieldType.CHECKBOX -> {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(f.label)
                Checkbox(
                    checked = f.boolValue == true,
                    onCheckedChange = onCheckbox
                )
            }
        }
        FieldType.NUMBER -> {
            Surface(
                color = MaterialTheme.colorScheme.surface, // Белый фон для полей
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = f.numberValue,
                    onValueChange = onNumberChange,
                    label = { Text(labelWithUnit(f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = {
                        val warn = validateNumber(f.numberValue, f.min, f.max)
                        if (warn != null) Text(warn)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        FieldType.TEXT -> {
            Surface(
                color = MaterialTheme.colorScheme.surface, // Белый фон для полей
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = f.textValue,
                    onValueChange = onTextChange,
                    label = { Text(f.label) },
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun labelWithUnit(f: ChecklistUiField): String =
    if (f.unit != null) "${f.label}, ${f.unit}" else f.label

private fun validateNumber(v: String, min: Double?, max: Double?): String? {
    val d = v.toDoubleOrNull() ?: return null
    if (min != null && d < min) return "Минимум: $min"
    if (max != null && d > max) return "Максимум: $max"
    return null
}
