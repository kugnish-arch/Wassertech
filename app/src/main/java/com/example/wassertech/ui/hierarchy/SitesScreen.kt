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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    clientId: String,
    onOpenSite: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val sites by vm.sites(clientId).collectAsState(initial = emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var addr by remember { mutableStateOf(TextFieldValue("")) }
    
    // Получаем клиента для отображения имени в шапке
    val client by vm.client(clientId).collectAsState(initial = null)
    val clientName = client?.name ?: "Клиент"

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Шапка экрана - приведена к единому виду с другими экранами
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = clientName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Объекты",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                    )
                }
            }
            
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sites, key = { it.id }) { s ->
                    ElevatedCard(onClick = { onOpenSite(s.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium)
                            if (!s.address.isNullOrBlank()) Text(s.address!!, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Новый объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, singleLine = true)
                    OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Адрес (опц.)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = name.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addSite(clientId, n, addr.text.trim().ifEmpty { null })
                        name = TextFieldValue(""); addr = TextFieldValue("")
                        showAdd = false
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Отмена") } }
        )
    }
}
