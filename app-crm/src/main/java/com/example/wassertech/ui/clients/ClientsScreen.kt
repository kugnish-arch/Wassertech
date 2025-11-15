package ru.wassertech.ui.clients

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import ru.wassertech.data.entities.ClientEntity
import ru.wassertech.data.entities.ClientGroupEntity
import ru.wassertech.core.ui.R
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.ui.common.FABOption
import ru.wassertech.ui.common.CommonAddDialog
import ru.wassertech.core.ui.theme.ClientsGroupCollapsedBackground
import ru.wassertech.core.ui.theme.ClientsGroupExpandedBackground
import ru.wassertech.core.ui.theme.ClientsGroupExpandedText
import ru.wassertech.core.ui.theme.ClientsGroupBorder
import ru.wassertech.core.ui.theme.ClientsRowDivider
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn

private const val GENERAL_SECTION_ID: String = "__GENERAL__SECTION__"

private data class DeleteDialogState(
    val isClient: Boolean,
    val id: String,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClientsScreen(
    groups: List<ClientGroupEntity>,
    clients: List<ClientEntity>,

    @Suppress("UNUSED_PARAMETER") selectedGroupId: String?,
    includeArchived: Boolean,

    @Suppress("UNUSED_PARAMETER") onSelectAll: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSelectNoGroup: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSelectGroup: (String) -> Unit,

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
    @Suppress("UNUSED_PARAMETER") onMoveClientUp: (clientId: String) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onMoveClientDown: (clientId: String) -> Unit = {},

    // НОВОЕ: массовая фиксация порядка в БД
    onReorderGroupClients: (groupId: String?, orderedIds: List<String>) -> Unit = { _, _ -> },

    // Удаление
    onDeleteClient: (clientId: String) -> Unit = {},
    onDeleteGroup: (groupId: String) -> Unit = {},

    // Режим редактирования
    isEditing: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onToggleEdit: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    var createGroupDialog by remember { mutableStateOf(false) }
    var newGroupTitle by remember { mutableStateOf("") }

    var createClientDialog by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var newClientCorporate by remember { mutableStateOf(false) }
    var newClientGroupId by remember { mutableStateOf<String?>(null) }
    var groupPickerExpanded by remember { mutableStateOf(false) }

    var expandedSectionId by remember { mutableStateOf(GENERAL_SECTION_ID) }

    var includeArchivedBeforeEdit by remember { mutableStateOf<Boolean?>(null) }

    // Используем переданное состояние редактирования или локальное
    val isEditMode = isEditing

    // Диалог подтверждения удаления
    var deleteDialogState by remember { mutableStateOf<DeleteDialogState?>(null) }

    // Фильтруем клиентов: архивные показываются только в режиме редактирования
    val filteredClients = remember(clients, isEditMode, includeArchived) {
        if (isEditMode && includeArchived) {
            clients // Показываем всех клиентов в режиме редактирования с включенным includeArchived
        } else {
            clients.filter { it.isArchived != true } // В обычном режиме скрываем архивных
        }
    }

    // Исходные данные (используем отфильтрованный список)
    val clientsByGroup = remember(filteredClients) { filteredClients.groupBy { it.clientGroupId } }
    val generalClients = clientsByGroup[null].orEmpty()

    // --- словари для быстрого доступа по id (в композиционном контексте) ---
    val generalById = remember(filteredClients, generalClients) {
        generalClients.associateBy { it.id }
    }
    val byGroupIdMap = remember(filteredClients, groups) {
        groups.associate { g ->
            g.id to (clientsByGroup[g.id] ?: emptyList()).associateBy { it.id }
        }
    }

    // ===== ЛОКАЛЬНЫЙ ПОРЯДОК ДЛЯ LIVE-ПЕРЕСТАНОВКИ =====
    var localOrderGeneral by remember { mutableStateOf<List<String>>(emptyList()) }

    var localOrderByGroup by remember {
        mutableStateOf<MutableMap<String, List<String>>>(
            mutableMapOf()
        )
    }

    LaunchedEffect(filteredClients, groups) {
        val currentClientsByGroup = filteredClients.groupBy { it.clientGroupId }
        val currentGeneralClients = currentClientsByGroup[null].orEmpty()

        val newGeneralIds = currentGeneralClients.map { it.id }
        val newOrderByGroup = groups.associate { g ->
            g.id to (currentClientsByGroup[g.id]?.map { it.id } ?: emptyList())
        }

        if (localOrderGeneral != newGeneralIds) {
            localOrderGeneral = newGeneralIds
        }

        val updatedOrderByGroup = newOrderByGroup.toMutableMap()
        if (updatedOrderByGroup != localOrderByGroup) {
            localOrderByGroup = updatedOrderByGroup
        }
    }

    var savedOrderGeneral by remember { mutableStateOf<List<String>>(emptyList()) }
    var savedOrderByGroup by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var savedCrossGroupMoves by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }

    var crossGroupMoves by remember { mutableStateOf(mutableMapOf<String, String?>()) }

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
            to.add(id)
            setIdsFor(fromGroupId, from)
            setIdsFor(toGroupId, to)
            crossGroupMoves[id] = toGroupId
        }
    }

    // ===== UI =====
    var editGroupId by remember { mutableStateOf<String?>(null) }
    var editGroupTitle by remember { mutableStateOf("") }

    var editClientId by remember { mutableStateOf<String?>(null) }
    var editClientName by remember { mutableStateOf("") }
    var editClientGroupId by remember { mutableStateOf<String?>(null) }
    var editClientGroupPicker by remember { mutableStateOf(false) }

    var shouldSave by remember { mutableStateOf(true) }
    var previousIsEditing by remember { mutableStateOf(isEditMode) }

    val wrappedCancel = remember(onCancel) {
        onCancel?.let { originalCancel ->
            {
                shouldSave = false
                originalCancel()
            }
        }
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            includeArchivedBeforeEdit = includeArchived
            if (!includeArchived) onToggleIncludeArchived()
            savedOrderGeneral = localOrderGeneral.toList()
            savedOrderByGroup = localOrderByGroup.mapValues { it.value.toList() }
            savedCrossGroupMoves = crossGroupMoves.toMap()
            crossGroupMoves.clear()
            shouldSave = true
            previousIsEditing = true
        } else if (previousIsEditing) {
            if (shouldSave) {
                if (crossGroupMoves.isNotEmpty()) {
                    crossGroupMoves.forEach { (clientId, targetGroupId) ->
                        onAssignClientGroup(clientId, targetGroupId)
                    }
                    crossGroupMoves.clear()
                }
                onReorderGroupClients(null, localOrderGeneral)
                groups.forEach { g ->
                    onReorderGroupClients(g.id, localOrderByGroup[g.id] ?: emptyList())
                }
            } else {
                localOrderGeneral = savedOrderGeneral.toMutableList()
                localOrderByGroup =
                    savedOrderByGroup.mapValues { it.value.toMutableList() }.toMutableMap()
                crossGroupMoves = savedCrossGroupMoves.toMutableMap()
            }
            includeArchivedBeforeEdit = null
            shouldSave = true
            previousIsEditing = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AppFloatingActionButton(
                template = FABTemplate(
                    icon = Icons.Filled.Add,
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    onClick = { },
                    options = listOf(
                        FABOption(
                            label = "Клиент",
                            icon = Icons.Filled.Person,
                            onClick = {
                                onAddClient()
                                createClientDialog = true
                            }
                        ),
                        FABOption(
                            label = "Группа",
                            icon = Icons.Filled.Group,
                            onClick = {
                                createGroupDialog = true
                            }
                        )
                    ),
                    optionsColor = Color(0xFF1E1E1E)
                )
            )
        }
    ) { innerPadding ->
        val layoutDir = LocalLayoutDirection.current

        val hasAnyData = groups.isNotEmpty() || clients.isNotEmpty()

        Box(modifier = Modifier.fillMaxSize()) {
            if (!hasAnyData && !isEditMode) {
                EmptyStateHint()
            } else {
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
                    item(key = "general_content") {
                        AnimatedVisibility(
                            visible = expandedSectionId == GENERAL_SECTION_ID,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            if (localOrderGeneral.isEmpty()) {
                                EmptyGroupStub(indent = 16.dp)
                            } else if (isEditMode) {
                                // Используем ReorderableLazyColumn только в режиме редактирования
                                // НЕ оборачиваем в Column, чтобы избежать бесконечных ограничений по высоте
                                ReorderableLazyColumn(
                                    items = localOrderGeneral,
                                    onMove = { fromIndex, toIndex ->
                                        val mutable = localOrderGeneral.toMutableList()
                                        val item = mutable.removeAt(fromIndex)
                                        mutable.add(toIndex, item)
                                        localOrderGeneral = mutable
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp), // Ограничиваем максимальную высоту
                                    key = { it }, // clientId: String
                                    contentPadding = PaddingValues(0.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) { clientId, isDragging ->
                                    val client = generalById[clientId] ?: return@ReorderableLazyColumn
                                    
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ClientRowWithEdit(
                                            client = client,
                                            groupId = null,
                                            groups = groups,
                                            isEditMode = isEditMode,
                                            onClick = { onClientClick(client.id) },
                                            onArchive = { onArchiveClient(client.id) },
                                            onRestore = { onRestoreClient(client.id) },
                                            onMoveUp = {
                                                moveIdWithin(
                                                    null,
                                                    client.id,
                                                    up = true
                                                )
                                            },
                                            onMoveDown = {
                                                moveIdWithin(
                                                    null,
                                                    client.id,
                                                    up = false
                                                )
                                            },
                                            onMoveToGroup = { targetGroupId: String? ->
                                                moveIdToGroup(
                                                    client.id,
                                                    null,
                                                    targetGroupId
                                                )
                                            },
                                            onEditName = {
                                                editClientId = client.id
                                                editClientName = client.name
                                                editClientGroupId = client.clientGroupId
                                            },
                                            onDelete = {
                                                deleteDialogState = DeleteDialogState(
                                                    isClient = true,
                                                    id = client.id,
                                                    name = client.name
                                                )
                                            },
                                            isDragging = isDragging,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White)
                                        )
                                        // Разделительная линия между клиентами (кроме последнего)
                                        val index = localOrderGeneral.indexOf(clientId)
                                        if (index >= 0 && index < localOrderGeneral.size - 1) {
                                            HorizontalDivider(
                                                color = ClientsRowDivider,
                                                thickness = 1.dp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // В обычном режиме используем обычный список без drag-n-drop
                                Column(modifier = Modifier.animateContentSize()) {
                                    localOrderGeneral.forEachIndexed { index, clientId ->
                                        val client = generalById[clientId] ?: return@forEachIndexed
                                        
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            ClientRowWithEdit(
                                                client = client,
                                                groupId = null,
                                                groups = groups,
                                                isEditMode = isEditMode,
                                                onClick = { onClientClick(client.id) },
                                                onArchive = { onArchiveClient(client.id) },
                                                onRestore = { onRestoreClient(client.id) },
                                                onMoveUp = { },
                                                onMoveDown = { },
                                                onMoveToGroup = { },
                                                onEditName = {
                                                    editClientId = client.id
                                                    editClientName = client.name
                                                    editClientGroupId = client.clientGroupId
                                                },
                                                onDelete = {
                                                    deleteDialogState = DeleteDialogState(
                                                        isClient = true,
                                                        id = client.id,
                                                        name = client.name
                                                    )
                                                },
                                                isDragging = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White)
                                            )
                                            if (index < localOrderGeneral.size - 1) {
                                                HorizontalDivider(
                                                    color = ClientsRowDivider,
                                                    thickness = 1.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(
                        items = if (isEditMode && includeArchived) {
                            groups
                        } else {
                            groups.filter { it.isArchived != true }
                        },
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
                                expandedSectionId =
                                    if (expandedSectionId == groupId) "" else groupId
                            },
                            onMoveUp = { onMoveGroupUp(groupId) },
                            onMoveDown = { onMoveGroupDown(groupId) },
                            onEdit = {
                                editGroupId = groupId
                                editGroupTitle = group.title
                            },
                            onDelete = {
                                deleteDialogState = DeleteDialogState(
                                    isClient = false,
                                    id = groupId,
                                    name = group.title
                                )
                            },
                            modifier = Modifier.animateContentSize()
                        )

                        AnimatedVisibility(
                            visible = expandedSectionId == groupId,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            val listIds = localOrderByGroup[groupId] ?: emptyList()
                            if (listIds.isEmpty()) {
                                Column(modifier = Modifier.animateContentSize()) {
                                    EmptyGroupStub(indent = 16.dp)
                                    HorizontalDivider(color = ClientsRowDivider, thickness = 1.dp)
                                }
                            } else if (isEditMode) {
                                // Используем ReorderableLazyColumn только в режиме редактирования
                                // НЕ оборачиваем в Column, чтобы избежать бесконечных ограничений по высоте
                                ReorderableLazyColumn(
                                    items = listIds,
                                    onMove = { fromIndex, toIndex ->
                                        val mutable = listIds.toMutableList()
                                        val item = mutable.removeAt(fromIndex)
                                        mutable.add(toIndex, item)
                                        localOrderByGroup = localOrderByGroup.toMutableMap().also { it[groupId] = mutable }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp), // Ограничиваем максимальную высоту
                                    key = { it }, // clientId: String
                                    contentPadding = PaddingValues(0.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) { cid, isDragging ->
                                    val client = byGroupIdMap[groupId]?.get(cid) ?: return@ReorderableLazyColumn
                                    
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        ClientRowWithEdit(
                                            client = client,
                                            groupId = groupId,
                                            groups = groups,
                                            isEditMode = isEditMode,
                                            onClick = { onClientClick(client.id) },
                                            onArchive = { onArchiveClient(client.id) },
                                            onRestore = { onRestoreClient(client.id) },
                                            onMoveUp = {
                                                moveIdWithin(
                                                    groupId,
                                                    client.id,
                                                    up = true
                                                )
                                            },
                                            onMoveDown = {
                                                moveIdWithin(
                                                    groupId,
                                                    client.id,
                                                    up = false
                                                )
                                            },
                                            onMoveToGroup = { targetGroupId: String? ->
                                                moveIdToGroup(
                                                    client.id,
                                                    groupId,
                                                    targetGroupId
                                                )
                                            },
                                            onEditName = {
                                                editClientId = client.id
                                                editClientName = client.name
                                                editClientGroupId = client.clientGroupId
                                            },
                                            onDelete = {
                                                deleteDialogState = DeleteDialogState(
                                                    isClient = true,
                                                    id = client.id,
                                                    name = client.name
                                                )
                                            },
                                            isDragging = isDragging,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White)
                                        )
                                        // Разделительная линия между клиентами (кроме последнего)
                                        val index = listIds.indexOf(cid)
                                        if (index >= 0 && index < listIds.size - 1) {
                                            HorizontalDivider(
                                                color = ClientsRowDivider,
                                                thickness = 1.dp
                                            )
                                        }
                                    }
                                }
                            } else {
                                // В обычном режиме используем обычный список без drag-n-drop
                                Column(modifier = Modifier.animateContentSize()) {
                                    val byId = byGroupIdMap[groupId] ?: emptyMap()
                                    listIds.forEachIndexed { index, cid ->
                                        val client = byId[cid] ?: return@forEachIndexed
                                        
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            ClientRowWithEdit(
                                                client = client,
                                                groupId = groupId,
                                                groups = groups,
                                                isEditMode = isEditMode,
                                                onClick = { onClientClick(client.id) },
                                                onArchive = { onArchiveClient(client.id) },
                                                onRestore = { onRestoreClient(client.id) },
                                                onMoveUp = { },
                                                onMoveDown = { },
                                                onMoveToGroup = { },
                                                onEditName = {
                                                    editClientId = client.id
                                                    editClientName = client.name
                                                    editClientGroupId = client.clientGroupId
                                                },
                                                onDelete = {
                                                    deleteDialogState = DeleteDialogState(
                                                        isClient = true,
                                                        id = client.id,
                                                        name = client.name
                                                    )
                                                },
                                                isDragging = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White)
                                            )
                                            if (index < listIds.size - 1) {
                                                HorizontalDivider(
                                                    color = ClientsRowDivider,
                                                    thickness = 1.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Диалоги и т.д. — код ниже оставлен без изменений (включая диалоги создания/редактирования/удаления)

                if (createGroupDialog) {
                    CommonAddDialog(
                        title = "Новая группа",
                        text = {
                            OutlinedTextField(
                                value = newGroupTitle,
                                onValueChange = { newGroupTitle = it },
                                singleLine = true,
                                label = { Text("Название группы") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        onDismissRequest = { createGroupDialog = false },
                        confirmText = "Создать",
                        onConfirm = {
                            val title = newGroupTitle.trim()
                            if (title.isNotEmpty()) {
                                onCreateGroup(title)
                                newGroupTitle = ""
                                createGroupDialog = false
                            }
                        },
                        confirmEnabled = newGroupTitle.trim().isNotEmpty()
                    )
                }

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
                                            else -> groups.find { it.id == newClientGroupId }?.title
                                                ?: "Группа"
                                        }
                                        Text(label)
                                    }
                                    DropdownMenu(
                                        expanded = groupPickerExpanded,
                                        onDismissRequest = { groupPickerExpanded = false },
                                        modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Без группы") },
                                            onClick = {
                                                newClientGroupId = null
                                                groupPickerExpanded = false
                                            }
                                        )
                                        if (groups.isNotEmpty()) HorizontalDivider()
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
                                        onCreateClient(
                                            newClientName.trim(),
                                            newClientCorporate,
                                            newClientGroupId
                                        )
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

                if (editGroupId != null) {
                    CommonAddDialog(
                        title = "Переименовать группу",
                        text = {
                            OutlinedTextField(
                                value = editGroupTitle,
                                onValueChange = { editGroupTitle = it },
                                singleLine = true,
                                label = { Text("Новое название группы") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        onDismissRequest = { editGroupId = null },
                        confirmText = "Сохранить",
                        onConfirm = {
                            val canSave = editGroupTitle.trim().isNotEmpty()
                            if (canSave) {
                                onRenameGroup(editGroupId!!, editGroupTitle.trim())
                                editGroupId = null
                            }
                        },
                        confirmEnabled = editGroupTitle.trim().isNotEmpty()
                    )
                }

                if (editClientId != null) {
                    CommonAddDialog(
                        title = "Редактировать клиента",
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
                                            else -> groups.find { it.id == editClientGroupId }?.title
                                                ?: "Группа"
                                        }
                                        Text(label)
                                    }
                                    DropdownMenu(
                                        expanded = editClientGroupPicker,
                                        onDismissRequest = { editClientGroupPicker = false },
                                        modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Без группы") },
                                            onClick = {
                                                editClientGroupId = null
                                                editClientGroupPicker = false
                                            }
                                        )
                                        if (groups.isNotEmpty()) HorizontalDivider()
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
                        onDismissRequest = { editClientId = null },
                        confirmText = "Сохранить",
                        onConfirm = {
                            val canSave = editClientName.trim().isNotEmpty()
                            if (canSave) {
                                onRenameClientName(editClientId!!, editClientName.trim())
                                onAssignClientGroup(editClientId!!, editClientGroupId)
                                editClientId = null
                            }
                        },
                        confirmEnabled = editClientName.trim().isNotEmpty()
                    )
                }

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
        }
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
    @Suppress("UNUSED_PARAMETER") onArchive: () -> Unit,
    onRestore: () -> Unit,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDragStart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bg = if (isExpanded) ClientsGroupExpandedBackground else ClientsGroupCollapsedBackground
    val contentColor =
        if (isExpanded) ClientsGroupExpandedText else MaterialTheme.colorScheme.onBackground
    var lastMoveThreshold by remember { mutableStateOf(0f) }
    var dragOffset by remember { mutableStateOf(0.dp) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable { onToggle() }
                .offset(y = dragOffset)
                .zIndex(if (isDragging) 1f else 0f) // Перетаскиваемая группа поверх всех
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                                    dragOffset = 0.dp
                                    isDragging = true
                                    onDragStart?.invoke()
                                },
                                onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                                    // NOTE: не вызываем consumePositionChange(), т.к. в используемой версии Compose этот метод отсутствует.
                                    dragOffset += with(density) { dragAmount.y.toDp() }

                                    val threshold = 10f
                                    if (dragAmount.y < -threshold && lastMoveThreshold >= -threshold) {
                                        onMoveUp()
                                        lastMoveThreshold = -threshold
                                        dragOffset = 0.dp
                                    } else if (dragAmount.y > threshold && lastMoveThreshold <= threshold) {
                                        onMoveDown()
                                        lastMoveThreshold = threshold
                                        dragOffset = 0.dp
                                    }
                                    if (dragAmount.y in -threshold..threshold) {
                                        lastMoveThreshold = dragAmount.y
                                    }
                                },
                                onDragEnd = {
                                    lastMoveThreshold = 0f
                                    dragOffset = 0.dp
                                    isDragging = false
                                }
                            )
                        }
                )
                Spacer(Modifier.width(8.dp))
            }
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
                                tint = contentColor
                            )
                        }
                        IconButton(onClick = onArchive) {
                            Icon(
                                Icons.Filled.Archive,
                                contentDescription = "Архивировать группу",
                                tint = contentColor
                            )
                        }
                    }
                } else if (isArchived) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onRestore) {
                            Icon(
                                Icons.Filled.Unarchive,
                                contentDescription = "Восстановить группу",
                                tint = contentColor
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                contentDescription = "Удалить группу",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = if (isExpanded) ru.wassertech.core.ui.theme.NavigationIcons.CollapseMenuIcon else ru.wassertech.core.ui.theme.NavigationIcons.ExpandMenuIcon,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = contentColor
            )
        }
        HorizontalDivider(
            color = ClientsGroupBorder,
            thickness = 1.dp
        )
    }
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
private fun EmptyStateHint() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Начните с клиентов",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Создайте клиента или группу клиентов, чтобы добавить их объекты и установки. После этого вы сможете проводить и сохранять техническое обслуживание, а также формировать PDF-отчёты прямо в приложении.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClientRowWithEdit(
    client: ClientEntity,
    @Suppress("UNUSED_PARAMETER") groupId: String?,
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
    isDragging: Boolean,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isEditMode) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditMode && !client.isArchived) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Перетащить",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        val iconRes = when {
                client.isArchived == true && client.isCorporate == true -> R.drawable.person_client_corporate_red
                client.isArchived == true && client.isCorporate != true -> R.drawable.person_client_red
                client.isCorporate == true -> R.drawable.person_client_corporate_blue
                else -> R.drawable.person_client_blue
            }
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = if (client.isCorporate == true) "Корпоративный" else "Клиент",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit
            )
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
            if (!isEditMode) {
                Icon(
                    imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (isEditMode) {
                if (client.isArchived == true) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onRestore) {
                            Icon(
                                Icons.Filled.Unarchive,
                                contentDescription = "Восстановить клиента",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                contentDescription = "Удалить клиента",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onEditName) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Редактировать клиента",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Ещё",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Переместить в: Без группы") },
                                onClick = {
                                    menuOpen = false
                                    onMoveToGroup(null)
                                }
                            )
                            if (groups.isNotEmpty()) HorizontalDivider()
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text("Переместить в: ${g.title}") },
                                    onClick = {
                                        menuOpen = false
                                        onMoveToGroup(g.id)
                                    }
                                )
                            }
                            HorizontalDivider()
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

