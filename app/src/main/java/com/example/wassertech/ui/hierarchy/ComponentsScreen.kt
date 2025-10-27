@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.viewmodel.TemplatesViewModel
import com.example.wassertech.data.entities.ComponentTemplateEntity
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.ui.util.TemplateTypeMapper
import kotlinx.coroutines.launch

@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit,
    onStartMaintenanceAll: () -> Unit,
    vm: HierarchyViewModel = viewModel(),
    templatesVm: TemplatesViewModel = viewModel()
) {
    val components by vm.components(installationId).collectAsState(initial = emptyList())
    val templates by templatesVm.templates.collectAsState(initial = emptyList())

    // Header state: installation name + edit dialog
    val scope = rememberCoroutineScope()
    var installName by remember { mutableStateOf("Установка") }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(installationId) {
        val inst = vm.getInstallation(installationId)
        if (inst != null) {
            installName = inst.name
            editName = TextFieldValue(inst.name)
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTemplate by remember { mutableStateOf<ComponentTemplateEntity?>(null) }

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

            // Header with name + edit
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp)) {
                    Text(installName, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать установку")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (components.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Нет компонентов. Нажмите кнопку «Компонент»." )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(components, key = { _, it -> it.id }) { index, item ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(item.name) },
                                supportingContent = { Text(item.type.name) },
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
        var templateMenu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    val tmpl = selectedTemplate
                    val compName = if (newName.text.isNotBlank()) newName.text.trim()
                                   else tmpl?.name ?: "Компонент"
                    val ctype: ComponentType = TemplateTypeMapper.map(tmpl?.category, tmpl?.name ?: compName)

                    vm.addComponent(installationId, compName, ctype)
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
                    // Templates dropdown
                    ExposedDropdownMenuBox(expanded = templateMenu, onExpandedChange = { templateMenu = it }) {
                        OutlinedTextField(
                            value = selectedTemplate?.let { it.name + (it.category?.let { c -> " • $c" } ?: "") } ?: "Нет шаблонов",
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
                                templates.filter { !it.isArchived }.forEach { tmpl ->
                                    DropdownMenuItem(
                                        text = { Text(tmpl.name + (tmpl.category?.let { " • " + it } ?: "")) },
                                        onClick = { selectedTemplate = tmpl; templateMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    if (templates.isEmpty()) {
                        Text("Создайте шаблон в меню «Шаблоны», чтобы выбирать тип компонента.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        )
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Переименовать установку") },
            text = {
                OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Название установки") })
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val inst = vm.getInstallation(installationId) ?: return@launch
                        vm.editInstallation(inst.copy(name = editName.text.trim()))
                        installName = editName.text.trim()
                        showEdit = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Отмена") } }
        )
    }
}
