package com.example.wassertech.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
    onClientClick: (ClientEntity) -> Unit = {}
) {
    var groupsMenuExpanded by remember { mutableStateOf(false) }
    var createGroupDialog by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Клиенты", fontWeight = FontWeight.SemiBold) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Архив", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = includeArchived,
                            onCheckedChange = { onToggleIncludeArchived() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { createGroupDialog = true }
            ) {

                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Новая группа")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Фильтры по группам
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChipLikeButton(
                    selected = selectedGroupId == ALL_GROUP_ID,
                    onClick = onSelectAll,
                    label = "Все"
                )
                Spacer(Modifier.width(8.dp))
                FilterChipLikeButton(
                    selected = selectedGroupId == null,
                    onClick = onSelectNoGroup,
                    label = "Без группы"
                )
                Spacer(Modifier.width(8.dp))

                // Выпадающее меню выбора группы
                Box {
                    OutlinedButton(onClick = { groupsMenuExpanded = true }) {
                        val title = when {
                            selectedGroupId == ALL_GROUP_ID -> "Группы"
                            selectedGroupId == null -> "Без группы"
                            else -> groups.find { it.id == selectedGroupId }?.title ?: "Группы"
                        }
                        Text(title)
                    }
                    DropdownMenu(
                        expanded = groupsMenuExpanded,
                        onDismissRequest = { groupsMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все") },
                            onClick = {
                                onSelectAll()
                                groupsMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Без группы") },
                            onClick = {
                                onSelectNoGroup()
                                groupsMenuExpanded = false
                            }
                        )
                        if (groups.isNotEmpty()) {
                            Divider()
                        }
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.title) },
                                onClick = {
                                    onSelectGroup(g.id)
                                    groupsMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Divider()

            // Список клиентов
            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет клиентов для выбранного фильтра")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
                        ClientRow(
                            client = client,
                            currentGroup = when (client.clientGroupId) {
                                null -> "Без группы"
                                else -> groups.find { it.id == client.clientGroupId }?.title ?: "—"
                            },
                            groups = groups,
                            onClick = { onClientClick(client) },
                            onAssignGroup = { gid ->
                                onAssignClientGroup(client.id, gid)
                            }
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
}

@Composable
private fun ClientRow(
    client: ClientEntity,
    currentGroup: String,
    groups: List<ClientGroupEntity>,
    onClick: () -> Unit,
    onAssignGroup: (String?) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                client.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (client.isArchived) FontWeight.Normal else FontWeight.SemiBold
            )
            val secondary = buildString {
                client.phone?.takeIf { it.isNotBlank() }?.let { append(it) }
                client.email?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" • ")
                    append(it)
                }
                if (currentGroup.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(currentGroup)
                }
            }
            if (secondary.isNotBlank()) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Box {
            TextButton(onClick = { menuExpanded = true }) { Text("Группа") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Без группы") },
                    onClick = {
                        onAssignGroup(null)
                        menuExpanded = false
                    }
                )
                if (groups.isNotEmpty()) Divider()
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
