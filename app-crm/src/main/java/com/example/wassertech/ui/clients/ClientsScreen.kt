package ru.wassertech.ui.clients

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.components.EntityGroupHeader
import ru.wassertech.core.ui.components.EmptyGroupPlaceholder
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu

private const val GENERAL_SECTION_ID: String = "__GENERAL__SECTION__"

private data class DeleteDialogState(
    val isClient: Boolean,
    val id: String,
    val name: String
)

private data class ClientSearchMatch(
    val client: ClientEntity,
    val score: Int
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

    // Состояние поиска
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Фильтруем клиентов: архивные показываются только в режиме редактирования
    val filteredClients = remember(clients, isEditMode, includeArchived) {
        if (isEditMode && includeArchived) {
            clients // Показываем всех клиентов в режиме редактирования с включенным includeArchived
        } else {
            clients.filter { it.isArchived != true } // В обычном режиме скрываем архивных
        }
    }

    // Результаты поиска с fuzzy-поиском и сортировкой по релевантности
    val searchResults = remember(filteredClients, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList<ClientEntity>()
        } else {
            val q = searchQuery.trim().lowercase()
            val matches = filteredClients
                .mapNotNull { client ->
                    val score = computeClientSearchScore(client, q)
                    if (score > 0) ClientSearchMatch(client, score) else null
                }
                .sortedByDescending { it.score }
            matches.map { it.client }
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
                AppEmptyState(
                    icon = Icons.Filled.Lightbulb,
                    title = "Начните с клиентов",
                    description = "Создайте клиента или группу клиентов, чтобы добавить их объекты и установки. После этого вы сможете проводить и сохранять техническое обслуживание, а также формировать PDF-отчёты прямо в приложении."
                )
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
                    // Поле поиска
                    item(key = "search_bar") {
                        ClientsSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ТЕКУЩАЯ ЛОГИКА: отображение групп и секций (только если поиск пуст)
                    if (searchQuery.isBlank()) {
                        item(key = "header_general") {
                        EntityGroupHeader(
                            title = "Общая",
                            count = localOrderGeneral.size,
                            isExpanded = expandedSectionId == GENERAL_SECTION_ID,
                            isArchived = false,
                            canArchive = false,
                            showActions = isEditMode,
                            onArchive = null,
                            onRestore = null,
                            onToggle = {
                                expandedSectionId =
                                    if (expandedSectionId == GENERAL_SECTION_ID) "" else GENERAL_SECTION_ID
                            },
                            onMoveUp = null,
                            onMoveDown = null,
                        )
                    }
                    item(key = "general_content") {
                        AnimatedVisibility(
                            visible = expandedSectionId == GENERAL_SECTION_ID,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            if (localOrderGeneral.isEmpty()) {
                                EmptyGroupPlaceholder(text = "Клиенты отсутствуют", indent = 16.dp)
                            } else {
                                // Используем ReorderableLazyColumn всегда, чтобы detectReorderAfterLongPress мог работать
                                // НЕ оборачиваем в Column, чтобы избежать бесконечных ограничений по высоте
                                ReorderableLazyColumn(
                                    items = localOrderGeneral,
                                    onMove = { fromIndex, toIndex ->
                                        // Всегда обновляем локальное состояние для корректного отображения перетаскивания
                                        val mutable = localOrderGeneral.toMutableList()
                                        val item = mutable.removeAt(fromIndex)
                                        mutable.add(toIndex, item)
                                        localOrderGeneral = mutable
                                        // Изменения сохраняются в БД только в режиме редактирования (в LaunchedEffect при выходе из режима)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 2000.dp), // Большое значение вместо fillMaxHeight для избежания бесконечных ограничений
                                    key = { it }, // clientId: String
                                    contentPadding = PaddingValues(0.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) { clientId, isDragging, reorderableState ->
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
                                                if (isEditMode) {
                                                    moveIdWithin(
                                                        null,
                                                        client.id,
                                                        up = true
                                                    )
                                                }
                                            },
                                            onMoveDown = {
                                                if (isEditMode) {
                                                    moveIdWithin(
                                                        null,
                                                        client.id,
                                                        up = false
                                                    )
                                                }
                                            },
                                            onMoveToGroup = { targetGroupId: String? ->
                                                if (isEditMode) {
                                                    moveIdToGroup(
                                                        client.id,
                                                        null,
                                                        targetGroupId
                                                    )
                                                }
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
                                            reorderableState = reorderableState,
                                            onLongClick = null, // Не передаем onLongClick, чтобы не включать режим редактирования при long press
                                            onToggleEdit = onToggleEdit, // Передаем onToggleEdit для автоматического включения режима редактирования при начале перетаскивания
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
                        EntityGroupHeader(
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
                                    EmptyGroupPlaceholder(text = "Клиенты отсутствуют", indent = 16.dp)
                                    HorizontalDivider(color = ClientsRowDivider, thickness = 1.dp)
                                }
                            } else {
                                // Используем ReorderableLazyColumn всегда, чтобы detectReorderAfterLongPress мог работать
                                // НЕ оборачиваем в Column, чтобы избежать бесконечных ограничений по высоте
                                ReorderableLazyColumn(
                                    items = listIds,
                                    onMove = { fromIndex, toIndex ->
                                        // Всегда обновляем локальное состояние для корректного отображения перетаскивания
                                        val mutable = listIds.toMutableList()
                                        val item = mutable.removeAt(fromIndex)
                                        mutable.add(toIndex, item)
                                        localOrderByGroup = localOrderByGroup.toMutableMap().also { it[groupId] = mutable }
                                        // Изменения сохраняются в БД только в режиме редактирования (в LaunchedEffect при выходе из режима)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 2000.dp), // Большое значение вместо fillMaxHeight для избежания бесконечных ограничений
                                    key = { it }, // clientId: String
                                    contentPadding = PaddingValues(0.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) { cid, isDragging, reorderableState ->
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
                                                if (isEditMode) {
                                                    moveIdWithin(
                                                        groupId,
                                                        client.id,
                                                        up = true
                                                    )
                                                }
                                            },
                                            onMoveDown = {
                                                if (isEditMode) {
                                                    moveIdWithin(
                                                        groupId,
                                                        client.id,
                                                        up = false
                                                    )
                                                }
                                            },
                                            onMoveToGroup = { targetGroupId: String? ->
                                                if (isEditMode) {
                                                    moveIdToGroup(
                                                        client.id,
                                                        groupId,
                                                        targetGroupId
                                                    )
                                                }
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
                                            reorderableState = reorderableState,
                                            onLongClick = null, // Не передаем onLongClick, чтобы не включать режим редактирования при long press
                                            onToggleEdit = onToggleEdit, // Передаем onToggleEdit для автоматического включения режима редактирования при начале перетаскивания
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
                            }
                        }
                    }
                    }

                    // РЕЖИМ ПОИСКА: единый плоский список результатов (только если поиск не пуст)
                    if (searchQuery.isNotBlank()) {
                        if (searchResults.isEmpty()) {
                            item(key = "search_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Клиенты не найдены",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(
                                items = searchResults,
                                key = { it.id }
                            ) { client ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    ClientRowWithEdit(
                                        client = client,
                                        groupId = client.clientGroupId,
                                        groups = groups,
                                        isEditMode = isEditMode,
                                        searchQuery = searchQuery,
                                        onClick = { onClientClick(client.id) },
                                        onArchive = { onArchiveClient(client.id) },
                                        onRestore = { onRestoreClient(client.id) },
                                        onMoveUp = { /* не используется в режиме поиска */ },
                                        onMoveDown = { /* не используется в режиме поиска */ },
                                        onMoveToGroup = { targetGroupId ->
                                            onAssignClientGroup(client.id, targetGroupId)
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
                                        isDragging = false,
                                        reorderableState = null,
                                        onLongClick = onToggleEdit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White)
                                    )
                                    // Разделительная линия между клиентами (кроме последнего)
                                    val index = searchResults.indexOf(client)
                                    if (index >= 0 && index < searchResults.size - 1) {
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

/* ---------- Вспомогательные функции для поиска ---------- */

/**
 * Вычисляет релевантность клиента для поискового запроса.
 * Возвращает score > 0, если клиент подходит под запрос, иначе 0.
 * Чем выше score, тем более релевантен клиент.
 */
private fun computeClientSearchScore(
    client: ClientEntity,
    query: String
): Int {
    if (query.isBlank()) return 0
    
    val name = client.name.orEmpty().lowercase()
    val phone = client.phone.orEmpty().lowercase()
    val email = client.email.orEmpty().lowercase()
    val address = client.addressFull.orEmpty().lowercase()
    
    var score = 0
    
    // Поиск по имени (наиболее важное поле)
    if (name.isNotEmpty()) {
        when {
            name == query -> score += 1000 // Точное совпадение
            name.startsWith(query) -> score += 500 // Начинается с запроса
            name.contains(query) -> {
                score += 300 // Содержит запрос
                // Бонус за близость к началу
                val index = name.indexOf(query)
                score += (100 - index.coerceAtMost(100))
            }
            else -> {
                // Fuzzy-поиск: проверяем подстроки и похожесть
                val fuzzyScore = calculateFuzzyScore(name, query)
                if (fuzzyScore > 0) {
                    score += fuzzyScore
                }
            }
        }
    }
    
    // Поиск по телефону
    if (phone.isNotEmpty() && phone.contains(query)) {
        score += 200
        if (phone.startsWith(query)) score += 50
    }
    
    // Поиск по email
    if (email.isNotEmpty() && email.contains(query)) {
        score += 150
        if (email.startsWith(query)) score += 30
    }
    
    // Поиск по адресу
    if (address.isNotEmpty() && address.contains(query)) {
        score += 100
        if (address.startsWith(query)) score += 20
    }
    
    return score
}

/**
 * Вычисляет fuzzy-оценку для строки на основе подстрок и похожести.
 * Использует простую эвристику для поиска похожих строк.
 * Поддерживает поиск с опечатками и неполными совпадениями.
 */
private fun calculateFuzzyScore(text: String, query: String): Int {
    if (text.length < query.length) return 0
    
    // Проверяем, содержит ли текст все символы запроса в правильном порядке
    // Это позволяет находить "кембридж" при запросе "кмбрдж"
    var textIndex = 0
    var queryIndex = 0
    var matchedChars = 0
    var firstMatchIndex = -1
    
    while (textIndex < text.length && queryIndex < query.length) {
        if (text[textIndex] == query[queryIndex]) {
            if (firstMatchIndex == -1) {
                firstMatchIndex = textIndex
            }
            matchedChars++
            queryIndex++
        }
        textIndex++
    }
    
    // Если все символы запроса найдены в правильном порядке
    if (queryIndex == query.length) {
        // Бонус за близость символов (меньше пропусков = выше score)
        val ratio = matchedChars.toFloat() / query.length
        val distancePenalty = (textIndex - query.length).coerceAtMost(50)
        val positionBonus = if (firstMatchIndex < 10) 20 else 0 // Бонус за раннее совпадение
        return ((ratio * 200).toInt() - distancePenalty + positionBonus).coerceAtLeast(50)
    }
    
    // Проверяем максимальную общую подстроку
    val maxSubstring = findMaxCommonSubstring(text, query)
    if (maxSubstring.length >= query.length / 2) {
        val substringRatio = maxSubstring.length.toFloat() / query.length
        return (substringRatio * 100).toInt().coerceAtMost(100)
    }
    
    // Проверяем похожесть через сравнение символов (для очень коротких запросов)
    if (query.length <= 3) {
        var similarChars = 0
        val queryChars = query.toSet()
        text.forEach { char ->
            if (char in queryChars) {
                similarChars++
            }
        }
        if (similarChars >= query.length) {
            return (similarChars * 5).coerceAtMost(30)
        }
    }
    
    return 0
}

/**
 * Находит максимальную общую подстроку между двумя строками.
 */
private fun findMaxCommonSubstring(text: String, query: String): String {
    var maxSubstring = ""
    for (i in text.indices) {
        for (j in i + 1..text.length) {
            val substring = text.substring(i, j)
            if (query.contains(substring) && substring.length > maxSubstring.length) {
                maxSubstring = substring
            }
        }
    }
    return maxSubstring
}

/* ---------- Вспомогательные UI-компоненты ---------- */

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    normalColor: Color = Color.Unspecified,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    highlightFontWeight: FontWeight = FontWeight.SemiBold
) {
    val normalColorValue = if (normalColor != Color.Unspecified) normalColor else MaterialTheme.colorScheme.onSurface
    
    if (query.isBlank() || text.isBlank()) {
        Text(
            text = text,
            style = style,
            color = normalColorValue,
            modifier = modifier
        )
        return
    }
    
    val lowerText = text.lowercase()
    val lowerQuery = query.trim().lowercase()
    
    if (!lowerText.contains(lowerQuery)) {
        Text(
            text = text,
            style = style,
            color = normalColorValue,
            modifier = modifier
        )
        return
    }
    
    val annotatedString = buildAnnotatedString {
        var startIndex = 0
        
        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) {
                // Добавляем оставшийся текст
                append(text.substring(startIndex))
                break
            }
            
            // Добавляем текст до совпадения
            if (index > startIndex) {
                append(text.substring(startIndex, index))
            }
            
            // Добавляем выделенный текст
            withStyle(
                style = SpanStyle(
                    color = highlightColor,
                    fontWeight = highlightFontWeight
                )
            ) {
                append(text.substring(index, index + lowerQuery.length))
            }
            
            startIndex = index + lowerQuery.length
        }
    }
    
    Text(
        text = annotatedString,
        style = style,
        color = normalColorValue,
        modifier = modifier
    )
}

@Composable
private fun ClientsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Поиск"
            )
        },
        placeholder = { Text("Поиск по клиентам") },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Очистить"
                    )
                }
            }
        } else null
    )
}

@Composable
private fun ClientRowWithEdit(
    client: ClientEntity,
    @Suppress("UNUSED_PARAMETER") groupId: String?,
    groups: List<ClientGroupEntity>,
    isEditMode: Boolean,
    searchQuery: String = "",
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToGroup: (String?) -> Unit, // null = «Без группы»
    onEditName: () -> Unit,
    onDelete: () -> Unit = {},
    isDragging: Boolean,
    reorderableState: ReorderableState? = null,
    onLongClick: (() -> Unit)? = null,
    onToggleEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Определяем иконку клиента в зависимости от состояния
    val iconRes = when {
        client.isArchived == true && client.isCorporate == true -> R.drawable.person_client_corporate_red
        client.isArchived == true && client.isCorporate != true -> R.drawable.person_client_red
        client.isCorporate == true -> R.drawable.person_client_corporate_blue
        else -> R.drawable.person_client_blue
    }

    // Формируем подзаголовок из контактных данных
    val subtitle = listOfNotNull(
        client.phone?.takeIf { it.isNotBlank() },
        client.email?.takeIf { it.isNotBlank() },
        client.addressFull?.takeIf { it.isNotBlank() }
    ).joinToString(" · ").takeIf { it.isNotBlank() }

    // Преобразуем группы в формат (id, title) для EntityRowWithMenu
    val availableGroups = groups.map { it.id to it.title }

    // Если есть поисковый запрос, используем кастомную версию с подсветкой
    if (searchQuery.isNotBlank()) {
        ClientRowWithHighlight(
            client = client,
            iconRes = iconRes,
            subtitle = subtitle,
            groups = groups,
            isEditMode = isEditMode,
            searchQuery = searchQuery,
            onClick = onClick,
            onArchive = onArchive,
            onRestore = onRestore,
            onDelete = onDelete,
            onEditName = onEditName,
            onMoveToGroup = onMoveToGroup,
            availableGroups = availableGroups,
            onLongClick = onLongClick,
            modifier = modifier.background(Color.White)
        )
    } else {
        EntityRowWithMenu(
            title = client.name,
            subtitle = subtitle,
            leadingIcon = {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = if (client.isCorporate == true) "Корпоративный" else "Клиент",
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
            },
            trailingIcon = if (!isEditMode) {
                {
                    Icon(
                        imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                        contentDescription = "Открыть",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else null,
            isEditMode = isEditMode,
            isArchived = client.isArchived == true,
            onClick = onClick,
            onRestore = onRestore,
            onArchive = onArchive,
            onDelete = onDelete,
            onEdit = onEditName,
            onMoveToGroup = onMoveToGroup,
            availableGroups = availableGroups,
            modifier = modifier.background(Color.White),
            reorderableState = reorderableState,
            showDragHandle = isEditMode && !client.isArchived,
            onLongClick = onLongClick,
            isDragging = isDragging,
            onToggleEdit = onToggleEdit
        )
    }
}

/**
 * Кастомная версия строки клиента с подсветкой совпадений для режима поиска.
 * Повторяет структуру EntityRowWithMenu, но использует HighlightedText для title и subtitle.
 */
@Composable
private fun ClientRowWithHighlight(
    client: ClientEntity,
    iconRes: Int,
    subtitle: String?,
    groups: List<ClientGroupEntity>,
    isEditMode: Boolean,
    searchQuery: String,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onEditName: () -> Unit,
    onMoveToGroup: (String?) -> Unit,
    availableGroups: List<Pair<String, String>>,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isEditMode && (onClick != null || onLongClick != null)) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongClick?.invoke() }
                        )
                    }
                } else if (!isEditMode) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = if (client.isCorporate == true) "Корпоративный" else "Клиент",
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            HighlightedText(
                text = client.name,
                query = searchQuery,
                style = MaterialTheme.typography.titleMedium,
                normalColor = if (client.isArchived == true) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
                highlightColor = MaterialTheme.colorScheme.primary,
                highlightFontWeight = FontWeight.SemiBold
            )
            if (subtitle != null && subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                HighlightedText(
                    text = subtitle,
                    query = searchQuery,
                    style = MaterialTheme.typography.bodyMedium,
                    normalColor = if (client.isArchived == true) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                    highlightColor = MaterialTheme.colorScheme.primary,
                    highlightFontWeight = FontWeight.SemiBold
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
                    onRestore?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Filled.Unarchive,
                                contentDescription = "Восстановить",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(
                                imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                onEditName?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (onMoveToGroup != null || onArchive != null) {
                    Box {
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
                            if (onMoveToGroup != null) {
                                DropdownMenuItem(
                                    text = { Text("Переместить в: Без группы") },
                                    onClick = {
                                        menuOpen = false
                                        onMoveToGroup(null)
                                    }
                                )
                                if (availableGroups.isNotEmpty()) {
                                    HorizontalDivider()
                                }
                                availableGroups.forEach { (groupId, groupTitle) ->
                                    DropdownMenuItem(
                                        text = { Text("Переместить в: $groupTitle") },
                                        onClick = {
                                            menuOpen = false
                                            onMoveToGroup(groupId)
                                        }
                                    )
                                }
                            }
                            if (onMoveToGroup != null && onArchive != null) {
                                HorizontalDivider()
                            }
                            onArchive?.let {
                                DropdownMenuItem(
                                    text = { Text("Архивировать") },
                                    onClick = {
                                        menuOpen = false
                                        it()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

