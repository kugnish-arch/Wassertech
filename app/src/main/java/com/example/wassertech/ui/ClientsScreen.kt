package com.example.wassertech.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.wassertech.R
import com.example.wassertech.data.ClientEntity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.Box
import com.example.wassertech.ui.AppSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: ClientsViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateTo: (AppSection) -> Unit
) {

    var showDialog by remember { mutableStateOf(false) }
    val clients by viewModel.clients.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_wassertech),
                            contentDescription = "Wassertech",
                            modifier = Modifier
                                .height(40.dp)      // <-- увеличь, если хочешь крупнее
                                .aspectRatio(3f)    // <-- примерно 3:1, чтобы логотип был вытянут
                        )

                    }
                },
                actions = {
                    // Переключатель темы
                    IconButton(onClick = onToggleTheme) {
                        val icon = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode
                        Icon(icon, contentDescription = "Переключить тему")
                    }

                    // Меню разделов
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Клиенты") },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateTo(AppSection.Clients)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Пустой экран") },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateTo(AppSection.Empty)
                                }
                            )
                        }
                    }


                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить клиента")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (clients.isEmpty()) {
                Text(
                    "Клиентов пока нет. Нажми «+», чтобы добавить.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
                        ClientCard(client = client, onDelete = { viewModel.deleteClient(client) })
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddClientDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, phone, address, notes ->
                viewModel.addClient(name, phone, address, notes)
                showDialog = false
            }
        )
    }
}

@Composable
fun ClientCard(client: ClientEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(text = client.name, style = MaterialTheme.typography.titleMedium)
                client.phone?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodyMedium) }
                client.address?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodyMedium) }
                client.notes?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Удалить") }
        }
    }
}

@Composable
private fun AddClientDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, address: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый клиент") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; if (nameError && it.isNotBlank()) nameError = false },
                    label = { Text("Имя *") },
                    isError = nameError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError) {
                    Text("Поле «Имя» обязательно", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { nameError = true } else { onConfirm(name.trim(), phone, address, notes) }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
