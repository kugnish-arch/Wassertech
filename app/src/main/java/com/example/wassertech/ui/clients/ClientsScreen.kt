package com.example.wassertech.ui.clients

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.wassertech.data.entities.ClientEntity
import com.example.wassertech.data.entities.ClientGroupEntity
import com.example.wassertech.ui.common.EditDoneBottomBar

private const val GENERAL_SECTION_ID: String = "__GENERAL__SECTION__"

private data class DeleteDialogState(
    val isClient: Boolean,
    val id: String,
    val name: String
)

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

    onRenameGroup: (groupId: String, newTitle: String) -> Unit = { _, _ -> },
    onRenameClientName: (clientId: String, newName: String) -> Unit = { _, _ -> },

    onArchiveClient: (clientId: String) -> Unit = {},
    onRestoreClient: (clientId: String) -> Unit = {},
    onArchiveGroup: (groupId: String) -> Unit = {},
    onRestoreGroup: (groupId: String) -> Unit = {},

    onMoveGroupUp: (groupId: String) -> Unit = {},
    onMoveGroupDown: (groupId: String) -> Unit = {},
    onMoveClientUp: (clientId: String) -> Unit = {},
    onMoveClientDown: (clientId: String) -> Unit = {},

    // НОВОЕ: массовая фиксация порядка в БД
    onReorderGroupClients: (groupId: String?, orderedIds: List<String>) -> Unit = { _, _ -> },

    // Удаление
    onDeleteClient: (clientId: String) -> Unit = {},
    onDeleteGroup: (groupId: String) -> Unit = {}
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

    // Диалог подтверждения удаления
    var deleteDialogState by remember { mutableStateOf<DeleteDialogState?>(null) }

    // Исходные данные
    val clientsByGroup = remember(clients) { clients.groupBy { it.clientGroupId } }
    val generalClients = clientsByGroup[null].orEmpty()

    // --- словари для быстрого доступа по id (в композиционном контексте) ---
    val generalById = remember(clients, generalClients) {
        generalClients.associateBy { it.id }
    }
    val byGroupIdMap = remember(clients, groups) {
        groups.associate { g ->
            g.id to (clientsByGroup[g.id] ?: emptyList()).associateBy { it.id }
        }
    }



    val generalCount = generalClients.size
    val countsByGroup = remember(clientsByGroup, groups) {
        groups.associate { g -> g.id to (clientsByGroup[g.id]?.size ?: 0) }
    }

    // ===== ЛОКАЛЬНЫЙ ПОРЯДОК ДЛЯ LIVE-ПЕРЕСТАНОВКИ =====
    // null → «Общая»
    var localOrderGeneral by remember(clients) {
        mutableStateOf(generalClients.map { it.id })
    }
    // Для каждой группы — список id в текущем порядке
    var localOrderByGroup by remember(clients, groups) {
        mutableStateOf(
            groups.associate { g ->
                g.id to (clientsByGroup[g.id]?.map { it.id } ?: emptyList())
            }.toMutableMap()
        )
    }

    // Перемещения МЕЖДУ группами копим локально (clientId -> targetGroupId)
    var crossGroupMoves by remember { mutableStateOf(mutableMapOf<String, String?>()) }

    // Вспомогалки
    fun currentIdsFor(groupId: String?): MutableList<String> =
        if (groupId == null) localOrderGeneral.toMutableList()
        else localOrderByGroup[groupId]?.toMutableList() ?: mutableListOf()

    fun setIdsFor(groupId: String?, ids: List<String>) {
        if (groupId == null) localOrderGeneral = ids
        else {
            localOrderByGroup = localOrderByGroup.toMutableMap().also { it[groupId] = ids }
        }
    }

    fun moveIdWithin(groupId: String?, id: String, up: Boolean) {
        val ids = currentIdsFor(groupId)
        val i = ids.indexOf(id)
        if (i == -1) return
        val j = if (up) i - 1 else i + 1
        if (j !in 0..ids.lastIndex) return
        ids[i] = ids[j].also { ids[j] = ids[i] }
        setIdsFor(groupId, ids)
    }

    fun moveIdToGroup(id: String, fromGroupId: String?, toGroupId: String?) {
        if (fromGroupId == toGroupId) return
        val from = currentIdsFor(fromGroupId)
        val to = currentIdsFor(toGroupId)
        if (from.remove(id)) {
            to.add(id) // кладём в конец целевой группы
            setIdsFor(fromGroupId, from)
            setIdsFor(toGroupId, to)
            crossGroupMoves[id] = toGroupId
        }
    }

    // ===== UI =====
    // Диалоги редактирования
    var editGroupId by remember { mutableStateOf<String?>(null) }
    var editGroupTitle by remember { mutableStateOf("") }

    var editClientId by remember { mutableStateOf<String?>(null) }
    var editClientName by remember { mutableStateOf("") }
    var editClientGroupId by remember { mutableStateOf<String?>(null) }
    var editClientGroupPicker by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            EditDoneBottomBar(
                isEditing = isEditMode,
                onEdit = {
                    includeArchivedBeforeEdit = includeArchived
                    if (!includeArchived) onToggleIncludeArchived()
                    isEditMode = true
                    // Зафиксировать локальные порядки по актуальным данным (уже сделано в remember)
                    crossGroupMoves.clear()
                },
                onDone = {
                    // 1) Сначала применяем переносы между группами
                    if (crossGroupMoves.isNotEmpty()) {
                        crossGroupMoves.forEach { (clientId, targetGroupId) ->
                            onAssignClientGroup(clientId, targetGroupId)
                        }
                        crossGroupMoves.clear()
                    }
                    // 2) Затем сохраняем порядок в каждой группе (включая «Общую»)
                    onReorderGroupClients(null, localOrderGeneral)
                    groups.forEach { g ->
                        onReorderGroupClients(g.id, localOrderByGroup[g.id] ?: emptyList())
                    }

                    if (includeArchivedBeforeEdit == false && includeArchived) {
                        onToggleIncludeArchived()
                    }
                    includeArchivedBeforeEdit = null
                    isEditMode = false
                },
                actions = emptyList()
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onAddClient()
                        createClientDialog = true
                    },
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
            // ===== «Общая» секция =====
            item(key = "header_general") {
                GroupHeader(
                    title = "Общая",
                    count = localOrderGeneral.size,
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
                //val generalById = remember(clients) { generalClients.associateBy { it.id } }
                if (localOrderGeneral.isEmpty()) {
                    item(key = "general_empty") { EmptyGroupStub(indent = 16.dp) }
                } else {
                    items(
                        items = localOrderGeneral,
                        key = { it }
                    ) { clientId ->
                        val client = generalById[clientId] ?: return@items
                        ClientRowWithEdit(
                            client = client,
                            groupId = null,
                            groups = groups,
                            isEditMode = isEditMode,
                            onClick = { onClientClick(client.id) },
                            onArchive = { onArchiveClient(client.id) },
                            onRestore = { onRestoreClient(client.id) },
                            onMoveUp = { moveIdWithin(null, client.id, up = true) },
                            onMoveDown = { moveIdWithin(null, client.id, up = false) },
                            onMoveToGroup = { targetGroupId -> moveIdToGroup(client.id, null, targetGroupId) },
                            onEditName = {
                                editClientId = client.id
                                editClientName = client.name
                                editClientGroupId = client.clientGroupId
                            },
                            onDelete = {
                                deleteDialogState = DeleteDialogState(isClient = true, id = client.id, name = client.name)
                            },
                            modifier = Modifier.animateContentSize()
                        )
                        Divider()
                    }
                }
            }

            // ===== Группы =====
            items(
                items = groups,
                key = { "header_${it.id}" }
            ) { group ->
                val groupId = group.id
                GroupHeader(
                    title = group.title,
                    count = (localOrderByGroup[groupId] ?: emptyList()).size,
                    isExpanded = expandedSectionId == groupId,
                    isArchived = group.isArchived == true,
                    canArchive = true,
                    showActions = isEditMode,
                    onArchive = { onArchiveGroup(groupId) },
                    onRestore = { onRestoreGroup(groupId) },
                    onToggle = {
                        expandedSectionId = if (expandedSectionId == groupId) "" else groupId
                    },
                    onMoveUp = { onMoveGroupUp(groupId) },
                    onMoveDown = { onMoveGroupDown(groupId) },
                    onEdit = {
                        editGroupId = groupId
                        editGroupTitle = group.title
                    },
                    onDelete = {
                        deleteDialogState = DeleteDialogState(isClient = false, id = groupId, name = group.title)
                    },
                    modifier = Modifier.animateContentSize()
                )

                if (expandedSectionId == groupId) {
                    val listIds = localOrderByGroup[groupId] ?: emptyList()
                    if (listIds.isEmpty()) {
                        Column {
                            EmptyGroupStub(indent = 16.dp)
                            Divider()
                        }
                    } else {
                        val byId = byGroupIdMap[groupId] ?: emptyMap()
                        Column {
                            listIds.forEach { cid ->
                                val client = byId[cid] ?: return@forEach
                                ClientRowWithEdit(
                                    client = client,
                                    groupId = groupId,
                                    groups = groups,
                                    isEditMode = isEditMode,
                                    onClick = { onClientClick(client.id) },
                                    onArchive = { onArchiveClient(client.id) },
                                    onRestore = { onRestoreClient(client.id) },
                                    onMoveUp = { moveIdWithin(groupId, client.id, up = true) },
                                    onMoveDown = { moveIdWithin(groupId, client.id, up = false) },
                                    onMoveToGroup = { targetGroupId -> moveIdToGroup(client.id, groupId, targetGroupId) },
                                    onEditName = {
                                        editClientId = client.id
                                        editClientName = client.name
                                        editClientGroupId = client.clientGroupId
                                    },
                                    onDelete = {
                                        deleteDialogState = DeleteDialogState(isClient = true, id = client.id, name = client.name)
                                    },
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

    // ===== Диалоги =====

    // Создание группы
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

    // Создание клиента
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

    // Переименование группы
    if (editGroupId != null) {
        AlertDialog(
            onDismissRequest = { editGroupId = null },
            title = { Text("Переименовать группу") },
            text = {
                OutlinedTextField(
                    value = editGroupTitle,
                    onValueChange = { editGroupTitle = it },
                    singleLine = true,
                    label = { Text("Новое название группы") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                val canSave = editGroupTitle.trim().isNotEmpty()
                TextButton(
                    onClick = {
                        if (canSave) {
                            onRenameGroup(editGroupId!!, editGroupTitle.trim())
                            editGroupId = null
                        }
                    },
                    enabled = canSave
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editGroupId = null }) { Text("Отмена") }
            }
        )
    }

    // Редактирование клиента
    if (editClientId != null) {
        AlertDialog(
            onDismissRequest = { editClientId = null },
            title = { Text("Редактировать клиента") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editClientName,
                        onValueChange = { editClientName = it },
                        singleLine = true,
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        OutlinedButton(
                            onClick = { editClientGroupPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val label = when (editClientGroupId) {
                                null -> "Без группы"
                                else -> groups.find { it.id == editClientGroupId }?.title ?: "Группа"
                            }
                            Text(label)
                        }
                        DropdownMenu(
                            expanded = editClientGroupPicker,
                            onDismissRequest = { editClientGroupPicker = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Без группы") },
                                onClick = {
                                    editClientGroupId = null
                                    editClientGroupPicker = false
                                }
                            )
                            if (groups.isNotEmpty()) Divider()
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        editClientGroupId = g.id
                                        editClientGroupPicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val canSave = editClientName.trim().isNotEmpty()
                TextButton(
                    onClick = {
                        if (canSave) {
                            onRenameClientName(editClientId!!, editClientName.trim())
                            onAssignClientGroup(editClientId!!, editClientGroupId)
                            editClientId = null
                        }
                    },
                    enabled = canSave
                ) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { editClientId = null }) { Text("Отмена") }
            }
        )
    }

    // Диалог подтверждения удаления
    deleteDialogState?.let { state ->
        AlertDialog(
            onDismissRequest = { deleteDialogState = null },
            title = { Text("Подтверждение удаления") },
            text = {
                Text(
                    if (state.isClient) {
                        "Вы уверены, что хотите удалить клиента \"${state.name}\"?\n\nЭто действие нельзя отменить."
                    } else {
                        "Вы уверены, что хотите удалить группу \"${state.name}\"?\n\nВсе клиенты из этой группы будут перемещены в \"Общую\". Это действие нельзя отменить."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (state.isClient) {
                            onDeleteClient(state.id)
                        } else {
                            onDeleteGroup(state.id)
                        }
                        deleteDialogState = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogState = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/* ---------- Вспомогательные UI-компоненты ---------- */

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
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDragStart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.secondaryContainer
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    var lastMoveThreshold by remember { mutableStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ручка для перетаскивания (только в режиме редактирования и для неархивных групп)
        if (showActions && !isArchived && canArchive) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Перетащить",
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .pointerInput("group_$title") {
                        detectDragGestures(
                            onDragStart = { 
                                lastMoveThreshold = 0f
                                onDragStart?.invoke() 
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y < -60 && lastMoveThreshold >= -60) {
                                    onMoveUp()
                                    lastMoveThreshold = -60f
                                } else if (dragAmount.y > 60 && lastMoveThreshold <= 60) {
                                    onMoveDown()
                                    lastMoveThreshold = 60f
                                }
                                if (dragAmount.y in -60f..60f) {
                                    lastMoveThreshold = dragAmount.y
                                }
                            },
                            onDragEnd = {
                                lastMoveThreshold = 0f
                            }
                        )
                    }
            )
            Spacer(Modifier.width(8.dp))
        }
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Переименовать группу",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            } else if (isArchived) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Filled.Unarchive, contentDescription = "Восстановить группу", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить группу", tint = MaterialTheme.colorScheme.error)
                    }
                }
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
private fun ClientRowWithEdit(
    client: ClientEntity,
    groupId: String?,
    groups: List<ClientGroupEntity>,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToGroup: (String?) -> Unit, // null = «Без группы»
    onEditName: () -> Unit,
    onDelete: () -> Unit = {},
    onDragStart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    var lastMoveThreshold by remember { mutableStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ручка для перетаскивания (только в режиме редактирования и для неархивных клиентов)
        if (isEditMode && !client.isArchived) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Перетащить",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(24.dp)
                    .pointerInput(client.id) {
                        detectDragGestures(
                            onDragStart = { 
                                lastMoveThreshold = 0f
                                onDragStart?.invoke() 
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                // dragAmount.y - накопленное смещение с начала жеста
                                // Перемещаем при достижении порога 60dp и сбрасываем счетчик
                                if (dragAmount.y < -60 && lastMoveThreshold >= -60) {
                                    onMoveUp()
                                    lastMoveThreshold = -60f
                                } else if (dragAmount.y > 60 && lastMoveThreshold <= 60) {
                                    onMoveDown()
                                    lastMoveThreshold = 60f
                                }
                                // Сбрасываем порог при возврате в зону
                                if (dragAmount.y in -60f..60f) {
                                    lastMoveThreshold = dragAmount.y
                                }
                            },
                            onDragEnd = {
                                lastMoveThreshold = 0f
                            }
                        )
                    }
            )
            Spacer(Modifier.width(8.dp))
        }
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
        if (isEditMode) {
            if (client.isArchived == true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Filled.Unarchive, contentDescription = "Восстановить клиента", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить клиента", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                IconButton(onClick = onEditName) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать клиента", tint = MaterialTheme.colorScheme.onSurface)
                }
                // Меню «Переместить в…»
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Ещё", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Переместить в: Без группы") },
                            onClick = {
                                menuOpen = false
                                onMoveToGroup(null)
                            }
                        )
                        if (groups.isNotEmpty()) Divider()
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text("Переместить в: ${g.title}") },
                                onClick = {
                                    menuOpen = false
                                    onMoveToGroup(g.id)
                                }
                            )
                        }
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Архивировать") },
                            onClick = {
                                menuOpen = false
                                onArchive()
                            }
                        )
                    }
                }
            }
        }
    }
}
