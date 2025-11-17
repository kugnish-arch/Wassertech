package ru.wassertech.ui.clients

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import ru.wassertech.core.ui.R
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.viewmodel.HierarchyViewModel
import ru.wassertech.viewmodel.ClientsViewModel
import androidx.compose.ui.platform.LocalContext
import ru.wassertech.data.AppDatabase
import ru.wassertech.ui.icons.AppIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import ru.wassertech.ui.common.EditDoneBottomBar
import ru.wassertech.ui.common.BarAction
import ru.wassertech.data.entities.SiteEntity
import ru.wassertech.data.entities.InstallationEntity
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.ui.common.FABOption
import ru.wassertech.ui.common.CommonAddDialog
import androidx.compose.material.icons.filled.Add
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.components.IconPickerDialog
import androidx.compose.material.icons.filled.Image

private data class SiteDeleteDialogState(
    val isSite: Boolean,
    val id: String,
    val name: String
)

// Компонент для объекта с drag-and-drop
@Composable
fun SiteRowWithDrag(
    site: SiteEntity,
    index: Int,
    isArchived: Boolean,
    isCorporate: Boolean,
    icon: ru.wassertech.data.entities.IconEntity? = null,
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
    onToggleEdit: (() -> Unit)? = null
) {
    var hasTriggeredEditMode by remember { mutableStateOf(false) }
    
    // Автоматически включаем режим редактирования, когда начинается перетаскивание
    // и режим редактирования еще не включен
    LaunchedEffect(isDragging, isEditing) {
        if (isDragging && !isEditing && !hasTriggeredEditMode && onToggleEdit != null) {
            hasTriggeredEditMode = true
            onToggleEdit()
        }
        // Сбрасываем флаг, когда перетаскивание заканчивается или режим редактирования включается
        if (!isDragging || isEditing) {
            hasTriggeredEditMode = false
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isEditing && onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ручка для перетаскивания (только в режиме редактирования и для неархивных) - визуальная подсказка
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
        val context = LocalContext.current
        val iconRepository = remember { ru.wassertech.data.repository.IconRepository(context) }
        val localImagePath by remember(icon?.id) {
            kotlinx.coroutines.flow.flow {
                val path = icon?.id?.let { iconRepository.getLocalIconPath(it) }
                emit(path)
            }
        }.collectAsState(initial = null)
        IconResolver.IconImage(
            androidResName = icon?.androidResName,
            entityType = IconEntityType.SITE,
            contentDescription = "Объект",
            size = 48.dp,
            code = icon?.code, // Передаем code для fallback
            localImagePath = localImagePath // Передаем локальный путь к файлу изображения
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "${index + 1}. ${site.name}",
            style = MaterialTheme.typography.titleMedium,
            color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
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
        }
    }
}

// Компонент для установки с drag-and-drop
@Composable
fun InstallationRowWithDrag(
    installation: InstallationEntity,
    index: Int,
    isArchived: Boolean,
    icon: ru.wassertech.data.entities.IconEntity? = null,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    reorderableState: ReorderableState? = null,
    onClick: (() -> Unit)? = null,
    isEditing: Boolean = false,
    isDragging: Boolean = false,
    onToggleEdit: (() -> Unit)? = null
) {
    var hasTriggeredEditMode by remember { mutableStateOf(false) }
    
    // Автоматически включаем режим редактирования, когда начинается перетаскивание
    // и режим редактирования еще не включен
    LaunchedEffect(isDragging, isEditing) {
        if (isDragging && !isEditing && !hasTriggeredEditMode && onToggleEdit != null) {
            hasTriggeredEditMode = true
            onToggleEdit()
        }
        // Сбрасываем флаг, когда перетаскивание заканчивается или режим редактирования включается
        if (!isDragging || isEditing) {
            hasTriggeredEditMode = false
        }
    }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isArchived)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!isEditing && onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ручка для перетаскивания (только в режиме редактирования и для неархивных) - визуальная подсказка
            if (isEditing && !isArchived) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Перетащить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(20.dp)
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
            val context = LocalContext.current
            val iconRepository = remember { ru.wassertech.data.repository.IconRepository(context) }
            val localImagePath by remember(icon?.id) {
                kotlinx.coroutines.flow.flow {
                    val path = icon?.id?.let { iconRepository.getLocalIconPath(it) }
                    emit(path)
                }
            }.collectAsState(initial = null)
            IconResolver.IconImage(
                androidResName = icon?.androidResName,
                entityType = IconEntityType.INSTALLATION,
                contentDescription = "Установка",
                size = 48.dp,
                code = icon?.code, // Передаем code для fallback
                localImagePath = localImagePath // Передаем локальный путь к файлу изображения
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${index + 1}. ${installation.name}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
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
                            contentDescription = "Восстановить установку",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                            contentDescription = "Удалить установку",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                } else {
                    IconButton(onClick = onArchive) {
                        Icon(
                            Icons.Filled.Archive,
                            contentDescription = "Архивировать установку",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: String,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onOpenSite: (String) -> Unit,
    onOpenInstallation: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val context = LocalContext.current
    val clientDao = AppDatabase.getInstance(context).clientDao()

    // Передаём сам AppDatabase в конструктор ClientsViewModel (если конструктор ожидает параметр 'db')
    val clientsVm: ClientsViewModel = viewModel {
        ClientsViewModel(clientDao, AppDatabase.getInstance(context))
    }

    val groups by clientsVm.groups.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    // Данные
    val sites by vm.sites(clientId).collectAsState(initial = emptyList())

    //Данные клиента
    val client by vm.client(clientId).collectAsState(initial = null)
    val clientName = client?.name ?: "-Клиент-"
    val isCorporate = client?.isCorporate ?: false

    // Режим редактирования - используем переданное состояние из топбара
    var includeArchived by remember { mutableStateOf(false) }
    var includeArchivedBeforeEdit by remember { mutableStateOf<Boolean?>(null) }

    // Синхронизируем локальное состояние с переданным isEditing
    LaunchedEffect(isEditing) {
        if (isEditing) {
            includeArchivedBeforeEdit = includeArchived
            if (!includeArchived) includeArchived = true
        } else {
            if (includeArchivedBeforeEdit == false && includeArchived) {
                includeArchived = false
            }
            includeArchivedBeforeEdit = null
        }
    }

    // Данные с учетом архивирования
    val sitesIncludingArchived by vm.sites(clientId, includeArchived = true)
        .collectAsState(initial = emptyList())
    val sitesToShow = if (includeArchived) sitesIncludingArchived else sites

    // Локальный порядок установок по объектам (siteId -> List<installationId>)
    var localInstallationOrders by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    // Локальный порядок сайтов (для drag-and-drop)
    var localOrder by remember(clientId, sitesToShow) { mutableStateOf(sitesToShow.map { it.id }) }
    LaunchedEffect(sitesToShow, isEditing) {
        if (!isEditing) {
            localOrder = sitesToShow.map { it.id }
        } else {
            // При входе в режим редактирования инициализируем порядок
            localOrder = sitesToShow.map { it.id }
            // Инициализация локального порядка установок
            scope.launch(Dispatchers.IO) {
                val orders = mutableMapOf<String, List<String>>()
                sitesToShow.forEach { site ->
                    // Загружаем установки заново, включая архивные
                    val installations = vm.installations(site.id, includeArchived = true).first()
                    orders[site.id] = installations.map { it.id }
                }
                localInstallationOrders = orders
            }
        }
    }

    // Сохранение порядка при выходе из режима редактирования
    LaunchedEffect(isEditing) {
        if (!isEditing && localOrder.isNotEmpty()) {
            // сохраняем порядок сайтов
            vm.reorderSites(clientId, localOrder)
            // сохраняем порядок установок для каждого объекта
            scope.launch {
                localInstallationOrders.forEach { (siteId: String, orderIds: List<String>) ->
                    vm.reorderInstallations(siteId, orderIds)
                }
            }
        }
    }

    // Получаем все установки заранее для всех сайтов (включая архивные в режиме редактирования)
    var allInstallations by remember {
        mutableStateOf<Map<String, List<InstallationEntity>>>(
            emptyMap()
        )
    }
    LaunchedEffect(sitesToShow, includeArchived, isEditing) {
        scope.launch(Dispatchers.IO) {
            val installationsMap = mutableMapOf<String, List<InstallationEntity>>()
            val shouldIncludeArchived = includeArchived || isEditing
            sitesToShow.forEach { site ->
                val installations =
                    vm.installations(site.id, includeArchived = shouldIncludeArchived).first()
                installationsMap[site.id] = installations
            }
            allInstallations = installationsMap
        }
    }
    
    // Загружаем иконки для всех сайтов
    var siteIcons by remember {
        mutableStateOf<Map<String, ru.wassertech.data.entities.IconEntity?>>(emptyMap())
    }
    LaunchedEffect(sitesToShow) {
        scope.launch(Dispatchers.IO) {
            val iconsMap = mutableMapOf<String, ru.wassertech.data.entities.IconEntity?>()
            sitesToShow.forEach { site ->
                if (site.iconId != null) {
                    val icon = vm.getIcon(site.iconId)
                    iconsMap[site.id] = icon
                } else {
                    iconsMap[site.id] = null
                }
            }
            siteIcons = iconsMap
        }
    }
    
    // Загружаем иконки для всех установок
    var installationIcons by remember {
        mutableStateOf<Map<String, ru.wassertech.data.entities.IconEntity?>>(emptyMap())
    }
    LaunchedEffect(allInstallations) {
        scope.launch(Dispatchers.IO) {
            val iconsMap = mutableMapOf<String, ru.wassertech.data.entities.IconEntity?>()
            allInstallations.values.flatten().forEach { installation ->
                if (installation.iconId != null) {
                    val icon = vm.getIcon(installation.iconId)
                    iconsMap[installation.id] = icon
                } else {
                    iconsMap[installation.id] = null
                }
            }
            installationIcons = iconsMap
        }
    }

    // Диалог подтверждения удаления
    var deleteDialogState by remember { mutableStateOf<SiteDeleteDialogState?>(null) }
    
    // Состояние для IconPickerDialog
    var isIconPickerVisible by remember { mutableStateOf(false) }
    var iconPickerState by remember { mutableStateOf<ru.wassertech.core.ui.icons.IconPickerUiState?>(null) }
    var iconPickerSiteId by remember { mutableStateOf<String?>(null) }

    // Диалоги
    var showAddSite by remember { mutableStateOf(false) }
    var showAddInstallation by remember { mutableStateOf(false) }

    // add site
    var addSiteName by remember { mutableStateOf(TextFieldValue("")) }
    var addSiteAddr by remember { mutableStateOf(TextFieldValue("")) }

    // add installation
    var addInstallationName by remember { mutableStateOf(TextFieldValue("")) }
    var sitePickerExpanded by remember { mutableStateOf(false) }
    var selectedSiteIndex by remember { mutableStateOf(0) }

    // Раскрытие установок по объектам (в обычном режиме)
    var expandedSites by remember { mutableStateOf(setOf<String>()) }

    var showEditClient by remember { mutableStateOf(false) }
    var editClientName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedGroupIndex by remember { mutableStateOf(0) }
    var groupPickerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(showEditClient, client, groups) {
        if (showEditClient && client != null) {
            // имя
            editClientName = TextFieldValue(client!!.name)

            // выбрать индекс группы клиента; если null — ставим "Без группы" (индекс 0, см. ниже)
            val currentGroupId = client!!.clientGroupId
            selectedGroupIndex = if (currentGroupId == null) {
                0 // "Без группы"
            } else {
                // +1 потому что 0 — это "Без группы", дальше идут реальные группы
                val idx = groups.indexOfFirst { it.id == currentGroupId }
                if (idx >= 0) idx + 1 else 0
            }
        }
    }

    Scaffold(
        // Полностью убираем системные отступы — как просили
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        // FAB — только вне режима редактирования
        floatingActionButton = {
            if (!isEditing) {
                AppFloatingActionButton(
                    template = FABTemplate(
                        icon = Icons.Filled.Add,
                        containerColor = Color(0xFFD32F2F), // Красный цвет
                        contentColor = Color.White,
                        onClick = { }, // Не используется, так как есть опции
                        options = listOf(
                            FABOption(
                                label = "Установка",
                                icon = Icons.Filled.SettingsApplications,
                                onClick = { showAddInstallation = true }
                            ),
                            FABOption(
                                label = "Объект",
                                icon = Icons.Filled.Business,
                                onClick = { showAddSite = true }
                            )
                        ),
                        optionsColor = Color(0xFF1E1E1E) // Черный цвет для выпрыгивающих кнопок
                    )
                )
            }
        },

        // Ботомбар убран - используется переключатель в топбаре

    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ======= HEADER =======
            Column(modifier = Modifier.fillMaxWidth()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = ru.wassertech.core.ui.theme.HeaderCardStyle.backgroundColor
                    ),
                    shape = ru.wassertech.core.ui.theme.HeaderCardStyle.shape
                ) {
                    // Плашка имени — используем стиль из темы
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

                        Spacer(Modifier.weight(1f))

                        // Иконка "Редактировать" (✎) — показывается только в режиме редактирования
                        if (isEditing) {
                            IconButton(onClick = {
                                // при открытии диалога сразу заполняем поля текущими данными
                                editClientName = TextFieldValue(clientName)
                                showEditClient = true
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Редактировать клиента",
                                    tint = Color(0xFF1E1E1E) // Иконка на плашке заголовка
                                )
                            }
                        }
                    }
                    // Бордер снизу как у групп клиентов
                    HorizontalDivider(
                        color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                        thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                    )
                }

                // ======= Список объектов/установок =======
                // Используем ReorderableLazyColumn всегда, чтобы detectReorderAfterLongPress мог работать
                ReorderableLazyColumn(
                    items = localOrder,
                    onMove = { fromIndex, toIndex ->
                        // Всегда обновляем локальное состояние для корректного отображения перетаскивания
                        if (localOrder.isNotEmpty() && fromIndex in localOrder.indices && toIndex in localOrder.indices) {
                            val mutable = localOrder.toMutableList()
                            val item = mutable.removeAt(fromIndex)
                            mutable.add(toIndex, item)
                            localOrder = mutable
                        }
                        // Изменения сохраняются в БД только в режиме редактирования
                    },
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { siteId, isDragging, reorderableState ->
                    val site = sitesToShow.find { it.id == siteId } ?: return@ReorderableLazyColumn
                    val index = localOrder.indexOf(siteId)
                    val installations = allInstallations[siteId] ?: emptyList()
                    val installationOrder =
                        localInstallationOrders[siteId] ?: installations.map { it.id }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFFFFFFFF) // Почти белый фон для карточек
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Увеличенная тень
                    ) {
                        Column {
                            // Рядок объекта с ручкой для drag-and-drop
                            SiteRowWithDrag(
                                site = site,
                                index = index,
                                isArchived = site.isArchived,
                                isCorporate = isCorporate,
                                icon = siteIcons[site.id],
                                onMoveUp = {
                                    if (isEditing) {
                                        val pos = localOrder.indexOf(siteId)
                                        if (pos > 0) {
                                            val list = localOrder.toMutableList()
                                            val tmp = list[pos - 1]; list[pos - 1] =
                                                list[pos]; list[pos] = tmp
                                            localOrder = list
                                        }
                                    }
                                },
                                onMoveDown = {
                                    if (isEditing) {
                                        val pos = localOrder.indexOf(siteId)
                                        if (pos >= 0 && pos < localOrder.lastIndex) {
                                            val list = localOrder.toMutableList()
                                            val tmp = list[pos + 1]; list[pos + 1] =
                                                list[pos]; list[pos] = tmp
                                            localOrder = list
                                        }
                                    }
                                },
                                onArchive = { vm.archiveSite(siteId) },
                                onRestore = { vm.restoreSite(siteId) },
                                onDelete = {
                                    deleteDialogState = SiteDeleteDialogState(
                                        isSite = true,
                                        id = siteId,
                                        name = site.name
                                    )
                                },
                                onChangeIcon = if (isEditing) {
                                    {
                                        scope.launch {
                                            iconPickerSiteId = siteId
                                            iconPickerState = vm.loadIconPacksAndIconsFor(IconEntityType.SITE)
                                            isIconPickerVisible = true
                                        }
                                    }
                                } else null,
                                reorderableState = reorderableState,
                                onClick = if (!isEditing) { { onOpenSite(siteId) } } else null,
                                isEditing = isEditing,
                                isDragging = isDragging,
                                onToggleEdit = onToggleEdit
                            )

                            // Установки внутри объекта
                            val installationsToShow =
                                if (site.isArchived) emptyList() else installations
                            // Обновляем локальный порядок, если в нем отсутствуют некоторые установки
                            // НЕ обновляем во время перетаскивания объекта, чтобы избежать прыжков карточек установок
                            LaunchedEffect(siteId, installations.size, isDragging) {
                                if (!isDragging) {
                                    val currentOrder =
                                        localInstallationOrders[siteId] ?: emptyList()
                                    val allIds = installations.map { it.id }
                                    // Добавляем отсутствующие установки в конец
                                    val newOrder =
                                        (currentOrder + allIds.filter { it !in currentOrder }).distinct()
                                    if (newOrder != currentOrder) {
                                        localInstallationOrders =
                                            localInstallationOrders.toMutableMap().apply {
                                                put(siteId, newOrder)
                                            }
                                    }
                                }
                            }
                            if (installationOrder.isNotEmpty() && installationsToShow.isNotEmpty()) {
                                // Используем ReorderableLazyColumn для установок внутри объекта
                                // Фильтруем список установок, чтобы показывать только существующие
                                // Используем стабильный список, который не пересчитывается во время перетаскивания объекта
                                // Используем key для стабильности - список будет пересчитываться только когда изменяется siteId или список установок
                                val installationIdsSet = remember(installations) { installations.map { it.id }.toSet() }
                                val filteredInstallationOrder = remember(siteId, installationOrder, installationIdsSet) {
                                    installationOrder.filter { instId -> instId in installationIdsSet }
                                }
                                ReorderableLazyColumn(
                                    items = filteredInstallationOrder,
                                    onMove = { fromIndex, toIndex ->
                                        // Всегда обновляем локальное состояние для корректного отображения перетаскивания
                                        // Используем текущее значение installationOrder и installations из замыкания
                                        val currentFilteredOrder = installationOrder.filter { instId -> installations.any { it.id == instId } }
                                        if (currentFilteredOrder.isNotEmpty() && fromIndex in currentFilteredOrder.indices && toIndex in currentFilteredOrder.indices) {
                                            val mutable = currentFilteredOrder.toMutableList()
                                            val item = mutable.removeAt(fromIndex)
                                            mutable.add(toIndex, item)
                                            // Восстанавливаем полный порядок с учетом отфильтрованных элементов
                                            val fullOrder = installationOrder.toMutableList()
                                            val filteredIds = currentFilteredOrder.toSet()
                                            val nonFilteredIds = fullOrder.filter { it !in filteredIds }
                                            val newOrder = mutable + nonFilteredIds
                                            localInstallationOrders = localInstallationOrders.toMutableMap().apply {
                                                put(siteId, newOrder)
                                            }
                                        }
                                        // Изменения сохраняются в БД только в режиме редактирования
                                    },
                                    modifier = Modifier
                                        .padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 8.dp
                                        )
                                        .heightIn(max = 400.dp),
                                    key = { it },
                                    contentPadding = PaddingValues(0.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) { instId, isDraggingInstallation, installationReorderableState ->
                                    val inst = installations.find { it.id == instId } ?: return@ReorderableLazyColumn
                                    val instIndex = installationOrder.indexOf(instId)
                                    InstallationRowWithDrag(
                                        installation = inst,
                                        index = instIndex,
                                        isArchived = inst.isArchived,
                                        icon = installationIcons[instId],
                                        onMoveUp = {
                                            if (isEditing) {
                                                val pos = installationOrder.indexOf(instId)
                                                if (pos > 0) {
                                                    val list = installationOrder.toMutableList()
                                                    val tmp = list[pos - 1]; list[pos - 1] = list[pos]; list[pos] = tmp
                                                    localInstallationOrders = localInstallationOrders.toMutableMap().apply {
                                                        put(siteId, list)
                                                    }
                                                }
                                            }
                                        },
                                        onMoveDown = {
                                            if (isEditing) {
                                                val pos = installationOrder.indexOf(instId)
                                                if (pos >= 0 && pos < installationOrder.lastIndex) {
                                                    val list = installationOrder.toMutableList()
                                                    val tmp = list[pos + 1]; list[pos + 1] = list[pos]; list[pos] = tmp
                                                    localInstallationOrders = localInstallationOrders.toMutableMap().apply {
                                                        put(siteId, list)
                                                    }
                                                }
                                            }
                                        },
                                        onArchive = { vm.archiveInstallation(instId) },
                                        onRestore = { vm.restoreInstallation(instId) },
                                        onDelete = {
                                            deleteDialogState = SiteDeleteDialogState(
                                                isSite = false,
                                                id = instId,
                                                name = inst.name
                                            )
                                        },
                                        reorderableState = installationReorderableState,
                                        onClick = if (!isEditing) { { onOpenInstallation(instId) } } else null,
                                        isEditing = isEditing,
                                        isDragging = isDraggingInstallation,
                                        onToggleEdit = onToggleEdit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалог подтверждения удаления
        deleteDialogState?.let { state ->
            AlertDialog(
                onDismissRequest = { deleteDialogState = null },
                title = { Text("Подтверждение удаления") },
                text = {
                    Text(
                        if (state.isSite) {
                            "Вы уверены, что хотите удалить объект \"${state.name}\"?\n\nЭто действие нельзя отменить."
                        } else {
                            "Вы уверены, что хотите удалить установку \"${state.name}\"?\n\nЭто действие нельзя отменить."
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (state.isSite) {
                                vm.deleteSite(state.id)
                            } else {
                                vm.deleteInstallation(state.id)
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

        // ---- Dialogs ----
        if (showAddSite) {
            CommonAddDialog(
                title = "Добавить объект",
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addSiteName,
                            onValueChange = { addSiteName = it },
                            label = { Text("Название объекта") })
                        OutlinedTextField(
                            value = addSiteAddr,
                            onValueChange = { addSiteAddr = it },
                            label = { Text("Адрес (опц.)") })
                    }
                },
                onDismissRequest = { showAddSite = false },
                onConfirm = {
                    val n = addSiteName.text.trim()
                    if (n.isNotEmpty()) {
                        vm.addSite(clientId, n, addSiteAddr.text.trim().ifEmpty { null })
                        addSiteName = TextFieldValue("")
                        addSiteAddr = TextFieldValue("")
                        showAddSite = false
                    }
                },
                confirmEnabled = addSiteName.text.trim().isNotEmpty()
            )
        }

        if (showAddInstallation) {
            CommonAddDialog(
                title = "Добавить установку",
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addInstallationName,
                            onValueChange = { addInstallationName = it },
                            label = { Text("Название установки") })
                        ExposedDropdownMenuBox(
                            expanded = sitePickerExpanded,
                            onExpandedChange = { sitePickerExpanded = it }) {
                            OutlinedTextField(
                                value = sites.getOrNull(selectedSiteIndex)?.name
                                    ?: "Выберите объект",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Объект") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sitePickerExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = sitePickerExpanded,
                                onDismissRequest = { sitePickerExpanded = false },
                                modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground) // Практически белый фон для выпадающих меню
                            ) {
                                sites.forEachIndexed { index, s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            selectedSiteIndex = index
                                            sitePickerExpanded = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                onDismissRequest = { showAddInstallation = false },
                onConfirm = {
                    val n = addInstallationName.text.trim()
                    if (n.isNotEmpty()) {
                        val selectedSiteId = sites.getOrNull(selectedSiteIndex)?.id
                        if (selectedSiteId != null) vm.addInstallationToSite(selectedSiteId, n)
                        else vm.addInstallationToMain(clientId, n)
                        addInstallationName = TextFieldValue("")
                        showAddInstallation = false
                    }
                },
                confirmEnabled = addInstallationName.text.trim().isNotEmpty()
            )
        }

        if (showEditClient) {
            val clientGroups by clientsVm.groups.collectAsState(initial = emptyList())

            // Опции групп: [0] — "Без группы", дальше реальные группы
            val groupOptions = remember(clientGroups) {
                buildList<Pair<String?, String>> {
                    add(null to "Без группы")
                    addAll(clientGroups.map { it.id to it.title })
                }
            }
            AlertDialog(
                onDismissRequest = { showEditClient = false },
                title = { Text("Редактировать клиента") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editClientName,
                            onValueChange = { editClientName = it },
                            label = { Text("Имя / название клиента") },
                            singleLine = true
                        )

                        // Выбор группы


                        ExposedDropdownMenuBox(
                            expanded = groupPickerExpanded,
                            onExpandedChange = { groupPickerExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = groupOptions.getOrNull(selectedGroupIndex)?.second
                                    ?: "Без группы",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Группа") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupPickerExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = groupPickerExpanded,
                                onDismissRequest = { groupPickerExpanded = false },
                                modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground) // Практически белый фон для выпадающих меню
                            ) {
                                groupOptions.forEachIndexed { index, pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.second) },
                                        onClick = {
                                            selectedGroupIndex = index
                                            groupPickerExpanded = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }

                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        client?.let { current ->
                            val newName = editClientName.text.trim()
                            val selectedGroupId = groupOptions.getOrNull(selectedGroupIndex)?.first

                            // имя
                            clientsVm.editClient(current.copy(name = newName))

                            // группа (включая переход в "Без группы" при null)
                            if (selectedGroupId != current.clientGroupId) {
                                clientsVm.assignClientToGroup(current.id, selectedGroupId)
                            }
                        }
                        showEditClient = false
                    }) { Text("Сохранить") }

                },
                dismissButton = {
                    TextButton(onClick = { showEditClient = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
        
        // ===== Диалог выбора иконки объекта =====
        iconPickerState?.let { state ->
            val site = iconPickerSiteId?.let { siteId ->
                sitesToShow.firstOrNull { it.id == siteId }
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
                        vm.updateSiteIcon(siteId, newIconId)
                        // Обновляем локальное состояние иконки
                        scope.launch(Dispatchers.IO) {
                            val icon = if (newIconId != null) vm.getIcon(newIconId) else null
                            siteIcons = siteIcons.toMutableMap().apply {
                                put(siteId, icon)
                            }
                        }
                    }
                    isIconPickerVisible = false
                    iconPickerSiteId = null
                }
            )
        }
    }
}
