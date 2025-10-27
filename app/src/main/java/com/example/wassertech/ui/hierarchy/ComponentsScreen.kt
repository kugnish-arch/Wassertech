
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.viewmodel.TemplatesViewModel
import com.example.wassertech.data.entities.ChecklistTemplateEntity

@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit,
    onStartMaintenanceAll: () -> Unit,
    vm: HierarchyViewModel = viewModel(),
    templatesVm: TemplatesViewModel = viewModel()
) {
    // ---- Header: Installation title + edit ----
    var installation by remember { mutableStateOf<com.example.wassertech.data.entities.InstallationEntity?>(null) }
    LaunchedEffect(installationId) {
        installation = vm.getInstallation(installationId)
    }

    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue(installation?.name.orEmpty())) }

    if (installation != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (installation!!.name.isBlank()) "Без названия" else installation!!.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                editName = TextFieldValue(installation!!.name)
                showEdit = true
            }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Редактировать установку")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onStartMaintenanceAll,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) { Text("Провести ТО") }
    }

    if (showEdit) {
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
                    val newName = editName.text.trim()
                    if (newName.isNotEmpty() && installation != null) {
                        vm.renameInstallation(installationId, newName)
                        installation = installation!!.copy(name = newName)
                        showEdit = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Отмена") } }
        )
    }

    val components by vm.components(installationId).collectAsState(initial = emptyList())
    val templates by templatesVm.templates.collectAsState()

    // Быстрый индекс id -> title
    val templateTitleById = remember(templates) {
        templates.associate { it.id to it.title }
    }

    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTemplate by remember { mutableStateOf<ChecklistTemplateEntity?>(null) }
    var templateMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true; newName = TextFieldValue(""); selectedTemplate = templates.firstOrNull() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Компонент") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // ---- История ТО ----
            val context = LocalContext.current
            val db = remember { com.example.wassertech.data.AppDatabase.getInstance(context) }
            val sessionsFlow = remember(installationId) { db.sessionsDao().observeSessionsByInstallation(installationId) }
            val sessions by sessionsFlow.collectAsState(initial = emptyList())
            var showSessionId by remember { mutableStateOf<String?>(null) }
            var sessionDetails by remember { mutableStateOf<List<com.example.wassertech.ui.maintenance.ObservationDetail>>(emptyList()) }

            if (sessions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "История ТО",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val subset: List<com.example.wassertech.data.entities.MaintenanceSessionEntity> = sessions.take(5)
                    itemsIndexed(subset, key = { _, item -> item.id }) { _, s ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(s.startedAtEpoch))) },
                                supportingContent = { Text(s.notes ?: "") },
                                modifier = Modifier.clickable { showSessionId = s.id }
                            )
                        }
                    }
                }
            }

            if (showSessionId != null) {
                val sid = showSessionId!!
                LaunchedEffect(sid) {
                    val obs = db.sessionsDao().getObservations(sid)
                    val details = mutableListOf<com.example.wassertech.ui.maintenance.ObservationDetail>()
                    for (o in obs) {
                        val comp = db.hierarchyDao().getComponent(o.componentId)
                        val componentName = comp?.name ?: o.componentId
                        val value = when {
                            o.valueText != null -> o.valueText
                            o.valueNumber != null -> o.valueNumber.toString()
                            o.valueBool != null -> if (o.valueBool) "Да" else "Нет"
                            else -> ""
                        }
                        details.add(com.example.wassertech.ui.maintenance.ObservationDetail(o.componentId, componentName, o.fieldKey, value))
                    }
                    sessionDetails = details
                }
                AlertDialog(
                    onDismissRequest = { showSessionId = null },
                    confirmButton = { TextButton(onClick = { showSessionId = null }) { Text("Закрыть") } },
                    title = { Text("Детали ТО") },
                    text = {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sessionDetails.forEach { d ->
                                Text("• ${'$'}{d.componentName}: ${'$'}{d.fieldKey} — ${'$'}{d.valueText}")
                            }
                        }
                    }
                )
            }

            // ---- Список компонентов ----
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
                                        IconButton(onClick = { /* up */ }) { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
                                        IconButton(onClick = { /* down */ }) { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    val tmpl = selectedTemplate
                    val compName = if (newName.text.isNotBlank()) newName.text.trim() else tmpl?.title ?: "Компонент"
                    // тип больше не показываем и не используем в UI; колонка в Entity может остаться для совместимости
                    vm.addComponentFromTemplate(installationId, compName, /* type ignored */ com.example.wassertech.data.types.ComponentType.FILTER, tmpl?.id)
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
                    ExposedDropdownMenuBox(expanded = templateMenu, onExpandedChange = { templateMenu = it }) {
                        OutlinedTextField(
                            value = selectedTemplate?.title ?: "Нет шаблонов",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Шаблон") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = templateMenu, onDismissRequest = { templateMenu = false }) {
                            if (templates.isEmpty()) {
                                DropdownMenuItem(text = { Text("Шаблонов нет") }, onClick = { templateMenu = false })
                            } else {
                                templates.forEach { tmpl ->
                                    DropdownMenuItem(
                                        text = { Text(tmpl.title) },
                                        onClick = { selectedTemplate = tmpl; templateMenu = false }
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
