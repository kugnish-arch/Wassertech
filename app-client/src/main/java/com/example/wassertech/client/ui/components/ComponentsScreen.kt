package ru.wassertech.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.WindowInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.ComponentEntity
import ru.wassertech.client.data.entities.ComponentTemplateEntity
import ru.wassertech.client.data.entities.InstallationEntity
import ru.wassertech.client.data.entities.SiteEntity
import ru.wassertech.client.data.types.ComponentType
import ru.wassertech.core.auth.SessionManager
import ru.wassertech.core.auth.OriginType
import ru.wassertech.client.permissions.canCreateEntity
import ru.wassertech.client.permissions.canEditComponent
import ru.wassertech.client.permissions.canDeleteComponent
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.components.ScreenTitleWithSubtitle
import ru.wassertech.core.ui.theme.ClientsRowDivider
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.icons.IconPickerUiState
import ru.wassertech.core.ui.components.IconPickerDialog
import ru.wassertech.client.permissions.canChangeIconForComponent
import ru.wassertech.client.data.repository.IconRepository
import androidx.compose.material.icons.filled.Image
import kotlinx.coroutines.flow.map
import java.util.UUID
import ru.wassertech.core.screens.hierarchy.InstallationComponentsScreenShared
import ru.wassertech.core.screens.hierarchy.ui.InstallationComponentsUiState
import ru.wassertech.client.ui.hierarchy.ClientHierarchyUiStateMapper
import ru.wassertech.client.data.entities.toUserMembershipInfoList
import ru.wassertech.client.ui.common.LocalEditingState
import kotlinx.coroutines.withContext

/**
 * Экран компонентов установки для app-client.
 * Показывает список компонентов установки с ограничениями по правам доступа.
 * БЕЗ кнопок "Провести ТО" и генерации PDF (только просмотр истории ТО).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(
    installationId: String,
    @Suppress("UNUSED_PARAMETER") onOpenMaintenanceHistory: (String) -> Unit, // Не используется, оставлен для совместимости
    onNavigateBack: (() -> Unit)? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val iconRepository = remember { IconRepository(context) }
    val scope = rememberCoroutineScope()
    val layoutDir = LocalLayoutDirection.current
    
    // Получаем состояние редактирования из CompositionLocal
    val editingState = LocalEditingState.current
    val isEditing = editingState?.isEditing ?: false
    val onToggleEdit = editingState?.onToggle
    
    // Получаем текущую сессию пользователя
    val currentUser = remember { SessionManager.getInstance(context).getCurrentSession() }
    
    // Получаем данные установки
    val installation by db.hierarchyDao().observeInstallation(installationId).collectAsState(initial = null)
    val installationName = installation?.name ?: "Установка"
    
    // Получаем список компонентов
    val components by db.hierarchyDao().observeComponents(installationId).collectAsState(initial = emptyList())
    
    // Получаем данные объекта и клиента для подзаголовка
    val siteId = installation?.siteId
    val site by remember(siteId) {
        if (siteId != null) {
            db.hierarchyDao().observeSite(siteId)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)
    
    val clientId = site?.effectiveOwnerClientId() ?: site?.clientId
    val client by remember(clientId) {
        if (clientId != null) {
            db.clientDao().observeClientRaw(clientId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    val clientName = client.firstOrNull()?.name
    
    // Формируем подзаголовок
    val siteSubtitle: String? = when {
        clientName != null && site?.name != null -> "Объект: $clientName — ${site!!.name}"
        site?.name != null -> "Объект: ${site!!.name}"
        else -> null
    }
    
    // Логирование для отладки подзаголовка
    LaunchedEffect(site, client, siteSubtitle) {
        android.util.Log.d("ComponentsScreen", "Site: ${site?.name}, Client: $clientName, Subtitle: $siteSubtitle")
    }
    
    // Логирование ID клиента и ID создателя установки для отладки FAB
    LaunchedEffect(installation, currentUser) {
        val currentInstallation = installation
        val currentUserId = currentUser?.userId
        val currentClientId = currentUser?.clientId
        if (currentInstallation != null) {
            android.util.Log.d("ComponentsScreen", "Installation ID: ${currentInstallation.id}")
            android.util.Log.d("ComponentsScreen", "Installation createdByUserId: ${currentInstallation.createdByUserId}")
            android.util.Log.d("ComponentsScreen", "Current user ID: $currentUserId")
            android.util.Log.d("ComponentsScreen", "Current user clientId: $currentClientId")
            android.util.Log.d("ComponentsScreen", "FAB should show: ${currentUserId != null && currentInstallation.createdByUserId == currentUserId}")
        }
    }
    
    // Получаем шаблоны компонентов для отображения названий
    val templates by db.componentTemplatesDao().observeAll().collectAsState(initial = emptyList())
    val templateTitleById = remember(templates) {
        templates.associate { it.id to it.name }
    }
    
    // Получаем membership для текущего пользователя
    val memberships by remember(currentUser?.userId) {
        if (currentUser?.userId != null) {
            db.userMembershipDao().observeForUser(currentUser.userId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())
    val membershipsInfo = remember(memberships) {
        memberships.toUserMembershipInfoList()
    }
    
    // Загружаем иконки для всех компонентов
    var componentIcons by remember {
        mutableStateOf<Map<String, ru.wassertech.client.data.entities.IconEntity?>>(emptyMap())
    }
    LaunchedEffect(components) {
        scope.launch(Dispatchers.IO) {
            val iconsMap = mutableMapOf<String, ru.wassertech.client.data.entities.IconEntity?>()
            components.forEach { component ->
                if (component.iconId != null) {
                    val icon = db.iconDao().getById(component.iconId)
                    iconsMap[component.id] = icon
                } else {
                    iconsMap[component.id] = null
                }
            }
            componentIcons = iconsMap
        }
    }
    
    // Преобразуем в UI State с учётом прав доступа
    var uiState by remember {
        mutableStateOf<InstallationComponentsUiState?>(null)
    }
    LaunchedEffect(components, componentIcons, currentUser, membershipsInfo, installation, site, templateTitleById) {
        if (currentUser != null && installation != null) {
            scope.launch(Dispatchers.IO) {
                val currentUserNonNull = currentUser!!
                val currentInstallation = installation!!
                val currentSite = site
                val componentItems = components.mapNotNull { component ->
                    val icon = componentIcons[component.id]
                    val templateName = component.templateId?.let { templateTitleById[it] }
                    withContext(Dispatchers.IO) {
                        ClientHierarchyUiStateMapper.run {
                            component.toComponentItemUi(iconRepository, currentUserNonNull, membershipsInfo, currentInstallation, currentSite, icon, templateName)
                        }
                    }
                }
                val canAddComponent = currentInstallation.createdByUserId == currentUserNonNull.userId
                uiState = InstallationComponentsUiState(
                    installationId = installationId,
                    installationName = currentInstallation.name,
                    siteName = site?.name,
                    clientName = clientName,
                    components = componentItems,
                    canAddComponent = canAddComponent,
                    canEditInstallation = false, // В app-client не редактируем установки
                    isLoading = false
                )
            }
        }
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newComponentName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<ComponentTemplateEntity?>(null) }
    var templateMenuExpanded by remember { mutableStateOf(false) }
    
    var deleteComponentId by remember { mutableStateOf<String?>(null) }
    
    // Состояние для IconPickerDialog
    var isIconPickerVisible by remember { mutableStateOf(false) }
    var iconPickerState by remember { mutableStateOf<IconPickerUiState?>(null) }
    var iconPickerComponentId by remember { mutableStateOf<String?>(null) }
    
    // Вычисляем отступ для FAB с учетом bottomBar из AppScaffold
    val fabBottomPadding = paddingValues.calculateBottomPadding() + 16.dp // Высота bottomBar + зазор
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            // Показываем FAB только если пользователь может добавлять компоненты
            if (uiState?.canAddComponent == true) {
                FloatingActionButton(
                    onClick = {
                        showAddDialog = true
                        newComponentName = ""
                        selectedTemplate = templates.firstOrNull()
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    shape = CircleShape, // Полностью круглый FAB
                    modifier = Modifier.padding(bottom = fabBottomPadding) // Отступ снизу для учета высоты bottomBar
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить компонент")
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
                    top = 0.dp,
                    bottom = padding.calculateBottomPadding()
                )
        ) {
            // Заголовок установки
            Column(modifier = Modifier.fillMaxWidth()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = ru.wassertech.core.ui.theme.HeaderCardStyle.backgroundColor
                    ),
                    shape = ru.wassertech.core.ui.theme.HeaderCardStyle.shape
                ) {
                    Column(Modifier.padding(ru.wassertech.core.ui.theme.HeaderCardStyle.padding)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Загружаем иконку установки
                            val installationIcon by db.iconDao().observeAllActive().map { icons ->
                                installation?.iconId?.let { iconId -> icons.firstOrNull { it.id == iconId } }
                            }.collectAsState(initial = null)
                            
                            // Загружаем локальный путь к изображению иконки установки
                            val installationLocalImagePath by remember(installation?.iconId) {
                                kotlinx.coroutines.flow.flow {
                                    val path = installation?.iconId?.let { iconRepository.getLocalIconPath(it) }
                                    emit(path)
                                }
                            }.collectAsState(initial = null)
                            
                            IconResolver.IconImage(
                                androidResName = installationIcon?.androidResName,
                                entityType = IconEntityType.INSTALLATION,
                                contentDescription = "Установка",
                                size = ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize * 2,
                                code = installationIcon?.code, // Передаем code для fallback
                                localImagePath = installationLocalImagePath // Передаем локальный путь к файлу изображения
                            )
                            Spacer(Modifier.width(8.dp))
                            ScreenTitleWithSubtitle(
                                title = installationName,
                                subtitle = siteSubtitle,
                                titleStyle = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                                subtitleStyle = MaterialTheme.typography.bodySmall,
                                titleColor = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor,
                                subtitleColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                    thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Используем shared-экран для списка компонентов
            uiState?.let { state ->
                Box(modifier = Modifier.fillMaxSize()) {
                    InstallationComponentsScreenShared(
                        state = state.copy(
                            siteName = null, // Убираем siteName из заголовка, так как он уже есть в отдельном заголовке
                            clientName = null // Убираем clientName из заголовка, так как он уже есть в отдельном заголовке
                        ),
                        onComponentClick = null, // Компоненты не кликабельны в app-client
                        onAddComponentClick = {
                            showAddDialog = true
                            newComponentName = ""
                            selectedTemplate = templates.firstOrNull()
                        },
                        onComponentArchive = { componentId ->
                            scope.launch(Dispatchers.IO) {
                                db.hierarchyDao().archiveComponent(componentId)
                            }
                        },
                        onComponentRestore = { componentId ->
                            scope.launch(Dispatchers.IO) {
                                db.hierarchyDao().restoreComponent(componentId)
                            }
                        },
                        onComponentDelete = { componentId ->
                            deleteComponentId = componentId
                        },
                        onChangeComponentIcon = { componentId ->
                            if (currentUser != null && site != null) {
                                val component = components.find { it.id == componentId }
                                if (component != null && canChangeIconForComponent(currentUser, component, site)) {
                                    scope.launch(Dispatchers.IO) {
                                        val packs = db.iconPackDao().getAll()
                                        val allIcons = db.iconDao().getAllActive()
                                        val filteredIcons = allIcons.filter { icon ->
                                            icon.entityType == "ANY" || icon.entityType == IconEntityType.COMPONENT.name
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
                                        iconPickerComponentId = componentId
                                        isIconPickerVisible = true
                                    }
                                }
                            }
                        },
                        onComponentsReordered = { newOrder ->
                            scope.launch(Dispatchers.IO) {
                                val currentComponents = db.hierarchyDao().observeComponents(installationId).first()
                                val ordered = newOrder.mapIndexedNotNull { idx, id ->
                                    currentComponents.firstOrNull { it.id == id }?.copy(orderIndex = idx)
                                }
                                if (ordered.isNotEmpty()) {
                                    db.hierarchyDao().reorderComponents(ordered)
                                }
                            }
                        },
                        isEditing = isEditing,
                        onToggleEdit = onToggleEdit
                    )
                }
            }
        }
    }
    
    // Диалог добавления компонента
    if (showAddDialog && installation != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Новый компонент") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newComponentName,
                        onValueChange = { newComponentName = it },
                        label = { Text("Название (опционально)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Выбор шаблона
                    ExposedDropdownMenuBox(
                        expanded = templateMenuExpanded,
                        onExpandedChange = { templateMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTemplate?.name ?: if (templates.isEmpty()) "Нет шаблонов" else "Выберите шаблон",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Шаблон") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = templateMenuExpanded,
                            onDismissRequest = { templateMenuExpanded = false },
                            modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground)
                        ) {
                            if (templates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Шаблонов нет") },
                                    onClick = { templateMenuExpanded = false }
                                )
                            } else {
                                templates.forEach { template ->
                                    DropdownMenuItem(
                                        text = { Text(template.name) },
                                        onClick = {
                                            selectedTemplate = template
                                            templateMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tmpl = selectedTemplate
                        val compName = if (newComponentName.isNotBlank()) {
                            newComponentName.trim()
                        } else {
                            tmpl?.name ?: "Компонент"
                        }
                        if (tmpl != null || templates.isNotEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                val session = ru.wassertech.core.auth.SessionManager.getInstance(context).getCurrentSession()
                                val currentTime = System.currentTimeMillis()
                                val newComponent = ComponentEntity(
                                    id = UUID.randomUUID().toString(),
                                    installationId = installationId,
                                    name = compName,
                                    type = ComponentType.COMMON,
                                    orderIndex = components.size,
                                    templateId = tmpl?.id,
                                    createdAtEpoch = currentTime,
                                    updatedAtEpoch = currentTime,
                                    isArchived = false,
                                    archivedAtEpoch = null,
                                    deletedAtEpoch = null,
                                    dirtyFlag = true, // Помечаем как требующий синхронизации
                                    syncStatus = 1, // QUEUED
                                    ownerClientId = currentUser?.clientId,
                                    origin = OriginType.CLIENT.name,
                                    createdByUserId = session?.userId
                                )
                                db.hierarchyDao().upsertComponent(newComponent)
                            }
                            showAddDialog = false
                            newComponentName = ""
                            selectedTemplate = null
                        }
                    },
                    enabled = selectedTemplate != null || templates.isNotEmpty()
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
    
    // Диалог подтверждения удаления компонента
    deleteComponentId?.let { componentId ->
        AlertDialog(
            onDismissRequest = { deleteComponentId = null },
            title = { Text("Удалить компонент?") },
            text = {
                Text("Вы уверены, что хотите удалить этот компонент? Это действие нельзя отменить.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.hierarchyDao().deleteComponent(componentId)
                        }
                        deleteComponentId = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteComponentId = null }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог выбора иконки компонента
    iconPickerState?.let { state ->
        val component = iconPickerComponentId?.let { compId ->
            components.firstOrNull { it.id == compId }
        }
        IconPickerDialog(
            visible = isIconPickerVisible,
            onDismissRequest = { 
                isIconPickerVisible = false
                iconPickerComponentId = null
            },
            entityType = IconEntityType.COMPONENT,
            packs = state.packs,
            iconsByPack = state.iconsByPack,
            selectedIconId = component?.iconId,
            onIconSelected = { newIconId ->
                iconPickerComponentId?.let { compId ->
                    scope.launch(Dispatchers.IO) {
                        val componentToUpdate = db.hierarchyDao().getComponent(compId)
                        if (componentToUpdate != null) {
                            val updatedComponent = componentToUpdate.copy(
                                iconId = newIconId,
                                updatedAtEpoch = System.currentTimeMillis(),
                                dirtyFlag = true,
                                syncStatus = 1 // QUEUED
                            )
                            db.hierarchyDao().upsertComponent(updatedComponent)
                            // Принудительно обновляем Flow, чтобы UI перерисовался сразу
                            // observeComponents должен автоматически обновиться через Flow
                        }
                    }
                }
                isIconPickerVisible = false
                iconPickerComponentId = null
            }
        )
    }
}

@Composable
private fun ComponentRow(
    component: ComponentEntity,
    templateTitle: String,
    icon: ru.wassertech.client.data.entities.IconEntity? = null,
    localImagePath: String? = null,
    onChangeIcon: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconResolver.IconImage(
            androidResName = icon?.androidResName,
            entityType = IconEntityType.COMPONENT,
            contentDescription = "Компонент",
            size = 48.dp,
            code = icon?.code, // Передаем code для fallback
            localImagePath = localImagePath // Передаем локальный путь к файлу изображения
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                component.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                templateTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Меню действий (если есть действия)
        if (onChangeIcon != null || onDelete != null) {
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
                    if (onChangeIcon != null && onDelete != null) {
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

