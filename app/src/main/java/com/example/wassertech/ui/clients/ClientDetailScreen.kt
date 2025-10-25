
package com.example.wassertech.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    onOpenComponents: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()

    // Data
    val sites by vm.sites(clientId).collectAsState(initial = emptyList())
    var clientName by remember { mutableStateOf("Заказчик") }
    var isCorporate by remember { mutableStateOf(false) }

    // Client edit dialog
    var showEditClient by remember { mutableStateOf(false) }
    var editClientName by remember { mutableStateOf(TextFieldValue("")) }
    var editClientNotes by remember { mutableStateOf(TextFieldValue("")) }
    var editClientCorporate by remember { mutableStateOf(false) }

    // Site edit dialog state
    var editSiteId by remember { mutableStateOf<String?>(null) }

    // Add dialogs
    var showAddSite by remember { mutableStateOf(false) }
    var addSiteName by remember { mutableStateOf(TextFieldValue("")) }
    var addSiteAddr by remember { mutableStateOf(TextFieldValue("")) }

    var showAddInstallation by remember { mutableStateOf(false) }
    var addInstallationName by remember { mutableStateOf(TextFieldValue("")) }

    // Expand/collapse state
    var expandedSites by remember { mutableStateOf(setOf<String>()) }

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

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Header with client name and text "Редактировать"
        ElevatedCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp)) {
                Icon(imageVector = if (isCorporate) AppIcons.ClientCorporate else AppIcons.ClientPrivate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(clientName, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showEditClient = true }) { Text("Редактировать") }
            }
        }

        // Actions: add installation / add site
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAddInstallation = true }, modifier = Modifier.weight(1f)) {
                Text("Добавить установку")
            }
            OutlinedButton(onClick = { showAddSite = true }, modifier = Modifier.weight(1f)) {
                Text("Добавить объект")
            }
        }

        // Expandable sites
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sites, key = { it.id }) { s ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            leadingContent = { Icon(imageVector = AppIcons.Site, contentDescription = null) },
                            headlineContent = { Text(s.name) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { editSiteId = s.id }) {
                                        Icon(Icons.Filled.Edit, contentDescription = null)
                                    }
                                    val isExpanded = expandedSites.contains(s.id)
                                    IconButton(onClick = {
                                        expandedSites = if (isExpanded) expandedSites - s.id else expandedSites + s.id
                                    }) {
                                        Icon(imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                                    }
                                }
                            }
                        )
                        if (expandedSites.contains(s.id)) {
                            // Load installations of this site
                            val installations by vm.installations(s.id).collectAsState(initial = emptyList())
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (installations.isEmpty()) {
                                    Text("Нет установок", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    installations.forEach { inst ->
                                        ElevatedCard(onClick = { onOpenComponents(inst.id) }, modifier = Modifier.fillMaxWidth()) {
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

    // --- Dialogs ---

    if (showEditClient) {
        AlertDialog(
            onDismissRequest = { showEditClient = false },
            title = { Text("Редактировать заказчика") },
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
                val name = editClientName.text.trim()
                TextButton(onClick = {
                    scope.launch {
                        val c = vm.getClient(clientId)
                        if (c != null) {
                            vm.editClient(c.copy(name = name, notes = editClientNotes.text.trim().ifEmpty { null }, isCorporate = editClientCorporate))
                        }
                        showEditClient = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEditClient = false }) { Text("Отмена") } }
        )
    }

    // Edit site dialog
    editSiteId?.let { siteId ->
        EditSiteDialog(siteId = siteId, vm = vm, onClose = { editSiteId = null })
    }

    // Add site dialog
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

    // Add installation dialog (to "Главный" site as discussed)
    if (showAddInstallation) {
        AlertDialog(
            onDismissRequest = { showAddInstallation = false },
            title = { Text("Добавить установку") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = addInstallationName, onValueChange = { addInstallationName = it }, label = { Text("Название установки") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = addInstallationName.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addInstallationToMain(clientId, n)
                        addInstallationName = TextFieldValue("")
                        showAddInstallation = false
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddInstallation = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun EditSiteDialog(siteId: String, vm: HierarchyViewModel, onClose: () -> Unit) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var addr by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(siteId) {
        val s = vm.getSite(siteId)
        if (s != null) {
            name = TextFieldValue(s.name)
            addr = TextFieldValue(s.address ?: "")
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Редактировать объект") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") })
                OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Адрес (опц.)") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    val s = vm.getSite(siteId)
                    if (s != null) {
                        vm.editSite(s.copy(name = name.text.trim(), address = addr.text.trim().ifEmpty { null }))
                    }
                    onClose()
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Отмена") } }
    )
}
