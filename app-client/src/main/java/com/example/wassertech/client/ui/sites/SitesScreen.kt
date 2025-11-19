package ru.wassertech.client.ui.sites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.SiteEntity
import ru.wassertech.core.auth.SessionManager
import ru.wassertech.core.auth.OriginType
import ru.wassertech.client.permissions.canCreateEntity
import ru.wassertech.client.permissions.canEditSite
import ru.wassertech.client.permissions.canDeleteSite
import ru.wassertech.core.auth.UserAuthService
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.theme.ClientsRowDivider
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.icons.IconPickerUiState
import ru.wassertech.core.ui.components.IconPickerDialog
import ru.wassertech.client.permissions.canChangeIconForSite
import ru.wassertech.client.data.repository.IconRepository
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import ru.wassertech.client.ui.common.LocalEditingState
import androidx.compose.material.icons.filled.Image
import kotlinx.coroutines.flow.map
// Временно используем стандартные Material3 компоненты
// TODO: Вынести AppFloatingActionButton и CommonAddDialog в core:ui или создать локальные версии
import java.util.UUID
import ru.wassertech.core.screens.hierarchy.ClientSitesScreenShared
import ru.wassertech.core.screens.hierarchy.ui.ClientSitesUiState
import ru.wassertech.client.ui.hierarchy.ClientHierarchyUiStateMapper
import ru.wassertech.client.data.entities.toUserMembershipInfoList
import ru.wassertech.core.auth.HierarchyPermissionChecker

/**
 * Экран списка объектов для app-client.
 * Показывает объекты текущего клиента (как созданные инженером, так и самим клиентом).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SitesScreen(
    clientId: String,
    onOpenSite: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val iconRepository = remember { IconRepository(context) }
    val scope = rememberCoroutineScope()
    
    // Получаем состояние редактирования из CompositionLocal
    val editingState = LocalEditingState.current
    val isEditing = editingState?.isEditing ?: false
    val onToggleEdit = editingState?.onToggle
    
    // Получаем текущую сессию пользователя
    val currentUser = remember { SessionManager.getInstance(context).getCurrentSession() }
    
    // Получаем user_membership для текущего пользователя
    val membershipsRaw by remember(currentUser?.userId) {
        if (currentUser?.userId != null) {
            db.userMembershipDao().observeForUser(currentUser.userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<ru.wassertech.client.data.entities.UserMembershipEntity>())
        }
    }.collectAsState(initial = emptyList())
    val memberships = remember(membershipsRaw) {
        membershipsRaw.toUserMembershipInfoList()
    }
    
    // Получаем список объектов для текущего клиента (включая архивные в режиме редактирования)
    val sites by if (isEditing) {
        db.hierarchyDao().observeSitesIncludingArchived(clientId)
    } else {
        db.hierarchyDao().observeSites(clientId)
    }.collectAsState(initial = emptyList())
    
    // Получаем информацию о клиенте для отображения в заголовке
    val client by remember(clientId) {
        db.clientDao().observeClientRaw(clientId)
    }.collectAsState(initial = emptyList())
    val clientName = client.firstOrNull()?.name ?: "Мои объекты"
    val isCorporate = client.firstOrNull()?.isCorporate ?: false
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newSiteName by remember { mutableStateOf("") }
    var newSiteAddress by remember { mutableStateOf("") }
    
    var editSiteId by remember { mutableStateOf<String?>(null) }
    var editSiteName by remember { mutableStateOf("") }
    var editSiteAddress by remember { mutableStateOf("") }
    
    var deleteSiteId by remember { mutableStateOf<String?>(null) }
    
    // Состояние для IconPickerDialog
    var isIconPickerVisible by remember { mutableStateOf(false) }
    var iconPickerState by remember { mutableStateOf<IconPickerUiState?>(null) }
    var iconPickerSiteId by remember { mutableStateOf<String?>(null) }
    
    // Локальный порядок теперь управляется shared-экраном
    
    // Загружаем иконки для всех сайтов
    var siteIcons by remember {
        mutableStateOf<Map<String, ru.wassertech.client.data.entities.IconEntity?>>(emptyMap())
    }
    LaunchedEffect(sites) {
        scope.launch(Dispatchers.IO) {
            val iconsMap = mutableMapOf<String, ru.wassertech.client.data.entities.IconEntity?>()
            sites.forEach { site ->
                if (site.iconId != null) {
                    val icon = db.iconDao().getById(site.iconId)
                    iconsMap[site.id] = icon
                } else {
                    iconsMap[site.id] = null
                }
            }
            siteIcons = iconsMap
        }
    }
    
    // Преобразуем в UI State с учётом прав доступа
    var uiState by remember {
        mutableStateOf<ClientSitesUiState?>(null)
    }
    LaunchedEffect(sites, siteIcons, currentUser, memberships) {
        if (currentUser != null) {
            scope.launch(Dispatchers.IO) {
                val siteItems = sites.mapNotNull { site ->
                    val icon = siteIcons[site.id]
                    withContext(Dispatchers.IO) {
                        ClientHierarchyUiStateMapper.run {
                            site.toSiteItemUi(iconRepository, currentUser, memberships, icon)
                        }
                    }
                }
                // Для CLIENT роли пользователь может создавать объекты, если он является клиентом
                val canAddSite = currentUser.isClient() && currentUser.clientId != null
                uiState = ClientSitesUiState(
                    clientId = clientId,
                    clientName = clientName,
                    isCorporate = isCorporate,
                    sites = siteItems,
                    includeArchived = isEditing,
                    canAddSite = canAddSite,
                    canEditClient = false,
                    isLoading = false
                )
            }
        }
    }
    
    // Используем shared-экран с черным заголовком
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Черный заголовок клиента
        Column(modifier = Modifier.fillMaxWidth()) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = ru.wassertech.core.ui.theme.HeaderCardStyle.backgroundColor
                ),
                shape = ru.wassertech.core.ui.theme.HeaderCardStyle.shape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ru.wassertech.core.ui.theme.HeaderCardStyle.padding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Выбираем иконку клиента в зависимости от типа
                    val clientIconRes =
                        if (isCorporate) R.drawable.person_client_corporate_blue else R.drawable.person_client_blue
                    Image(
                        painter = painterResource(id = clientIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize * 2),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        clientName,
                        style = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                        color = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                    )
                }
            }
            HorizontalDivider(
                color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
            )
        }
        
        // Shared-экран со списком объектов
        uiState?.let { state ->
            Box(modifier = Modifier.fillMaxSize()) {
                ClientSitesScreenShared(
                    state = state,
                    onSiteClick = onOpenSite,
                    onAddSiteClick = {
                        showAddDialog = true
                        newSiteName = ""
                        newSiteAddress = ""
                    },
                    onSiteArchive = { siteId ->
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().archiveSite(siteId)
                        }
                    },
                    onSiteRestore = { siteId ->
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().restoreSite(siteId)
                        }
                    },
                    onSiteDelete = { siteId ->
                        deleteSiteId = siteId
                    },
                    onChangeSiteIcon = { siteId ->
                        if (currentUser != null) {
                            val site = sites.find { it.id == siteId }
                            if (site != null && canChangeIconForSite(currentUser, site)) {
                                scope.launch(Dispatchers.IO) {
                                    val packs = db.iconPackDao().getAll()
                                    val allIcons = db.iconDao().getAllActive()
                                    val filteredIcons = allIcons.filter { icon ->
                                        icon.entityType == "ANY" || icon.entityType == IconEntityType.SITE.name
                                    }
                                    val iconsByPack = filteredIcons.groupBy { it.packId }
                                    
                                    val iconsByPackWithPaths = iconsByPack.mapValues { (_, icons) ->
                                        icons.map { icon ->
                                            var localPath = iconRepository.getLocalIconPath(icon.id)
                                            
                                            if (localPath == null && !icon.imageUrl.isNullOrBlank() && icon.androidResName.isNullOrBlank()) {
                                                val downloadResult = iconRepository.downloadIconImage(icon.id, icon.imageUrl)
                                                if (downloadResult.isSuccess) {
                                                    localPath = iconRepository.getLocalIconPath(icon.id)
                                                }
                                            }
                                            
                                            ru.wassertech.core.ui.components.IconUiData(
                                                id = icon.id,
                                                packId = icon.packId,
                                                label = icon.label,
                                                entityType = icon.entityType,
                                                androidResName = icon.androidResName,
                                                code = icon.code,
                                                localImagePath = localPath
                                            )
                                        }
                                    }
                                    
                                    iconPickerState = IconPickerUiState(
                                        packs = packs.map { 
                                            ru.wassertech.core.ui.components.IconPackUiData(
                                                id = it.id,
                                                name = it.name
                                            )
                                        },
                                        iconsByPack = iconsByPackWithPaths
                                    )
                                    iconPickerSiteId = siteId
                                    isIconPickerVisible = true
                                }
                            }
                        }
                    },
                    onSitesReordered = { newOrder ->
                        scope.launch(Dispatchers.IO) {
                            val currentSites = db.hierarchyDao().observeSitesIncludingArchived(clientId).first()
                            val ordered = newOrder.mapIndexedNotNull { idx, id ->
                                currentSites.firstOrNull { it.id == id }?.copy(orderIndex = idx)
                            }
                            if (ordered.isNotEmpty()) {
                                db.hierarchyDao().reorderSites(ordered)
                            }
                        }
                    },
                    isEditing = isEditing,
                    onToggleEdit = onToggleEdit
                )
            }
        }
    }
    
    // Диалог добавления объекта
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSiteName,
                        onValueChange = { newSiteName = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSiteAddress,
                        onValueChange = { newSiteAddress = it },
                        label = { Text("Адрес (опционально)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                    val name = newSiteName.trim()
                    if (name.isNotEmpty() && currentUser != null) {
                        scope.launch(Dispatchers.IO) {
                            val session = ru.wassertech.core.auth.SessionManager.getInstance(context).getCurrentSession()
                            val currentTime = System.currentTimeMillis()
                            val newSite = SiteEntity(
                                id = UUID.randomUUID().toString(),
                                clientId = clientId,
                                name = name,
                                address = newSiteAddress.trim().takeIf { it.isNotEmpty() },
                                orderIndex = sites.size,
                                isArchived = false,
                                archivedAtEpoch = null,
                                createdAtEpoch = currentTime,
                                updatedAtEpoch = currentTime,
                                deletedAtEpoch = null,
                                dirtyFlag = true, // Помечаем как требующий синхронизации
                                syncStatus = 1, // QUEUED
                                ownerClientId = currentUser.clientId,
                                origin = OriginType.CLIENT.name,
                                createdByUserId = session?.userId
                            )
                            db.hierarchyDao().upsertSite(newSite)
                            
                            // Автоматически создаём membership для созданного объекта
                            if (session?.userId != null) {
                                ru.wassertech.client.data.UserMembershipHelper.createSiteMembership(
                                    context = context,
                                    siteId = newSite.id,
                                    userId = session.userId
                                )
                            }
                        }
                            showAddDialog = false
                            newSiteName = ""
                            newSiteAddress = ""
                        }
                    },
                    enabled = newSiteName.trim().isNotEmpty()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог редактирования объекта
    editSiteId?.let { siteId ->
        AlertDialog(
            onDismissRequest = { editSiteId = null },
            title = { Text("Редактировать объект") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editSiteName,
                        onValueChange = { editSiteName = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSiteAddress,
                        onValueChange = { editSiteAddress = it },
                        label = { Text("Адрес (опционально)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = editSiteName.trim()
                        if (name.isNotEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                val site = db.hierarchyDao().getSite(siteId)
                                if (site != null) {
                                    val updatedSite = site.copy(
                                        name = name,
                                        address = editSiteAddress.trim().takeIf { it.isNotEmpty() },
                                        updatedAtEpoch = System.currentTimeMillis(),
                                        dirtyFlag = true, // Помечаем как требующий синхронизации
                                        syncStatus = 1 // QUEUED
                                    )
                                    db.hierarchyDao().upsertSite(updatedSite)
                                }
                            }
                            editSiteId = null
                        }
                    },
                    enabled = editSiteName.trim().isNotEmpty()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editSiteId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог подтверждения удаления
    deleteSiteId?.let { siteId ->
        AlertDialog(
            onDismissRequest = { deleteSiteId = null },
            title = { Text("Удалить объект?") },
            text = {
                Text("Вы уверены, что хотите удалить этот объект? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().deleteSite(siteId)
                        }
                        deleteSiteId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteSiteId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог выбора иконки объекта
    iconPickerState?.let { state ->
        val site = iconPickerSiteId?.let { siteId ->
            sites.firstOrNull { it.id == siteId }
        }
        IconPickerDialog(
            visible = isIconPickerVisible,
            onDismissRequest = { 
                isIconPickerVisible = false
                iconPickerSiteId = null
            },
            entityType = IconEntityType.SITE,
            packs = state.packs,
            iconsByPack = state.iconsByPack,
            selectedIconId = site?.iconId,
            onIconSelected = { newIconId ->
                iconPickerSiteId?.let { siteId ->
                    scope.launch(Dispatchers.IO) {
                        val siteToUpdate = db.hierarchyDao().getSite(siteId)
                        if (siteToUpdate != null) {
                            val updatedSite = siteToUpdate.copy(
                                iconId = newIconId,
                                updatedAtEpoch = System.currentTimeMillis(),
                                dirtyFlag = true,
                                syncStatus = 1 // QUEUED
                            )
                            db.hierarchyDao().upsertSite(updatedSite)
                            
                            // Обновляем локальное состояние иконки для немедленной перерисовки
                            val icon = if (newIconId != null) {
                                db.iconDao().getById(newIconId)
                            } else {
                                null
                            }
                            siteIcons = siteIcons.toMutableMap().apply {
                                put(siteId, icon)
                            }
                            
                            // Обновляем localImagePath для иконки
                            if (icon != null && icon.imageUrl != null) {
                                val localPath = iconRepository.getLocalIconPath(icon.id)
                                if (localPath == null) {
                                    // Загружаем изображение, если его еще нет
                                    iconRepository.downloadIconImage(icon.id, icon.imageUrl)
                                }
                            }
                        }
                    }
                }
                isIconPickerVisible = false
                iconPickerSiteId = null
            }
        )
    }
}

// Компонент для объекта с drag-and-drop
@Composable
private fun SiteRowWithDrag(
    site: SiteEntity,
    index: Int,
    isArchived: Boolean,
    isCorporate: Boolean,
    icon: ru.wassertech.client.data.entities.IconEntity? = null,
    localImagePath: String? = null,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onChangeIcon: (() -> Unit)? = null,
    reorderableState: ReorderableState? = null,
    onClick: (() -> Unit)? = null,
    isEditing: Boolean = false,
    isDragging: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var hasTriggeredEditMode by remember { mutableStateOf(false) }
    
    // Автоматически включаем режим редактирования, когда начинается перетаскивание
    LaunchedEffect(isDragging, isEditing) {
        if (isDragging && !isEditing && !hasTriggeredEditMode && onToggleEdit != null) {
            hasTriggeredEditMode = true
            onToggleEdit()
        }
        if (!isDragging || isEditing) {
            hasTriggeredEditMode = false
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isEditing && onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ручка для перетаскивания (только в режиме редактирования и для неархивных)
        if (isEditing && !isArchived) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Перетащить",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (reorderableState != null) {
                            Modifier.detectReorder(reorderableState)
                        } else {
                            Modifier
                        }
                    )
            )
            Spacer(Modifier.width(8.dp))
        }
        // Отображаем иконку из БД или дефолтную
        IconResolver.IconImage(
            androidResName = icon?.androidResName,
            entityType = IconEntityType.SITE,
            contentDescription = "Объект",
            size = 48.dp,
            code = icon?.code,
            localImagePath = localImagePath
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${index + 1}. ${site.name}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
            if (site.address != null && site.address.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    site.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Кнопки действий (только в режиме редактирования)
        if (isEditing) {
            if (isArchived) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onRestore) {
                        Icon(
                            Icons.Filled.Unarchive,
                            contentDescription = "Восстановить объект",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                            contentDescription = "Удалить объект",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                // Кнопка изменения иконки
                onChangeIcon?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "Изменить иконку",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                // Кнопка архивации
                IconButton(onClick = onArchive) {
                    Icon(
                        Icons.Filled.Archive,
                        contentDescription = "Архивировать объект",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // Вне режима редактирования показываем иконку навигации
            if (onClick != null) {
                Icon(
                    imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                    contentDescription = "Открыть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

