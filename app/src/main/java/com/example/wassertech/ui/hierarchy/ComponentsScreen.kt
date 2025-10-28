@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.viewmodel.TemplatesViewModel
import com.example.wassertech.data.entities.ChecklistTemplateEntity

@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit, // (пока не используется — оставляем для совместимости)
    onStartMaintenanceAll: () -> Unit,
    onOpenMaintenanceHistoryForInstallation: (String) -> Unit = {}, // NEW: дефолт — ничего не делает
    vm: HierarchyViewModel = viewModel(),
    templatesVm: TemplatesViewModel = viewModel()
) {
    // --- Заголовок: название установки + редактирование ---
    var installation by remember { mutableStateOf<com.example.wassertech.data.entities.InstallationEntity?>(null) }
    LaunchedEffect(installationId) {
        installation = vm.getInstallation(installationId)
    }

    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }

    // --- Данные списков ---
    val components by vm.components(installationId).collectAsState(initial = emptyList())
    val templates by templatesVm.templates.collectAsState() // список шаблонов (для выпадашки)

    // Быстрый индекс id -> title
    val templateTitleById = remember(templates) {
        templates.associate { it.id to it.title }
    }

    // --- Состояния диалога добавления компонента ---
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTemplate by remember { mutableStateOf<ChecklistTemplateEntity?>(null) }
    var templateMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    showAdd = true
                    newName = TextFieldValue("")
                    selectedTemplate = templates.firstOrNull()
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Компонент") }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // ----- Шапка экрана установки -----
            if (installation != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (installation!!.name.isBlank()) "Без названия" else installation!!.name,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            editName = TextFieldValue(installation!!.name)
                            showEdit = true
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать установку")
                    }
                }

                // Кнопки действий под заголовком
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onStartMaintenanceAll,
                        modifier = Modifier.weight(1f)
                    ) { Text("Провести ТО") }

                    OutlinedButton(
                        onClick = { onOpenMaintenanceHistoryForInstallation(installationId) },
                        modifier = Modifier.weight(1f)
                    ) { Text("История ТО") }
                }

                Spacer(Modifier.height(8.dp))
            }

            // ----- Список компонентов -----
            if (components.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет компонентов. Нажмите «Компонент».")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(components, key = { _, it -> it.id }) { _, item ->
                        val tmplTitle = item.templateId?.let { templateTitleById[it] } ?: "Без шаблона"
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text(tmplTitle) },
                                trailingContent = {
                                    Row {
                                        IconButton(onClick = { /* TODO: move up */ }) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх")
                                        }
                                        IconButton(onClick = { /* TODO: move down */ }) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз")
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

    // ----- Диалог редактирования названия установки -----
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
                        installation = installation!!.copy(name = newTitle)
                    }
                    showEdit = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEdit = false }) { Text("Отмена") }
            }
        )
    }

    // ----- Диалог добавления компонента -----
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    val tmpl = selectedTemplate
                    val compName = if (newName.text.isNotBlank()) newName.text.trim()
                    else tmpl?.title ?: "Компонент"

                    // тип в UI не используется; колонка в Entity может оставаться для совместимости
                    vm.addComponentFromTemplate(
                        installationId = installationId,
                        name = compName,
                        type = com.example.wassertech.data.types.ComponentType.FILTER, // игнорим тип в логике
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
                        singleLine = true
                    )
                    ExposedDropdownMenuBox(
                        expanded = templateMenu,
                        onExpandedChange = { templateMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTemplate?.title ?: if (templates.isEmpty()) "Нет шаблонов" else "Выберите шаблон",
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
}
