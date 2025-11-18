package ru.wassertech.client.ui.sites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.client.data.entities.SiteEntity
import ru.wassertech.core.auth.SessionManager
import ru.wassertech.client.permissions.canCreateEntity
import ru.wassertech.client.permissions.canEditSite
import ru.wassertech.client.permissions.canDeleteSite
import ru.wassertech.client.permissions.canEditInstallation
import ru.wassertech.client.permissions.canDeleteInstallation
import ru.wassertech.core.auth.OriginType
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EmptyGroupPlaceholder
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.components.ScreenTitleWithSubtitle
import ru.wassertech.core.ui.theme.ClientsRowDivider
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.icons.IconPickerUiState
import ru.wassertech.core.ui.components.IconPickerDialog
import ru.wassertech.client.permissions.canChangeIconForSite
import ru.wassertech.client.permissions.canChangeIconForInstallation
import ru.wassertech.client.data.repository.IconRepository
import androidx.compose.material.icons.filled.Image
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Экран деталей объекта для app-client.
 * Показывает список установок внутри объекта с ограничениями по правам доступа.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    siteId: String,
    onOpenInstallation: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val iconRepository = remember { IconRepository(context) }
    val scope = rememberCoroutineScope()
    val layoutDir = LocalLayoutDirection.current
    
    // Получаем текущую сессию пользователя
    val currentUser = remember { SessionManager.getInstance(context).getCurrentSession() }
    
    // Получаем данные объекта
    val site by db.hierarchyDao().observeSite(siteId).collectAsState(initial = null)
    val siteName = site?.name ?: "Объект"
    
    // Получаем список установок
    val installations by db.hierarchyDao().observeInstallations(siteId).collectAsState(initial = emptyList())
    
    // Получаем информацию о клиенте для определения типа
    val clientId = site?.effectiveOwnerClientId() ?: site?.clientId
    val client by remember(clientId) {
        if (clientId != null) {
            db.clientDao().observeClientRaw(clientId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    val isCorporate = client.firstOrNull()?.isCorporate ?: false
    val isSiteArchived = site?.isArchived == true
    val clientName = client.firstOrNull()?.name
    
    // Логирование загрузки клиента
    LaunchedEffect(clientId, client) {
        android.util.Log.d("SiteDetailScreen", "=== Client Loading ===")
        android.util.Log.d("SiteDetailScreen", "clientId: $clientId")
        android.util.Log.d("SiteDetailScreen", "client list size: ${client.size}")
        android.util.Log.d("SiteDetailScreen", "clientName: $clientName")
        if (clientId != null && client.isEmpty()) {
            // Проверяем, может быть клиент архивирован или есть проблема с запросом
            scope.launch(Dispatchers.IO) {
                try {
                    val clientDirect = db.clientDao().getClient(clientId)
                    android.util.Log.d("SiteDetailScreen", "Direct getClient result: ${clientDirect != null}")
                    if (clientDirect != null) {
                        android.util.Log.d("SiteDetailScreen", "Direct client name: ${clientDirect.name}")
                        android.util.Log.d("SiteDetailScreen", "Direct client isArchived: ${clientDirect.isArchived}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SiteDetailScreen", "Error getting client directly", e)
                }
            }
        }
    }
    
    // Формируем подзаголовок
    val siteSubtitle: String? = clientName?.let { "Клиент: $it" }
    
    // Логирование ID клиента и ID создателя объекта для отладки
    LaunchedEffect(site, currentUser, clientName, siteSubtitle) {
        val currentSite = site
        val currentUserId = currentUser?.userId
        val currentClientId = currentUser?.clientId
        android.util.Log.d("SiteDetailScreen", "=== LaunchedEffect ===")
        android.util.Log.d("SiteDetailScreen", "clientName: $clientName")
        android.util.Log.d("SiteDetailScreen", "siteSubtitle: $siteSubtitle")
        if (currentSite != null) {
            android.util.Log.d("SiteDetailScreen", "Site ID: ${currentSite.id}")
            android.util.Log.d("SiteDetailScreen", "Site createdByUserId: ${currentSite.createdByUserId}")
            android.util.Log.d("SiteDetailScreen", "Current user ID: $currentUserId")
            android.util.Log.d("SiteDetailScreen", "Current user clientId: $currentClientId")
            android.util.Log.d("SiteDetailScreen", "Site clientId: ${currentSite.clientId}")
            android.util.Log.d("SiteDetailScreen", "Site effectiveOwnerClientId: ${currentSite.effectiveOwnerClientId()}")
            val shouldShowFab = currentUserId != null && currentSite.createdByUserId == currentUserId
            android.util.Log.d("SiteDetailScreen", "FAB should show: $shouldShowFab")
            android.util.Log.d("SiteDetailScreen", "currentUser != null: ${currentUser != null}")
            android.util.Log.d("SiteDetailScreen", "currentSite != null: ${currentSite != null}")
            android.util.Log.d("SiteDetailScreen", "createdByUserId == userId: ${currentSite.createdByUserId == currentUserId}")
        } else {
            android.util.Log.d("SiteDetailScreen", "Site is null!")
        }
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newInstallationName by remember { mutableStateOf("") }
    
    var editSiteId by remember { mutableStateOf<String?>(null) }
    var editSiteName by remember { mutableStateOf("") }
    var editSiteAddress by remember { mutableStateOf("") }
    
    var deleteInstallationId by remember { mutableStateOf<String?>(null) }
    
    // Состояние для IconPickerDialog (для объекта)
    var isIconPickerVisibleForSite by remember { mutableStateOf(false) }
    var iconPickerStateForSite by remember { mutableStateOf<IconPickerUiState?>(null) }
    
    // Состояние для IconPickerDialog (для установок)
    var isIconPickerVisibleForInstallation by remember { mutableStateOf(false) }
    var iconPickerStateForInstallation by remember { mutableStateOf<IconPickerUiState?>(null) }
    var iconPickerInstallationId by remember { mutableStateOf<String?>(null) }
    
    // Логирование для FAB (вынесено наружу)
    LaunchedEffect(site, currentUser) {
        val currentSite = site
        val shouldShowFab = currentUser != null && currentSite != null && currentSite.createdByUserId == currentUser.userId
        android.util.Log.d("SiteDetailScreen", "=== FAB LaunchedEffect ===")
        android.util.Log.d("SiteDetailScreen", "shouldShowFab: $shouldShowFab")
        android.util.Log.d("SiteDetailScreen", "currentSite != null: ${currentSite != null}")
        android.util.Log.d("SiteDetailScreen", "currentUser != null: ${currentUser != null}")
        if (currentSite != null && currentUser != null) {
            android.util.Log.d("SiteDetailScreen", "createdByUserId: ${currentSite.createdByUserId}")
            android.util.Log.d("SiteDetailScreen", "userId: ${currentUser.userId}")
            android.util.Log.d("SiteDetailScreen", "match: ${currentSite.createdByUserId == currentUser.userId}")
        }
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Показываем FAB только если пользователь является создателем объекта
            val currentSite = site
            if (currentUser != null && currentSite != null && currentSite.createdByUserId == currentUser.userId) {
                FloatingActionButton(
                    onClick = {
                        showAddDialog = true
                        newInstallationName = ""
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить установку")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDir),
                    end = padding.calculateEndPadding(layoutDir),
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
        ) {
            // Заголовок объекта
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
                        // Загружаем иконку объекта
                        val siteIcon by db.iconDao().observeAllActive().map { icons ->
                            site?.iconId?.let { iconId -> icons.firstOrNull { it.id == iconId } }
                        }.collectAsState(initial = null)
                        
                        // Загружаем локальный путь к изображению иконки объекта
                        val siteLocalImagePath by remember(site?.iconId) {
                            kotlinx.coroutines.flow.flow {
                                val path = site?.iconId?.let { iconRepository.getLocalIconPath(it) }
                                emit(path)
                            }
                        }.collectAsState(initial = null)
                        
                        IconResolver.IconImage(
                            androidResName = siteIcon?.androidResName,
                            entityType = IconEntityType.SITE,
                            contentDescription = "Объект",
                            size = ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize * 2,
                            code = siteIcon?.code, // Передаем code для fallback
                            localImagePath = siteLocalImagePath // Передаем локальный путь к файлу изображения
                        )
                        Spacer(Modifier.width(8.dp))
                        // Заголовок и подзаголовок объекта
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = siteName,
                                style = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                                color = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                            )
                            if (siteSubtitle != null && siteSubtitle.isNotBlank()) {
                                Text(
                                    text = siteSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                        // Кнопки действий для объекта
                        val siteForEdit = site
                        if (currentUser != null && siteForEdit != null) {
                            // Кнопка изменения иконки (только если можно менять иконку)
                            if (canChangeIconForSite(currentUser, siteForEdit)) {
                                IconButton(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val packs = db.iconPackDao().getAll()
                                        val allIcons = db.iconDao().getAllActive()
                                        val filteredIcons = allIcons.filter { icon ->
                                            icon.entityType == "ANY" || icon.entityType == IconEntityType.SITE.name
                                        }
                                        val iconsByPack = filteredIcons.groupBy { it.packId }
                                        
                                        // Загружаем localImagePath для каждой иконки (suspend функция)
                                        // Если файл не существует и есть imageUrl, загружаем изображение
                                        val iconsByPackWithPaths = iconsByPack.mapValues { (_, icons) ->
                                            icons.map { icon ->
                                                var localPath = iconRepository.getLocalIconPath(icon.id)
                                                
                                                // Если файл не существует и есть imageUrl, загружаем изображение
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
                                                    code = icon.code, // Передаем code для fallback
                                                    localImagePath = localPath // Загружаем локальный путь через IconRepository
                                                )
                                            }
                                        }
                                        
                                        iconPickerStateForSite = IconPickerUiState(
                                            packs = packs.map { 
                                                ru.wassertech.core.ui.components.IconPackUiData(
                                                    id = it.id,
                                                    name = it.name
                                                )
                                            },
                                            iconsByPack = iconsByPackWithPaths
                                        )
                                        isIconPickerVisibleForSite = true
                                    }
                                }) {
                                    Icon(
                                        Icons.Filled.Image,
                                        contentDescription = "Изменить иконку",
                                        tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                    )
                                }
                            }
                            // Кнопка редактирования объекта
                            if (canEditSite(currentUser, siteForEdit)) {
                                IconButton(onClick = {
                                    editSiteId = siteForEdit.id
                                    editSiteName = siteForEdit.name
                                    editSiteAddress = siteForEdit.address ?: ""
                                }) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Редактировать объект",
                                        tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                    thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                )
            }
            
            // Список установок
            if (installations.isEmpty()) {
                EmptyGroupPlaceholder(
                    text = "У этого объекта пока нет установок",
                    indent = 16.dp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(installations, key = { it.id }) { installation ->
                        // Загружаем иконку установки
                        val installationIcon by db.iconDao().observeAllActive().map { icons ->
                            installation.iconId?.let { iconId -> icons.firstOrNull { it.id == iconId } }
                        }.collectAsState(initial = null)
                        
                        // Загружаем локальный путь к изображению иконки установки
                        val installationLocalImagePath by remember(installation.iconId) {
                            kotlinx.coroutines.flow.flow {
                                val path = installation.iconId?.let { iconRepository.getLocalIconPath(it) }
                                emit(path)
                            }
                        }.collectAsState(initial = null)
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            InstallationRow(
                                installation = installation,
                                icon = installationIcon,
                                localImagePath = installationLocalImagePath,
                                onClick = { onOpenInstallation(installation.id) },
                                onEdit = if (currentUser != null && canEditInstallation(currentUser, installation, site)) {
                                    { /* TODO: Реализовать редактирование установки */ }
                                } else null,
                                onChangeIcon = if (currentUser != null && canChangeIconForInstallation(currentUser, installation, site)) {
                                    {
                                        scope.launch(Dispatchers.IO) {
                                            val packs = db.iconPackDao().getAll()
                                            val allIcons = db.iconDao().getAllActive()
                                            val filteredIcons = allIcons.filter { icon ->
                                                icon.entityType == "ANY" || icon.entityType == IconEntityType.INSTALLATION.name
                                            }
                                            val iconsByPack = filteredIcons.groupBy { it.packId }
                                            iconPickerStateForInstallation = IconPickerUiState(
                                                packs = packs.map { 
                                                    ru.wassertech.core.ui.components.IconPackUiData(
                                                        id = it.id,
                                                        name = it.name
                                                    )
                                                },
                                                iconsByPack = iconsByPack.mapValues { (_, icons) ->
                                                    icons.map { icon ->
                                                        val localPath = iconRepository.getLocalIconPath(icon.id)
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
                                            )
                                            iconPickerInstallationId = installation.id
                                            isIconPickerVisibleForInstallation = true
                                        }
                                    }
                                } else null,
                                onDelete = if (currentUser != null && canDeleteInstallation(currentUser, installation, site)) {
                                    { deleteInstallationId = installation.id }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            )
                            // Разделительная линия между установками (кроме последней)
                            val index = installations.indexOf(installation)
                            if (index >= 0 && index < installations.size - 1) {
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
    
    // Диалог добавления установки
    if (showAddDialog && site != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новая установка") },
            text = {
                OutlinedTextField(
                    value = newInstallationName,
                    onValueChange = { newInstallationName = it },
                    label = { Text("Название установки") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newInstallationName.trim()
                        val siteForCreation = site
                        if (name.isNotEmpty() && siteForCreation != null) {
                            scope.launch(Dispatchers.IO) {
                                val session = ru.wassertech.core.auth.SessionManager.getInstance(context).getCurrentSession()
                                val newInstallation = InstallationEntity(
                                    id = UUID.randomUUID().toString(),
                                    siteId = siteForCreation.id,
                                    name = name,
                                    createdAtEpoch = System.currentTimeMillis(),
                                    updatedAtEpoch = System.currentTimeMillis(),
                                    isArchived = false,
                                    dirtyFlag = true, // Помечаем как созданную локально
                                    syncStatus = 1, // QUEUED
                                    orderIndex = installations.size,
                                    ownerClientId = currentUser?.clientId,
                                    origin = OriginType.CLIENT.name,
                                    createdByUserId = session?.userId
                                )
                                db.hierarchyDao().upsertInstallation(newInstallation)
                            }
                            showAddDialog = false
                            newInstallationName = ""
                        }
                    },
                    enabled = newInstallationName.trim().isNotEmpty() && site != null
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
    editSiteId?.let { id ->
        val siteToEdit = site
        if (siteToEdit != null) {
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
                                    val updatedSite = siteToEdit.copy(
                                        name = name,
                                        address = editSiteAddress.trim().takeIf { it.isNotEmpty() }
                                    )
                                    db.hierarchyDao().upsertSite(updatedSite)
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
    }
    
    // Диалог подтверждения удаления установки
    deleteInstallationId?.let { installationId ->
        AlertDialog(
            onDismissRequest = { deleteInstallationId = null },
            title = { Text("Удалить установку?") },
            text = {
                Text("Вы уверены, что хотите удалить эту установку? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().deleteInstallation(installationId)
                        }
                        deleteInstallationId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteInstallationId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог выбора иконки объекта
    iconPickerStateForSite?.let { state ->
        IconPickerDialog(
            visible = isIconPickerVisibleForSite,
            onDismissRequest = { 
                isIconPickerVisibleForSite = false
                iconPickerStateForSite = null
            },
            entityType = IconEntityType.SITE,
            packs = state.packs,
            iconsByPack = state.iconsByPack,
            selectedIconId = site?.iconId,
            onIconSelected = { newIconId ->
                site?.let { siteToUpdate ->
                    scope.launch(Dispatchers.IO) {
                        val updatedSite = siteToUpdate.copy(
                            iconId = newIconId,
                            updatedAtEpoch = System.currentTimeMillis(),
                            dirtyFlag = true,
                            syncStatus = 1 // QUEUED
                        )
                        db.hierarchyDao().upsertSite(updatedSite)
                        // Принудительно обновляем Flow, чтобы UI перерисовался сразу
                        // Это делается через upsertSite, который должен обновить observeSite
                    }
                }
                isIconPickerVisibleForSite = false
                iconPickerStateForSite = null
            }
        )
    }
    
    // Диалог выбора иконки установки
    iconPickerStateForInstallation?.let { state ->
        val installation = iconPickerInstallationId?.let { instId ->
            installations.firstOrNull { it.id == instId }
        }
        IconPickerDialog(
            visible = isIconPickerVisibleForInstallation,
            onDismissRequest = { 
                isIconPickerVisibleForInstallation = false
                iconPickerInstallationId = null
                iconPickerStateForInstallation = null
            },
            entityType = IconEntityType.INSTALLATION,
            packs = state.packs,
            iconsByPack = state.iconsByPack,
            selectedIconId = installation?.iconId,
            onIconSelected = { newIconId ->
                iconPickerInstallationId?.let { instId ->
                    scope.launch(Dispatchers.IO) {
                        val installationToUpdate = db.hierarchyDao().getInstallation(instId)
                        if (installationToUpdate != null) {
                            val updatedInstallation = installationToUpdate.copy(
                                iconId = newIconId,
                                updatedAtEpoch = System.currentTimeMillis(),
                                dirtyFlag = true,
                                syncStatus = 1 // QUEUED
                            )
                            db.hierarchyDao().upsertInstallation(updatedInstallation)
                            // Принудительно обновляем Flow, чтобы UI перерисовался сразу
                        }
                    }
                }
                isIconPickerVisibleForInstallation = false
                iconPickerInstallationId = null
                iconPickerStateForInstallation = null
            }
        )
    }
}

@Composable
private fun InstallationRow(
    installation: InstallationEntity,
    icon: ru.wassertech.client.data.entities.IconEntity? = null,
    localImagePath: String? = null,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onChangeIcon: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconResolver.IconImage(
            androidResName = icon?.androidResName,
            entityType = IconEntityType.INSTALLATION,
            contentDescription = "Установка",
            size = 48.dp,
            code = icon?.code, // Передаем code для fallback
            localImagePath = localImagePath // Передаем локальный путь к файлу изображения
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                installation.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (installation.isArchived == true) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground
            )
        }
        Icon(
            imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
            contentDescription = "Открыть",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        // Меню действий (если есть действия)
        if (onEdit != null || onChangeIcon != null || onDelete != null) {
            Spacer(Modifier.width(8.dp))
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Действия",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground)
                ) {
                    onChangeIcon?.let {
                        DropdownMenuItem(
                            text = { Text("Изменить иконку") },
                            onClick = {
                                menuOpen = false
                                it()
                            }
                        )
                    }
                    if (onChangeIcon != null && onEdit != null) {
                        HorizontalDivider()
                    }
                    onEdit?.let {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                menuOpen = false
                                it()
                            }
                        )
                    }
                    if ((onChangeIcon != null || onEdit != null) && onDelete != null) {
                        HorizontalDivider()
                    }
                    onDelete?.let {
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                menuOpen = false
                                it()
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }
    }
}

