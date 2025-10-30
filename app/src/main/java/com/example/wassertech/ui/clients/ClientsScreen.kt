package com.example.wassertech.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.ui.unit.Dp

import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding





const val ALL_GROUP_ID: String = "__ALL__"
private const val GENERAL_SECTION_ID: String = "__GENERAL__SECTION__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    groups: List<ClientGroupEntity>,
    clients: List<ClientEntity>,

    selectedGroupId: String?, // не используем в новой верстке, оставлен для совместимости
    includeArchived: Boolean,

    onSelectAll: () -> Unit,
    onSelectNoGroup: () -> Unit,
    onSelectGroup: (String) -> Unit,

    onToggleIncludeArchived: () -> Unit,
    onCreateGroup: (String) -> Unit,

    onAssignClientGroup: (clientId: String, groupId: String?) -> Unit, // не используется в списке
    onClientClick: (ClientEntity) -> Unit = {},
    onAddClient: () -> Unit = {},

    // создание клиента (сохраняем в БД во VM)
    onCreateClient: (name: String, corporate: Boolean, groupId: String?) -> Unit = { _, _, _ -> }
) {
    var createGroupDialog by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }

    // состояние диалога "Новый клиент"
    var createClientDialog by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var newClientCorporate by remember { mutableStateOf(false) }
    var newClientGroupId by remember { mutableStateOf<String?>(null) }
    var groupPickerExpanded by remember { mutableStateOf(false) }

    // ЕДИНСТВЕННАЯ раскрытая секция (по умолчанию "Общая")
    var expandedSectionId by remember { mutableStateOf(GENERAL_SECTION_ID) }

    // Группировка клиентов по clientGroupId (null = без группы)
    val clientsByGroup = remember(clients) { clients.groupBy { it.clientGroupId } }
    val generalClients = clientsByGroup[null].orEmpty()

    // Предрасчёт счётчиков
    val generalCount = generalClients.size
    val countsByGroup = remember(clientsByGroup, groups) {
        groups.associate { g -> g.id to (clientsByGroup[g.id]?.size ?: 0) }
    }

    Scaffold(
        //topBar = { /* если нужен */ },
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
        val layoutDir = LocalLayoutDirection.current

        LazyColumn(
            modifier = Modifier
                .padding(
                    // убираем большой верхний отступ от Scaffold
                    top = 0.dp,
                    // сохраняем боковые отступы от системных панелей (если будут)
                    start = padding.calculateStartPadding(layoutDir),
                    end   = padding.calculateEndPadding(layoutDir),
                    // низ не трогаем — им управляет contentPadding ниже
                )
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp, top = 0.dp)
        ) {
            // ----- СЕКЦИЯ "ОБЩАЯ" -----
            item(key = "header_general") {
                GroupHeader(
                    title = "Общая",
                    count = generalCount,
                    isExpanded = expandedSectionId == GENERAL_SECTION_ID,
                    onToggle = {
                        expandedSectionId =
                            if (expandedSectionId == GENERAL_SECTION_ID) "" else GENERAL_SECTION_ID
                    }
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
                            onClick = { onClientClick(client) },
                            indentStart = 16.dp // сдвиг вправо относительно группы
                        )
                        Divider()
                    }
                }
            }

            // ----- ОСТАЛЬНЫЕ ГРУППЫ -----
            items(
                items = groups,
                key = { "header_${it.id}" }
            ) { group ->
                GroupHeader(
                    title = group.title,
                    count = countsByGroup[group.id] ?: 0,
                    isExpanded = expandedSectionId == group.id,
                    onToggle = {
                        expandedSectionId =
                            if (expandedSectionId == group.id) "" else group.id
                    }
                )
                if (expandedSectionId == group.id) {
                    val list = clientsByGroup[group.id].orEmpty()
                    if (list.isEmpty()) {
                        EmptyGroupStub(indent = 16.dp)
                    } else {
                        list.forEach { client ->
                            ClientListRow(
                                client = client,
                                onClick = { onClientClick(client) },
                                indentStart = 16.dp
                            )
                            Divider()
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

                    // Комбо-бокс выбора группы (кнопка + DropdownMenu)
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
                        androidx.compose.material3.DropdownMenu(
                            expanded = groupPickerExpanded,
                            onDismissRequest = { groupPickerExpanded = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Без группы") },
                                onClick = {
                                    newClientGroupId = null
                                    groupPickerExpanded = false
                                }
                            )
                            if (groups.isNotEmpty()) Divider()
                            groups.forEach { g ->
                                androidx.compose.material3.DropdownMenuItem(
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
    onToggle: () -> Unit
) {
    // слегка более тёмная плашка, чтобы отделить от клиентов
    val bg = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Group,
            contentDescription = "Группа",
            tint = contentColor
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "$title ($count)",
            color = contentColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
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
    indentStart: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = indentStart, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (client.isCorporate == true) Icons.Filled.Business else Icons.Filled.Person
        Icon(
            imageVector = icon,
            contentDescription = if (client.isCorporate == true) "Корпоративный" else "Клиент",
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
        }
    }
}
