@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.wassertech.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.data.entities.ChecklistTemplateEntity
import com.example.wassertech.viewmodel.TemplatesViewModel

@Composable
fun TemplatesScreen(
    onOpenTemplate: (String) -> Unit = {},
    vm: TemplatesViewModel = viewModel()
) {
    val templates by vm.templates.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<com.example.wassertech.data.types.ComponentType?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Шаблоны компонентов") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Шаблон") }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (templates.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Шаблонов пока нет")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates, key = { it.id }) { t ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(t.title) },
                                supportingContent = { Text(t.componentType.name) },
                                modifier = Modifier.clickable { onOpenTemplate(t.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var menu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank() && type != null) {
                        vm.createTemplateSimple(title.trim(), type!!)
                        showAdd = false
                        title = ""
                        type = null
                    }
                }) { Text("Создать") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } },
            title = { Text("Новый шаблон") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true)
                    ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                        OutlinedTextField(
                            value = type?.name ?: "Выберите тип",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Тип компонента") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            com.example.wassertech.data.types.ComponentType.values().forEach { ct ->
                                DropdownMenuItem(text = { Text(ct.name) }, onClick = { type = ct; menu = false })
                            }
                        }
                    }
                }
            }
        )
    }
}
