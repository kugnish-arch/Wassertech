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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.viewmodel.TemplatesViewModel
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.ui.common.EditDoneBottomBar
import com.example.wassertech.viewmodel.ClientsViewModel
import com.example.wassertech.viewmodel.ClientsViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit, // пока не используем для одиночного компонента
    onStartMaintenanceAll: (siteId: String, installationName: String) -> Unit,
    onOpenMaintenanceHistoryForInstallation: (String) -> Unit = {},   // навигация в историю ТО
    vm: HierarchyViewModel = viewModel(),
    templatesVm: TemplatesViewModel = viewModel() // оставлено для совместимости
) {
    // --- Templates из БД без VM ---
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val allTemplatesFlow: Flow<List<ChecklistTemplateEntity>> =
        remember { db.templatesDao().observeAllTemplates() }
    val allTemplates by allTemplatesFlow.collectAsState(initial = emptyList())
    val templateTitleById = remember(allTemplates) { allTemplates.associate { it.id to it.title } }

    // --- Второй VM: клиенты (чтобы получить имя клиента без observeClient()) ---
    val clientsVm: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(db.clientDao(), db))
    val allClients by clientsVm.clients.collectAsState()

    // --- Данные установки / компонентов ---
    val installation by vm.installation(installationId).collectAsState(initial = null)
    val components by vm.components(installationId).collectAsState(initial = emptyList())

    // Подтянем сайт (нужен clientId). Если у тебя другой метод — поправь здесь.
    val siteFlow = remember(installation?.siteId) {
        installation?.siteId?.let { vm.site(it) } ?: flowOf(null)
    }
    val site by siteFlow.collectAsState(initial = null)

    // Имя клиента берём из clientsVm.clients
    val clientName: String? = remember(site, allClients) {
        val id = site?.clientId ?: return@remember null
        allClients.firstOrNull { it.id == id }?.name
    }

    // --- Локальные UI-состояния ---
    var isEditing by remember { mutableStateOf(false) }

    // локальный порядок (живой) — отрисовываем список по нему
    var localOrder by remember(components) { mutableStateOf(components.map { it.id } ) }

    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }

    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTemplate by remember { mutableStateOf<ChecklistTemplateEntity?>(null) }
    var templateMenu by remember { mutableStateOf(false) }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    showAdd = true
                    newName = TextFieldValue("")
                    selectedTemplate = allTemplates.firstOrNull()
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Компонент") }
            )
        },
        bottomBar = {
            EditDoneBottomBar(
                isEditing = isEditing,
                onEdit = {
                    isEditing = true
                    localOrder = components.map { it.id } // фиксируем актуальный порядок в момент входа в редактирование
                },
                onDone = {
                    // сохраняем новый порядок компонентов в БД
                    vm.reorderComponents(installationId, localOrder)
                    isEditing = false
                }
            )
        }
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current

        Column(
            Modifier
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = 0.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding()
                )
                .fillMaxSize()
        ) {
            // ===== Заголовок установки =====
            val instName = installation?.name?.takeIf { it.isNotBlank() } ?: "Без названия"
            val metaText: String? = when {
                clientName != null && site?.name != null ->
                    "Объект: $clientName — ${site!!.name}"
                site?.name != null ->
                    "Объект: ${site!!.name}"
                else -> null
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                    metaText?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // ===== Кнопки действий под заголовком =====
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

            // ===== Список компонентов (отрисовываем в порядке localOrder) =====
            val componentsById = remember(components) { components.associateBy { it.id } }
            val orderedComponents = remember(localOrder, componentsById) {
                localOrder.mapNotNull { componentsById[it] }
            }

            if (orderedComponents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет компонентов. Нажмите «Компонент».")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orderedComponents, key = { it.id }) { comp ->
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
                                            IconButton(
                                                onClick = {
                                                    val i = localOrder.indexOf(comp.id)
                                                    if (i > 0) {
                                                        val m = localOrder.toMutableList()
                                                        m[i - 1] = m[i].also { _ -> m[i] = m[i - 1] }
                                                        localOrder = m
                                                    }
                                                }
                                            ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Вверх") }

                                            IconButton(
                                                onClick = {
                                                    val i = localOrder.indexOf(comp.id)
                                                    if (i != -1 && i < localOrder.lastIndex) {
                                                        val m = localOrder.toMutableList()
                                                        m[i + 1] = m[i].also { _ -> m[i] = m[i + 1] }
                                                        localOrder = m
                                                    }
                                                }
                                            ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Вниз") }

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
                        type = ComponentType.FILTER, // подставь нужный тип при необходимости
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
                        val selectedTitle = selectedTemplate?.title
                            ?: if (allTemplates.isEmpty()) "Нет шаблонов" else "Выберите шаблон"
                        OutlinedTextField(
                            value = selectedTitle,
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
                            if (allTemplates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Шаблонов нет") },
                                    onClick = { templateMenu = false }
                                )
                            } else {
                                allTemplates.forEach { tmpl ->
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
