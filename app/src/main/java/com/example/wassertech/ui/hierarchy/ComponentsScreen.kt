@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
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
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.data.types.ComponentType

@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit,
    onStartMaintenanceAll: () -> Unit,
    vm: HierarchyViewModel = viewModel(),
    templatesVm: TemplatesViewModel = viewModel()
) {
    val components by vm.components(installationId).collectAsState(initial = emptyList())
    val templates by templatesVm.templates.collectAsState()

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
            if (components.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Нет компонентов. Нажмите «Компонент»." )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(components, key = { _, it -> it.id }) { _, item ->
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
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    val tmpl = selectedTemplate
                    val compName = if (newName.text.isNotBlank()) newName.text.trim()
                                   else tmpl?.title ?: "Компонент"
                    val ctype: ComponentType = ComponentType.FILTER
                    vm.addComponentFromTemplate(installationId, compName, ctype, tmpl?.id)
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
