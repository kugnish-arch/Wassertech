package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.data.types.ComponentType
import androidx.compose.ui.text.input.TextFieldValue
import com.example.wassertech.ui.icons.AppIcons
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(
    installationId: String,
    onStartMaintenance: (String) -> Unit,
    onStartMaintenanceAll: (String) -> Unit = {},
    vm: HierarchyViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    var installationName by remember { mutableStateOf<String?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(installationId) {
        val inst = vm.getInstallation(installationId)
        installationName = inst?.name
        editName = TextFieldValue(inst?.name ?: "")
    }

    val components by vm.components(installationId).collectAsState(initial = emptyList())

    var reorderMode by remember { mutableStateOf(false) }
    var localOrder by remember(installationId) { mutableStateOf(components.map { it.id }) }
    LaunchedEffect(components) { if (!reorderMode) localOrder = components.map { it.id } }
    val orderedComponents = remember(localOrder, components) { localOrder.mapNotNull { id -> components.find { it.id == id } } }

    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var typeIdx by remember { mutableStateOf(0) }
    val types = ComponentType.values()

    Scaffold(
        floatingActionButton = { if (!reorderMode) { ExtendedFloatingActionButton(onClick = { showAdd = true }) { Text("+ Компонент") } } }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = AppIcons.Installation, contentDescription = null)
                        Text(installationName ?: "Установка", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, contentDescription = null) }
                    }
                    Text("Состав: ${components.size} компонент(ов)", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onStartMaintenanceAll(installationId) }) { Text("Провести ТО") }
                        OutlinedButton(onClick = { 
                            reorderMode = !reorderMode
                            if (!reorderMode) localOrder = components.map { it.id }
                        }) { Text(if (reorderMode) "Отмена сортировки" else "Изменить порядок") }
                        if (reorderMode) {
                            Button(onClick = { vm.reorderComponents(installationId, localOrder); reorderMode = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        }
                    }
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                if (reorderMode) {
                    itemsIndexed(orderedComponents, key = { _, it -> it.id }) { index, c ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${index + 1}. ${c.name} • ${c.type}", style = MaterialTheme.typography.titleMedium)
                                Row {
                                    IconButton(onClick = {
                                        val pos = localOrder.indexOf(c.id)
                                        if (pos > 0) {
                                            val newList = localOrder.toMutableList()
                                            val tmp = newList[pos-1]; newList[pos-1] = newList[pos]; newList[pos] = tmp
                                            localOrder = newList
                                        }
                                    }, enabled = index > 0) { Icon(Icons.Filled.ArrowUpward, contentDescription = null) }
                                    IconButton(onClick = {
                                        val pos = localOrder.indexOf(c.id)
                                        if (pos >= 0 && pos < orderedComponents.lastIndex) {
                                            val newList = localOrder.toMutableList()
                                            val tmp = newList[pos+1]; newList[pos+1] = newList[pos]; newList[pos] = tmp
                                            localOrder = newList
                                        }
                                    }, enabled = index < orderedComponents.lastIndex) { Icon(Icons.Filled.ArrowDownward, contentDescription = null) }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(components, key = { _, it -> it.id }) { index, c ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp)) {
                                Icon(imageVector = AppIcons.Component, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(text = "${index + 1}. ${c.name} • ${c.type}", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Редактировать установку") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Название установки") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val inst = vm.getInstallation(installationId)
                        if (inst != null) {
                            vm.editInstallation(inst.copy(name = editName.text.trim()))
                            installationName = editName.text.trim()
                        }
                        showEdit = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Отмена") } }
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новый компонент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = types[typeIdx].name, onValueChange = {}, readOnly = true, label = { Text("Тип") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = androidx.compose.ui.Modifier.menuAnchor())
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
                        name = TextFieldValue(""); typeIdx = 0; showAdd = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}
