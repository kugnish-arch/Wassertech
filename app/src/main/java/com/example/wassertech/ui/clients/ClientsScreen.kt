package com.example.wassertech.ui.clients

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// NOTE: убрали animateItemPlacement, чтобы не зависеть от версии foundation

import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity

private const val GENERAL_SECTION_ID: String = "__GENERAL__SECTION__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    groups: List<ClientGroupEntity>,
    clients: List<ClientEntity>,

    selectedGroupId: String?,
    includeArchived: Boolean,

    onSelectAll: () -> Unit,
    onSelectNoGroup: () -> Unit,
    onSelectGroup: (String) -> Unit,

    onToggleIncludeArchived: () -> Unit,
    onCreateGroup: (String) -> Unit,

    onAssignClientGroup: (clientId: String, groupId: String?) -> Unit,
    onClientClick: (String) -> Unit = {},
    onAddClient: () -> Unit = {},

    onCreateClient: (name: String, corporate: Boolean, groupId: String?) -> Unit = { _, _, _ -> },

    // архивация/восстановление
    onArchiveClient: (clientId: String) -> Unit = {},
    onRestoreClient: (clientId: String) -> Unit = {},
    onArchiveGroup: (groupId: String) -> Unit = {},
    onRestoreGroup: (groupId: String) -> Unit = {},

    // перемещение
    onMoveGroupUp: (groupId: String) -> Unit = {},
    onMoveGroupDown: (groupId: String) -> Unit = {},
    onMoveClientUp: (clientId: String) -> Unit = {},
    onMoveClientDown: (clientId: String) -> Unit = {},
) {
    var createGroupDialog by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }

    var createClientDialog by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var newClientCorporate by remember { mutableStateOf(false) }
    var newClientGroupId by remember { mutableStateOf<String?>(null) }
    var groupPickerExpanded by remember { mutableStateOf(false) }

    var expandedSectionId by remember { mutableStateOf(GENERAL_SECTION_ID) }

    var isEditMode by remember { mutableStateOf(false) }
    var includeArchivedBeforeEdit by remember { mutableStateOf<Boolean?>(null) }

    val clientsByGroup = remember(clients) { clients.groupBy { it.clientGroupId } }
    val generalClients = clientsByGroup[null].orEmpty()

    val generalCount = generalClients.size
    val countsByGroup = remember(clientsByGroup, groups) {
        groups.associate { g -> g.id to (clientsByGroup[g.id]?.size ?: 0) }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        val next = !isEditMode
                        if (next) {
                            includeArchivedBeforeEdit = includeArchived
                            if (!includeArchived) onToggleIncludeArchived()
                        } else {
                            if (includeArchivedBeforeEdit == false && includeArchived) {
                                onToggleIncludeArchived()
                            }
                            includeArchivedBeforeEdit = null
                        }
                        isEditMode = next
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isEditMode)
                            Color(0xFF26A69A) // зелёно-бирюзовый при активном редактировании
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isEditMode)
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEditMode) "Готово" else "Изменить")
                }
                Spacer(Modifier.width(16.dp))
                Text(if (isEditMode) "Редактирование" else "Просмотр")
            }
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onAddClient()
                        createClientDialog = true
                    },
                    //containerColor = Color(0xFF4CAF50),
                    //contentColor = Color.White
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Клиент")
                }
                ExtendedFloatingActionButton(onClick = { createGroupDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Группа")
                }
            }
        }
    ) { innerPadding ->
        val layoutDir = LocalLayoutDirection.current


        LazyColumn(
            modifier = Modifier
                .padding(
                    top = 0.dp,
                    start = innerPadding.calculateStartPadding(layoutDir),
                    end = innerPadding.calculateEndPadding(layoutDir),
                )
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp, top = 0.dp)
        ) {
            // Общее
            item(key = "header_general") {
                GroupHeader(
                    title = "Общая",
                    count = generalCount,
                    isExpanded = expandedSectionId == GENERAL_SECTION_ID,
                    isArchived = false,
                    canArchive = false,
                    showActions = isEditMode,
                    onArchive = {},
                    onRestore = {},
                    onToggle = {
                        expandedSectionId =
                            if (expandedSectionId == GENERAL_SECTION_ID) "" else GENERAL_SECTION_ID
                    },
                    onMoveUp = {},
                    onMoveDown = {},
                )
            }
            if (expandedSectionId == GENERAL_SECTION_ID) {
                if (generalClients.isEmpty()) {
                    item(key = "general_empty") { EmptyGroupStub(indent = 16.dp) }
                } else {
                    items(
                        items = generalClients,
                        key = { it.id }
                    ) { client ->
                        ClientListRow(
                            client = client,
                            onClick = { onClientClick(client.id) },
                            indentStart = 16.dp,
                            showActions = isEditMode,
                            onArchive = { onArchiveClient(client.id) },
                            onRestore = { onRestoreClient(client.id) },
                            onMoveUp = { onMoveClientUp(client.id) },
                            onMoveDown = { onMoveClientDown(client.id) },
                            // вместо animateItemPlacement используем мягкое изменение размера
                            modifier = Modifier.animateContentSize()
                        )
                        Divider()
                    }
                }
            }

            // Остальные группы
            items(
                items = groups,
                key = { "header_${it.id}" }
            ) { group ->
                // Шапка группы
                GroupHeader(
                    title = group.title,
                    count = countsByGroup[group.id] ?: 0,
                    isExpanded = expandedSectionId == group.id,
                    isArchived = group.isArchived == true,
                    canArchive = true,
                    showActions = isEditMode,
                    onArchive = { onArchiveGroup(group.id) },
                    onRestore = { onRestoreGroup(group.id) },
                    onToggle = {
                        expandedSectionId =
                            if (expandedSectionId == group.id) "" else group.id
                    },
                    onMoveUp = { onMoveGroupUp(group.id) },
                    onMoveDown = { onMoveGroupDown(group.id) },
                    modifier = Modifier.animateContentSize()
                )

                // Содержимое группы (внутри Column, т.к. мы уже в item-контенте)
                if (expandedSectionId == group.id) {
                    val list = clientsByGroup[group.id].orEmpty()
                    if (list.isEmpty()) {
                        Column {
                            EmptyGroupStub(indent = 16.dp)
                            Divider()
                        }
                    } else {
                        Column {
                            list.forEach { client ->
                                ClientListRow(
                                    client = client,
                                    onClick = { onClientClick(client.id) },
                                    indentStart = 16.dp,
                                    showActions = isEditMode,
                                    onArchive = { onArchiveClient(client.id) },
                                    onRestore = { onRestoreClient(client.id) },
                                    onMoveUp = { onMoveClientUp(client.id) },
                                    onMoveDown = { onMoveClientDown(client.id) },
                                    modifier = Modifier.animateContentSize()
                                )
                                Divider()
                            }
                        }
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = newClientCorporate,
                            onCheckedChange = { newClientCorporate = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Корпоративный")
                    }

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
                TextButton(onClick = { createClientDialog = false }) { Text("Отмена") }
            }
        )
    }
}

/* ---------- UI-компоненты ---------- */

@Composable
private fun GroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    isArchived: Boolean,
    canArchive: Boolean,
    showActions: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Group, contentDescription = "Группа", tint = contentColor)
        Spacer(Modifier.width(12.dp))
        Text(
            "$title ($count)",
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (showActions) {
            if (!isArchived && canArchive) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Вверх", tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Вниз", tint = MaterialTheme.colorScheme.outline)
                    }
                    TextButton(onClick = onArchive) { Text("Архивировать", color = contentColor) }
                }
            } else if (isArchived) {
                TextButton(onClick = onRestore) { Text("Восстановить", color = contentColor) }
            }
        }
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
            tint = contentColor
        )
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun EmptyGroupStub(indent: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Text("Клиенты отсутствуют", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ClientListRow(
    client: ClientEntity,
    onClick: () -> Unit,
    indentStart: Dp,
    showActions: Boolean,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = indentStart, end = 16.dp, top = 12.dp, bottom = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (client.isCorporate == true) Icons.Filled.Business else Icons.Filled.Person
        Icon(icon, contentDescription = if (client.isCorporate == true) "Корпоративный" else "Клиент")
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                client.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (client.isArchived == true) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            val secondary = listOfNotNull(
                client.phone?.takeIf { it.isNotBlank() },
                client.email?.takeIf { it.isNotBlank() },
                client.addressFull?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (secondary.isNotBlank()) {
                Text(
                    secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (client.isArchived == true) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showActions) {
            if (client.isArchived == true) {
                TextButton(onClick = onRestore) { Text("Восстановить") }
            } else {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Вверх", tint = MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Вниз", tint = MaterialTheme.colorScheme.outline)
                }
                TextButton(onClick = onArchive) { Text("Архивировать") }
            }
        }
    }
}
