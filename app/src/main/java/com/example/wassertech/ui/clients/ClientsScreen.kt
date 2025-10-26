package com.example.wassertech.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wassertech.viewmodel.HierarchyViewModel
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.ui.icons.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onOpenClient: (String) -> Unit = {},
    vm: HierarchyViewModel = viewModel()
) {
    // Переключатель внизу — режим работы с архивом
    var archiveMode by rememberSaveable { mutableStateOf(false) }

    // Источник данных зависит от режима
    val clients by remember(archiveMode) {
        if (archiveMode) vm.clients(true) else vm.clients()
    }.collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var newCorporate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Клиенты") }) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Работа с архивом", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = archiveMode, onCheckedChange = { archiveMode = it })
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) { Text("+ Клиент") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = clients, key = { it.id }) { c ->
                ClientCard(
                    client = c,
                    showArchivedBadge = archiveMode && c.isArchived,
                    showActions = archiveMode, // кнопки видны только в архивном режиме
                    onOpen = { onOpenClient(c.id) },
                    onArchive = { vm.archiveClient(c.id) },
                    onRestore = { vm.restoreClient(c.id) }
                )
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новый клиент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Название/ФИО") })
                    OutlinedTextField(value = newNotes, onValueChange = { newNotes = it }, label = { Text("Заметка") })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = newCorporate, onCheckedChange = { newCorporate = it })
                        Spacer(Modifier.width(8.dp)); Text("Корпоративный")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        vm.addClient(name, newNotes.trim().ifEmpty { null }, newCorporate)
                        showAdd = false
                        newName = ""; newNotes = ""; newCorporate = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun ClientCard(
    client: ClientEntity,
    showArchivedBadge: Boolean,
    showActions: Boolean,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit
) {
    var confirmArchive by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (client.isCorporate) AppIcons.ClientCorporate else AppIcons.ClientPrivate
                Icon(imageVector = icon, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(client.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (showArchivedBadge) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text("Архив", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (!client.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(client.notes!!, style = MaterialTheme.typography.bodyMedium)
            }
            if (showActions) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (client.isArchived) {
                        TextButton(onClick = { confirmRestore = true }) { Text("Восстановить") }
                    } else {
                        TextButton(onClick = { confirmArchive = true }) { Text("Архивировать") }
                    }
                }
            }
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text("Архивировать клиента") },
            text = { Text("Клиент «${client.name}» будет перенесён в архив. Продолжить?") },
            confirmButton = { TextButton(onClick = { confirmArchive = false; onArchive() }) { Text("Архивировать") } },
            dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("Отмена") } }
        )
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Восстановление из архива") },
            text = { Text("Клиент «${client.name}» будет возвращён из архива.") },
            confirmButton = { TextButton(onClick = { confirmRestore = false; onRestore() }) { Text("Восстановить") } },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Отмена") } }
        )
    }
}
