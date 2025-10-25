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
import com.example.wassertech.data.entities.InstallationEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationsScreen(
    siteId: String,
    onOpenInstallation: (String) -> Unit,
    onOpenSessions: () -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val list by vm.installations(siteId).collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        floatingActionButton = {
            Column {
                FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { itn ->
                    ElevatedCard(onClick = { onOpenInstallation(itn.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(itn.name, style = MaterialTheme.typography.titleMedium)
                            Row { TextButton(onClick = { vm.deleteInstallation(itn.id) }) { Text("Удалить") } }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новая установка") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addInstallation(siteId, n)
                        name = TextFieldValue("")
                        showAdd = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}
