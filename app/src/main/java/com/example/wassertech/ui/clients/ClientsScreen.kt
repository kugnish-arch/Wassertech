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
import com.example.wassertech.ui.icons.AppIcons
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

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
    var newCorporate by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }) { Text("+ Клиент") }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clients, key = { it.id }) { c ->
                    ElevatedCard(onClick = { onOpenClient(c.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val icon = if (c.isCorporate) AppIcons.ClientCorporate else AppIcons.ClientPrivate
                            Icon(imageVector = icon, contentDescription = null)
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                        }
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
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Имя/название") }, singleLine = true)
                    OutlinedTextField(value = newNotes, onValueChange = { newNotes = it }, label = { Text("Адрес/заметки (временно)") })
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = newCorporate, onCheckedChange = { newCorporate = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Корпоративный клиент")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.text.trim()
                    if (name.isNotEmpty()) {
                        vm.addClient(name, newNotes.text.trim().ifEmpty { null }, newCorporate)
                        showAdd = false
                        newName = TextFieldValue(""); newNotes = TextFieldValue(""); newCorporate = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}
