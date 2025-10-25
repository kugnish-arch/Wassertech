package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.data.entities.ComponentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val list by vm.components(installationId).collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var typeIdx by remember { mutableStateOf(0) }
    val types = ComponentType.values()

    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { c ->
                    ElevatedCard {
                        Column(Modifier.padding(12.dp)) {
                            Text("${c.name} • ${c.type}", style = MaterialTheme.typography.titleMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onStartMaintenance(c.id) }) { Text("Провести ТО") }
                                TextButton(onClick = { vm.deleteComponent(c.id) }) { Text("Удалить") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новый компонент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                    // simple dropdown imitation
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = types[typeIdx].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Тип") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = androidx.compose.ui.Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            types.forEachIndexed { index, t ->
                                DropdownMenuItem(text = { Text(t.name) }, onClick = { typeIdx = index; expanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addComponent(installationId, n, types[typeIdx])
                        name = TextFieldValue(""); typeIdx = 0
                        showAdd = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}
