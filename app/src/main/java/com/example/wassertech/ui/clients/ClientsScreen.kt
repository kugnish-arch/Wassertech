package com.example.wassertech.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox

import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity

const val ALL_GROUP_ID: String = "__ALL__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    groups: List<ClientGroupEntity>,
    clients: List<ClientEntity>,

    selectedGroupId: String?, // ALL_GROUP_ID = все, null = без группы, иначе конкретная группа
    includeArchived: Boolean,

    onSelectAll: () -> Unit,
    onSelectNoGroup: () -> Unit,
    onSelectGroup: (String) -> Unit,

    onToggleIncludeArchived: () -> Unit,
    onCreateGroup: (String) -> Unit,

    onAssignClientGroup: (clientId: String, groupId: String?) -> Unit,
    onClientClick: (ClientEntity) -> Unit = {},
    onAddClient: () -> Unit = {},

    // новый коллбек создания клиента
    onCreateClient: (name: String, corporate: Boolean, groupId: String?) -> Unit = { _, _, _ -> }
) {
    var groupsMenuExpanded by remember { mutableStateOf(false) }
    var createGroupDialog by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }

    // состояние диалога "Новый клиент"
    var createClientDialog by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var newClientCorporate by remember { mutableStateOf(false) }
    var newClientGroupId by remember { mutableStateOf<String?>(null) }
    var groupPickerExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // заголовок скрыт по твоей версии
            /* CenterAlignedTopAppBar(
                title = { Text("Клиенты", fontWeight = FontWeight.SemiBold) },
                actions = { }
            ) */
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconToggleButton(
                    checked = includeArchived,
                    onCheckedChange = { onToggleIncludeArchived() }
                ) {
                    Icon(
                        imageVector = if (includeArchived) Icons.Filled.Delete else Icons.Outlined.Delete,
                        contentDescription = "Показать архив"
                    )
                }
                Text("Корзина")
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onAddClient()
                        // локально открываем диалог создания клиента
                        createClientDialog = true
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Клиент")
                }
                ExtendedFloatingActionButton(
                    onClick = { createGroupDialog = true }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Группа")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // (временно скрыто) фильтры по группам
            Spacer(Modifier.height(8.dp))

            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Text(
                        "Нет клиентов для выбранного фильтра",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 0.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
                        ClientRow(
                            client = client,
                            groups = groups,
                            onClick = { onClientClick(client) },
                            onAssignGroup = { gid -> onAssignClientGroup(client.id, gid) }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // Диалог создания группы
    if (createGroupDialog) {
        AlertDialog(
            onDismissRequest = { createGroupDialog = false },
            title = { Text("Новая группа") },
            text = {
                OutlinedTextField(
                    value = newGroupTitle,
                    onValueChange = { newGroupTitle = it },
                    singleLine = true,
                    label = { Text("Название группы") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newGroupTitle.trim()
                        if (title.isNotEmpty()) {
                            onCreateGroup(title)
                            newGroupTitle = ""
                            createGroupDialog = false
                        }
                    }
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { createGroupDialog = false }) { Text("Отмена") }
            }
        )
    }

    // Диалог создания клиента
    if (createClientDialog) {
        AlertDialog(
            onDismissRequest = { createClientDialog = false },
            title = { Text("Новый клиент") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newClientName,
                        onValueChange = { newClientName = it },
                        singleLine = true,
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = newClientCorporate,
                            onCheckedChange = { newClientCorporate = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Корпоративный")
                    }

                    // Комбо-бокс выбора группы (простая кнопка + DropdownMenu)
                    Box {
                        OutlinedButton(
                            onClick = { groupPickerExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val label = when (newClientGroupId) {
                                null -> "Без группы"
                                else -> groups.find { it.id == newClientGroupId }?.title ?: "Группа"
                            }
                            Text(label)
                        }
                        DropdownMenu(
                            expanded = groupPickerExpanded,
                            onDismissRequest = { groupPickerExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Без группы") },
                                onClick = {
                                    newClientGroupId = null
                                    groupPickerExpanded = false
                                }
                            )
                            if (groups.isNotEmpty()) Divider()
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        newClientGroupId = g.id
                                        groupPickerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val canSave = newClientName.trim().isNotEmpty()
                TextButton(
                    onClick = {
                        if (canSave) {
                            onCreateClient(newClientName.trim(), newClientCorporate, newClientGroupId)
                            // reset
                            newClientName = ""
                            newClientCorporate = false
                            newClientGroupId = null
                            createClientDialog = false
                        }
                    },
                    enabled = canSave
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        createClientDialog = false
                    }
                ) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ClientRow(
    client: ClientEntity,
    groups: List<ClientGroupEntity>,
    onClick: () -> Unit,
    onAssignGroup: (String?) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(client.name, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        val secondary = listOfNotNull(
            client.phone?.takeIf { it.isNotBlank() },
            client.email?.takeIf { it.isNotBlank() },
            client.addressFull?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        if (secondary.isNotBlank()) {
            Text(secondary, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { menuExpanded = true }) {
                val label = when (client.clientGroupId) {
                    null -> "Без группы"
                    else -> groups.find { it.id == client.clientGroupId }?.title ?: "Группа"
                }
                Text(label)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Без группы") },
                    onClick = {
                        onAssignGroup(null)
                        menuExpanded = false
                    }
                )
                if (true) Divider()
                groups.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g.title) },
                        onClick = {
                            onAssignGroup(g.id)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipLikeButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    if (selected) {
        FilledTonalButton(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}
