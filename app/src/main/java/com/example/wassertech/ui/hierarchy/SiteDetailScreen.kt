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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.ui.icons.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    siteId: String,
    onOpenInstallation: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var siteName by remember { mutableStateOf("Объект") }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }
    var editAddr by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(siteId) {
        val s = vm.getSite(siteId)
        if (s != null) {
            siteName = s.name
            editName = TextFieldValue(s.name)
            editAddr = TextFieldValue(s.address ?: "")
        }
    }

    val installs by vm.installations(siteId).collectAsState(initial = emptyList())
    var reorderMode by remember { mutableStateOf(false) }
    var localOrder by remember(siteId) { mutableStateOf(installs.map { it.id }) }
    LaunchedEffect(installs) { if (!reorderMode) localOrder = installs.map { it.id } }

    Scaffold(
        floatingActionButton = { if (!reorderMode) ExtendedFloatingActionButton(onClick = { /* TODO add install dialog */ }) { Text("+ Установка") } }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp)) {
                    Icon(imageVector = AppIcons.Site, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(siteName, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, contentDescription = null) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { 
                    reorderMode = !reorderMode
                    if (!reorderMode) localOrder = installs.map { it.id }
                }) { Text(if (reorderMode) "Отмена сортировки" else "Изменить порядок") }
                if (reorderMode) {
                    Button(onClick = { vm.reorderInstallations(siteId, localOrder); reorderMode = false }) { Icon(Icons.Filled.Check, contentDescription = null) }
                }
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                if (reorderMode) {
                    itemsIndexed(installs, key = { _, it -> it.id }) { index, inst ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${index + 1}. ${inst.name}", style = MaterialTheme.typography.titleMedium)
                                Row {
                                    IconButton(onClick = {
                                        val pos = localOrder.indexOf(inst.id)
                                        if (pos > 0) {
                                            val list = localOrder.toMutableList()
                                            val tmp = list[pos-1]; list[pos-1] = list[pos]; list[pos] = tmp
                                            localOrder = list
                                        }
                                    }, enabled = index > 0) { Icon(Icons.Filled.ArrowUpward, contentDescription = null) }
                                    IconButton(onClick = {
                                        val pos = localOrder.indexOf(inst.id)
                                        if (pos >= 0 && pos < localOrder.lastIndex) {
                                            val list = localOrder.toMutableList()
                                            val tmp = list[pos+1]; list[pos+1] = list[pos]; list[pos] = tmp
                                            localOrder = list
                                        }
                                    }, enabled = index < localOrder.lastIndex) { Icon(Icons.Filled.ArrowDownward, contentDescription = null) }
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(installs, key = { _, it -> it.id }) { _, inst ->
                        ElevatedCard(onClick = { onOpenInstallation(inst.id) }, modifier = Modifier.fillMaxWidth()) {
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

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Редактировать объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Название") })
                    OutlinedTextField(value = editAddr, onValueChange = { editAddr = it }, label = { Text("Адрес (опц.)") })
                }
            },
            confirmButton = { 
                TextButton(onClick = { 
                    scope.launch {
                        val s = vm.getSite(siteId)
                        if (s != null) {
                            vm.editSite(s.copy(name = editName.text.trim(), address = editAddr.text.trim().ifEmpty { null }))
                            siteName = editName.text.trim()
                        }
                        showEdit = false
                    }
                }) { Text("Сохранить") } 
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Отмена") } }
        )
    }
}
