package com.example.wassertech.ui.clients

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
import com.example.wassertech.data.entities.ClientEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onOpenClient: (String) -> Unit = {},
    vm: HierarchyViewModel = viewModel()
) {
    val clients by vm.clients().collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var newNotes by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (clients.isEmpty()) {
                Text(
                    "Нет клиентов. Нажми + чтобы добавить.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(clients, key = { it.id }) { c ->
                        ClientCard(
                            c,
                            onDelete = { vm.deleteClient(c.id) },
                            onOpen = { onOpenClient(c.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новый клиент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Название/имя") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Адрес/заметки (временно)") },
                        singleLine = false,
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.text.trim()
                    if (name.isNotEmpty()) {
                        vm.addClient(name, newNotes.text.trim().ifEmpty { null })
                        showAdd = false
                        newName = TextFieldValue("")
                        newNotes = TextFieldValue("")
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun ClientCard(c: ClientEntity, onDelete: () -> Unit, onOpen: () -> Unit) {
    ElevatedCard(onClick = onOpen) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(c.name, style = MaterialTheme.typography.titleMedium)
            if (!c.notes.isNullOrBlank()) {
                Text(c.notes!!, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) { Text("Удалить") }
                TextButton(onClick = onOpen) { Text("Открыть") }
            }
        }
    }
}
