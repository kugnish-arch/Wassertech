package com.example.wassertech.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.ui.icons.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    onOpenSite: (String) -> Unit,
    onOpenInstallation: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    // sites for list & selector
    val sites by vm.sites(clientId).collectAsState(initial = emptyList())

    // header state
    var clientName by remember { mutableStateOf("Клиент") }
    var isCorporate by remember { mutableStateOf(false) }

    // edit client
    var showEditClient by remember { mutableStateOf(false) }
    var editClientName by remember { mutableStateOf(TextFieldValue("")) }
    var editClientNotes by remember { mutableStateOf(TextFieldValue("")) }
    var editClientCorporate by remember { mutableStateOf(false) }

    // FAB dialogs
    var showAddSite by remember { mutableStateOf(false) }
    var showAddInstallation by remember { mutableStateOf(false) }

    // add site
    var addSiteName by remember { mutableStateOf(TextFieldValue("")) }
    var addSiteAddr by remember { mutableStateOf(TextFieldValue("")) }

    // add installation
    var addInstallationName by remember { mutableStateOf(TextFieldValue("")) }
    var sitePickerExpanded by remember { mutableStateOf(false) }
    var selectedSiteIndex by remember { mutableStateOf(0) }

    // expand/collapse per site
    var expandedSites by remember { mutableStateOf(setOf<String>()) }

    // reorder mode for sites
    var reorderMode by remember { mutableStateOf(false) }
    var localOrder by remember(clientId) { mutableStateOf(sites.map { it.id }) }
    LaunchedEffect(sites) { if (!reorderMode) localOrder = sites.map { it.id } }

    LaunchedEffect(clientId) {
        val c = vm.getClient(clientId)
        if (c != null) {
            clientName = c.name
            isCorporate = c.isCorporate
            editClientName = TextFieldValue(c.name)
            editClientNotes = TextFieldValue(c.notes ?: "")
            editClientCorporate = c.isCorporate
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!reorderMode) {
                Row(modifier = Modifier.padding(end = 8.dp)) {
                    ExtendedFloatingActionButton(onClick = { showAddInstallation = true }) { Text("+ Установка") }
                    Spacer(Modifier.width(8.dp))
                    ExtendedFloatingActionButton(onClick = { showAddSite = true }) { Text("+ Объект") }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Icon(
                            imageVector = if (isCorporate) AppIcons.ClientCorporate else AppIcons.ClientPrivate,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(clientName, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                    }
                    // edit under the name
                    TextButton(onClick = { showEditClient = true }) { Text("Редактировать") }
                }
            }

            // Reorder toolbar
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                OutlinedButton(onClick = {
                    reorderMode = !reorderMode
                    if (!reorderMode) localOrder = sites.map { it.id }
                }) { Text(if (reorderMode) "Отмена сортировки" else "Изменить порядок") }

                if (reorderMode) {
                    Button(
                        onClick = { vm.reorderSites(clientId, localOrder); reorderMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // зелёная "Сохранить"
                    ) {
                        Text("Сохранить")
                    }
                }
            }

            // Expandable list of Sites -> Installations
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (reorderMode) {
                    items(sites, key = { it.id }) { s ->
                        val index = localOrder.indexOf(s.id)
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${index + 1}. ${s.name}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    // Крупные стрелки
                                    IconButton(
                                        onClick = {
                                            val pos = localOrder.indexOf(s.id)
                                            if (pos > 0) {
                                                val list = localOrder.toMutableList()
                                                val tmp = list[pos - 1]; list[pos - 1] = list[pos]; list[pos] = tmp
                                                localOrder = list
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowUp,
                                            contentDescription = "Вверх",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val pos = localOrder.indexOf(s.id)
                                            if (pos >= 0 && pos < localOrder.lastIndex) {
                                                val list = localOrder.toMutableList()
                                                val tmp = list[pos + 1]; list[pos + 1] = list[pos]; list[pos] = tmp
                                                localOrder = list
                                            }
                                        },
                                        enabled = index < localOrder.lastIndex
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Вниз",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(sites, key = { it.id }) { s ->
                        val isExpanded = expandedSites.contains(s.id)
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column {
                                ListItem(
                                    modifier = Modifier.clickable { onOpenSite(s.id) },
                                    leadingContent = { Icon(imageVector = AppIcons.Site, contentDescription = null) },
                                    headlineContent = { Text(s.name) },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            expandedSites = if (isExpanded) expandedSites - s.id else expandedSites + s.id
                                        }) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                )
                                if (isExpanded) {
                                    val installations by vm.installations(s.id).collectAsState(initial = emptyList())
                                    Column(
                                        Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (installations.isEmpty()) {
                                            Text("Нет установок", style = MaterialTheme.typography.bodyMedium)
                                        } else {
                                            installations.forEach { inst ->
                                                ElevatedCard(
                                                    onClick = { onOpenInstallation(inst.id) },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(Modifier.padding(12.dp)) {
                                                        Icon(imageVector = AppIcons.Installation, contentDescription = null)
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(inst.name, style = MaterialTheme.typography.titleMedium)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Dialogs ----

    if (showAddSite) {
        AlertDialog(
            onDismissRequest = { showAddSite = false },
            title = { Text("Добавить объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = addSiteName, onValueChange = { addSiteName = it }, label = { Text("Название объекта") })
                    OutlinedTextField(value = addSiteAddr, onValueChange = { addSiteAddr = it }, label = { Text("Адрес (опц.)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = addSiteName.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addSite(clientId, n, addSiteAddr.text.trim().ifEmpty { null })
                        addSiteName = TextFieldValue("")
                        addSiteAddr = TextFieldValue("")
                        showAddSite = false
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddSite = false }) { Text("Отмена") } }
        )
    }

    if (showAddInstallation) {
        AlertDialog(
            onDismissRequest = { showAddInstallation = false },
            title = { Text("Добавить установку") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = addInstallationName, onValueChange = { addInstallationName = it }, label = { Text("Название установки") })
                    ExposedDropdownMenuBox(expanded = sitePickerExpanded, onExpandedChange = { sitePickerExpanded = it }) {
                        OutlinedTextField(
                            value = sites.getOrNull(selectedSiteIndex)?.name ?: "Выберите объект",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Объект") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sitePickerExpanded) },
                            modifier = androidx.compose.ui.Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = sitePickerExpanded, onDismissRequest = { sitePickerExpanded = false }) {
                            sites.forEachIndexed { index, s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = {
                                    selectedSiteIndex = index
                                    sitePickerExpanded = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = addInstallationName.text.trim()
                    if (n.isNotEmpty()) {
                        val selectedSiteId = sites.getOrNull(selectedSiteIndex)?.id
                        if (selectedSiteId != null) vm.addInstallationToSite(selectedSiteId, n)
                        else vm.addInstallationToMain(clientId, n)
                        addInstallationName = TextFieldValue("")
                        showAddInstallation = false
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddInstallation = false }) { Text("Отмена") } }
        )
    }

    if (showEditClient) {
        AlertDialog(
            onDismissRequest = { showEditClient = false },
            title = { Text("Редактировать клиента") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editClientName, onValueChange = { editClientName = it }, label = { Text("Имя/название") })
                    OutlinedTextField(value = editClientNotes, onValueChange = { editClientNotes = it }, label = { Text("Адрес/заметки") })
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = editClientCorporate, onCheckedChange = { editClientCorporate = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Корпоративный клиент")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val c = vm.getClient(clientId)
                        if (c != null) {
                            vm.editClient(
                                c.copy(
                                    name = editClientName.text.trim(),
                                    notes = editClientNotes.text.trim().ifEmpty { null },
                                    isCorporate = editClientCorporate
                                )
                            )
                            clientName = editClientName.text.trim()
                            isCorporate = editClientCorporate
                        }
                        showEditClient = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEditClient = false }) { Text("Отмена") } }
        )
    }
}
