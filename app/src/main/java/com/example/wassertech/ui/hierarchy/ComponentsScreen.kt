
package com.example.wassertech.ui.hierarchy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.data.entities.ComponentEntity
import com.example.wassertech.data.types.ComponentType
import com.example.wassertech.ui.icons.AppIcons
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(
    installationId: String,
    onBack: () -> Unit = {}, // not used (back button removed)
    onStartMaintenance: (String) -> Unit = {},
    onStartMaintenanceAll: (String) -> Unit = {},
    onOpenComponent: (String) -> Unit = {},
    vm: HierarchyViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    var installationName by remember { mutableStateOf("Установка") }

    // Edit installation
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }
    var clientIdForSites by remember { mutableStateOf<String?>(null) }
    var selectedSiteId by remember { mutableStateOf<String?>(null) }
    var selectedSiteIndex by remember { mutableStateOf(0) }
    var sitePickerExpanded by remember { mutableStateOf(false) }

    // Add component dialog
    var showAddComponent by remember { mutableStateOf(false) }
    var newComponentName by remember { mutableStateOf(TextFieldValue("")) }
    var typePickerExpanded by remember { mutableStateOf(false) }
    var newComponentType by remember { mutableStateOf(ComponentType.FILTER) }
    var selectedTypeIndex by remember { mutableStateOf(0) }

    // Edit component dialog
    var showEditComponent by remember { mutableStateOf(false) }
    var editComponentModel by remember { mutableStateOf<ComponentEntity?>(null) }
    var editComponentName by remember { mutableStateOf(TextFieldValue("")) }
    var editComponentType by remember { mutableStateOf(ComponentType.FILTER) }
    var editTypePickerExpanded by remember { mutableStateOf(false) }
    var editSelectedTypeIndex by remember { mutableStateOf(0) }

    // Reorder components
    var reorderMode by remember { mutableStateOf(false) }
    var localOrder by remember(installationId) { mutableStateOf(listOf<String>()) }

    LaunchedEffect(installationId) {
        val inst = vm.getInstallation(installationId)
        if (inst != null) {
            installationName = inst.name
            editName = TextFieldValue(inst.name)
            val currentSite = vm.getSite(inst.siteId)
            clientIdForSites = currentSite?.clientId
            selectedSiteId = currentSite?.id
        }
    }

    val components by vm.components(installationId).collectAsState(initial = emptyList())
    LaunchedEffect(components) {
        if (!reorderMode) localOrder = components.map { it.id }
    }

    val sitesForClient by remember(clientIdForSites) {
        if (clientIdForSites != null) vm.sites(clientIdForSites!!) else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    // keep selectedSiteIndex in sync with selectedSiteId + sites list
    LaunchedEffect(sitesForClient, selectedSiteId) {
        val idx = sitesForClient.indexOfFirst { it.id == selectedSiteId }
        selectedSiteIndex = if (idx >= 0) idx else 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(installationName) },
                actions = {
                    IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, contentDescription = "Редактировать") }
                }
            )
        },
        floatingActionButton = {
            if (!reorderMode) {
                ExtendedFloatingActionButton(onClick = { showAddComponent = true }) { Text("+ Компонент") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(onClick = { onStartMaintenanceAll(installationId) }) {
                    Text("Провести ТО")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    reorderMode = !reorderMode
                    if (!reorderMode) localOrder = components.map { it.id }
                }) { Text(if (reorderMode) "Отмена сортировки" else "Изменить порядок") }
                if (reorderMode) {
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            vm.reorderComponents(installationId, localOrder)
                            reorderMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) { Text("✔") }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (reorderMode) {
                    items(components, key = { it.id }) { c ->
                        val index = localOrder.indexOf(c.id)
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${index + 1}. ${c.name}", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            val pos = localOrder.indexOf(c.id)
                                            if (pos > 0) {
                                                val list = localOrder.toMutableList()
                                                val tmp = list[pos - 1]; list[pos - 1] = list[pos]; list[pos] = tmp
                                                localOrder = list
                                            }
                                        },
                                        enabled = index > 0
                                    ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Вверх") }
                                    FilledTonalIconButton(
                                        onClick = {
                                            val pos = localOrder.indexOf(c.id)
                                            if (pos >= 0 && pos < localOrder.lastIndex) {
                                                val list = localOrder.toMutableList()
                                                val tmp = list[pos + 1]; list[pos + 1] = list[pos]; list[pos] = tmp
                                                localOrder = list
                                            }
                                        },
                                        enabled = index < localOrder.lastIndex
                                    ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Вниз") }
                                }
                            }
                        }
                    }
                } else {
                    items(components, key = { it.id }) { c: ComponentEntity ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editComponentModel = c
                                    editComponentName = TextFieldValue(c.name)
                                    editComponentType = c.type
                                    editSelectedTypeIndex = ComponentType.values().indexOf(c.type).coerceAtLeast(0)
                                    showEditComponent = true
                                    onOpenComponent(c.id)
                                }
                        ) {
                            Row(Modifier.padding(12.dp)) {
                                Icon(imageVector = AppIcons.Component, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(c.name, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Edit Installation Dialog ----
    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Редактировать установку") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Название установки") }
                    )
                    // Dropdown identical approach to Clients "+ Установка"
                    ExposedDropdownMenuBox(
                        expanded = sitePickerExpanded,
                        onExpandedChange = { sitePickerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = sitesForClient.getOrNull(selectedSiteIndex)?.name ?: "Выберите объект",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Объект") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sitePickerExpanded) },
                            modifier = androidx.compose.ui.Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = sitePickerExpanded,
                            onDismissRequest = { sitePickerExpanded = false }
                        ) {
                            sitesForClient.forEachIndexed { index, s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        selectedSiteIndex = index
                                        selectedSiteId = s.id
                                        sitePickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = editName.text.trim()
                    if (newName.isNotEmpty()) {
                        val chosenSiteId = sitesForClient.getOrNull(selectedSiteIndex)?.id ?: selectedSiteId
                        scope.launch {
                            val inst = vm.getInstallation(installationId)
                            if (inst != null) {
                                vm.editInstallation(
                                    inst.copy(
                                        name = newName,
                                        siteId = chosenSiteId ?: inst.siteId
                                    )
                                )
                            }
                            showEdit = false
                        }
                    } else showEdit = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Отмена") } }
        )
    }

    // ---- Add Component Dialog ----
    if (showAddComponent) {
        AlertDialog(
            onDismissRequest = { showAddComponent = false },
            title = { Text("Добавить компонент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newComponentName,
                        onValueChange = { newComponentName = it },
                        label = { Text("Название") }
                    )
                    ExposedDropdownMenuBox(
                        expanded = typePickerExpanded,
                        onExpandedChange = { typePickerExpanded = it }
                    ) {
                        val types = ComponentType.values()
                        OutlinedTextField(
                            value = types.getOrNull(selectedTypeIndex)?.name ?: newComponentType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Тип") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typePickerExpanded) },
                            modifier = androidx.compose.ui.Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typePickerExpanded,
                            onDismissRequest = { typePickerExpanded = false }
                        ) {
                            types.forEachIndexed { index, t ->
                                DropdownMenuItem(
                                    text = { Text(t.name) },
                                    onClick = {
                                        selectedTypeIndex = index
                                        newComponentType = t
                                        typePickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newComponentName.text.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            vm.addComponent(installationId, name, newComponentType)
                            newComponentName = TextFieldValue("")
                            showAddComponent = false
                        }
                    } else showAddComponent = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddComponent = false }) { Text("Отмена") } }
        )
    }

    // ---- Edit Component Dialog ----
    if (showEditComponent && editComponentModel != null) {
        AlertDialog(
            onDismissRequest = { showEditComponent = false },
            title = { Text("Редактировать компонент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editComponentName,
                        onValueChange = { editComponentName = it },
                        label = { Text("Название") }
                    )
                    ExposedDropdownMenuBox(
                        expanded = editTypePickerExpanded,
                        onExpandedChange = { editTypePickerExpanded = it }
                    ) {
                        val types = ComponentType.values()
                        OutlinedTextField(
                            value = types.getOrNull(editSelectedTypeIndex)?.name ?: editComponentType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Тип") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editTypePickerExpanded) },
                            modifier = androidx.compose.ui.Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = editTypePickerExpanded,
                            onDismissRequest = { editTypePickerExpanded = false }
                        ) {
                            types.forEachIndexed { index, t ->
                                DropdownMenuItem(
                                    text = { Text(t.name) },
                                    onClick = {
                                        editSelectedTypeIndex = index
                                        editComponentType = t
                                        editTypePickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val base = editComponentModel
                    val name = editComponentName.text.trim()
                    if (base != null && name.isNotEmpty()) {
                        scope.launch {
                            vm.editComponent(base.copy(name = name, type = editComponentType))
                            showEditComponent = false
                        }
                    } else showEditComponent = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEditComponent = false }) { Text("Отмена") } }
        )
    }
}
