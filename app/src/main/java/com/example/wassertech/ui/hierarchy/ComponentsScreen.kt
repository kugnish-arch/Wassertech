@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.viewmodel.TemplatesViewModel
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType


@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit, // (пока не используется — оставляем для совместимости)
    onStartMaintenanceAll: (siteId: String, installationName: String) -> Unit,
    onOpenMaintenanceHistoryForInstallation: (String) -> Unit = {},
    vm: HierarchyViewModel = viewModel(),
    templatesVm: TemplatesViewModel = viewModel()
) {
    // 1) Данные
    val installation by vm.installation(installationId).collectAsState(initial = null)
    val components by vm.components(installationId).collectAsState(initial = emptyList())
    val templates by templatesVm.templates.collectAsState()
    val templateTitleById = remember(templates) { templates.associate { it.id to it.title } }

    // 2) UI-состояния
    var isEditing by remember { mutableStateOf(false) }              // режим редактирования (как на экранах Клиенты/Клиент)
    var showEdit by remember { mutableStateOf(false) }               // диалог переименования установки
    var editName by remember { mutableStateOf(TextFieldValue("")) }

    var showAdd by remember { mutableStateOf(false) }                // диалог добавления компонента
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTemplate by remember { mutableStateOf<ChecklistTemplateEntity?>(null) }
    var templateMenu by remember { mutableStateOf(false) }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) } // подтверждение удаления компонента

    Scaffold(
        floatingActionButton = {
            // FAB “Компонент” оставляем доступным в обоих режимах (как у тебя и было)
            ExtendedFloatingActionButton(
                onClick = {
                    showAdd = true
                    newName = TextFieldValue("")
                    selectedTemplate = templates.firstOrNull()
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Компонент") }
            )
        },
        // НИЖНЯЯ ПАНЕЛЬ — как на экране “Клиенты”
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val btnColors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isEditing)
                            Color(0xFF26A69A)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isEditing)
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            // здесь позже можно сохранить новый порядок в БД
                            isEditing = !isEditing
                        },
                        colors = btnColors
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditing) "Готово" else "Изменить")
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(if (isEditing) "Редактирование" else "Просмотр")
                }
            }
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            Modifier
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = 0.dp, // убираем зазор под заголовком «Установка»
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding()
                )
                .fillMaxSize()
        ) {
            // ===== Плашка заголовка (в стиле Клиенты/Клиент) =====
            val instName = installation?.name?.takeIf { it.isNotBlank() } ?: "Без названия"
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SettingsApplications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = instName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    // ✎ показываем ТОЛЬКО в режиме редактирования
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                editName = TextFieldValue(installation?.name ?: "")
                                showEdit = true
                            },
                            enabled = installation != null
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Редактировать установку",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Кнопки действий под заголовком
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val firstComp = components.firstOrNull()
                OutlinedButton(
                    onClick = {
                        val inst = installation
                        if (inst != null && firstComp != null) {
                            onStartMaintenanceAll(inst.siteId, inst.name ?: "")
                        }
                    },
                    enabled = installation != null && firstComp != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Провести ТО") }

                OutlinedButton(
                    onClick = { onOpenMaintenanceHistoryForInstallation(installationId) },
                    modifier = Modifier.weight(1f)
                ) { Text("История ТО") }
            }

            Spacer(Modifier.height(8.dp))

            // ===== Список компонентов =====
            if (components.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет компонентов. Нажмите «Компонент».")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(components, key = { it.id }) { comp ->
                        val tmplTitle = comp.templateId?.let { templateTitleById[it] } ?: "Без шаблона"
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null
                                    )
                                },
                                headlineContent = { Text(comp.name) },
                                supportingContent = { Text(tmplTitle) },
                                trailingContent = {
                                    if (isEditing) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { /* TODO: move up */ }) {
                                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Вверх")
                                            }
                                            IconButton(onClick = { /* TODO: move down */ }) {
                                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Вниз")
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            IconButton(onClick = { pendingDeleteId = comp.id }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = "Удалить компонент"
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ===== Диалог переименования установки =====
    if (showEdit && installation != null) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Редактировать установку") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Название установки") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newTitle = editName.text.trim()
                    if (newTitle.isNotEmpty()) {
                        vm.renameInstallation(installationId, newTitle)
                        // UI обновится через Flow
                    }
                    showEdit = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Отмена") }
            }
        )
    }

    // ===== Диалог добавления компонента =====
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    val tmpl = selectedTemplate
                    val compName = if (newName.text.isNotBlank()) newName.text.trim()
                    else tmpl?.title ?: "Компонент"
                    vm.addComponentFromTemplate(
                        installationId = installationId,
                        name = compName,
                        type = ComponentType.FILTER,      // тип для совместимости
                        templateId = tmpl?.id
                    )
                    showAdd = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } },
            title = { Text("Новый компонент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Название (опц.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = templateMenu,
                        onExpandedChange = { templateMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTemplate?.title
                                ?: if (templates.isEmpty()) "Нет шаблонов" else "Выберите шаблон",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Шаблон") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenu) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = templateMenu,
                            onDismissRequest = { templateMenu = false }
                        ) {
                            if (templates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Шаблонов нет") },
                                    onClick = { templateMenu = false }
                                )
                            } else {
                                templates.forEach { tmpl ->
                                    DropdownMenuItem(
                                        text = { Text(tmpl.title) },
                                        onClick = {
                                            selectedTemplate = tmpl
                                            templateMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // ===== Диалог подтверждения удаления компонента =====
    pendingDeleteId?.let { compId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Удалить компонент?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    // IMPORTANT: поменяй на свой метод, если он называется иначе (например, removeComponent)
                    vm.deleteComponent(compId)
                    pendingDeleteId = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Отмена") }
            }
        )
    }
}
