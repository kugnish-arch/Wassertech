@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.wassertech.ui.hierarchy

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.components.ScreenTitleWithSubtitle
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.theme.ClientsRowDivider
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.viewmodel.HierarchyViewModel
import ru.wassertech.viewmodel.TemplatesViewModel
import ru.wassertech.data.entities.ComponentTemplateEntity
import ru.wassertech.data.types.ComponentType
import ru.wassertech.data.AppDatabase
import ru.wassertech.ui.common.EditDoneBottomBar
import ru.wassertech.ui.common.CommonAddDialog
import ru.wassertech.viewmodel.ClientsViewModel
import ru.wassertech.viewmodel.ClientsViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.core.ui.theme.SegmentedButtonStyle
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.components.IconPickerDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import ru.wassertech.core.screens.hierarchy.InstallationComponentsScreenShared
import ru.wassertech.core.screens.hierarchy.ui.InstallationComponentsUiState
import ru.wassertech.ui.hierarchy.HierarchyUiStateMapper
import ru.wassertech.data.repository.IconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun ComponentsScreen(
    installationId: String,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onStartMaintenance: (String) -> Unit, // пока не используем для одиночного компонента
    onStartMaintenanceAll: (siteId: String, installationName: String) -> Unit,
    onOpenMaintenanceHistoryForInstallation: (String) -> Unit = {},   // навигация в историю ТО
    vm: HierarchyViewModel = viewModel(),
    templatesVm: TemplatesViewModel = viewModel() // оставлено для совместимости
) {
    // --- Templates из БД без VM ---
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val allTemplatesFlow: Flow<List<ComponentTemplateEntity>> =
        remember { db.templatesDao().observeAllTemplates() }
    val allTemplates by allTemplatesFlow.collectAsState(initial = emptyList())
    val templateTitleById = remember(allTemplates) { allTemplates.associate { it.id to it.name } }

    // --- Второй VM: клиенты (чтобы получить имя клиента без observeClient()) ---
    val clientsVm: ClientsViewModel = viewModel(factory = ClientsViewModelFactory(db.clientDao(), db))
    val allClients by clientsVm.clients.collectAsState()

    // --- Данные установки / компонентов ---
    val installation by vm.installation(installationId).collectAsState(initial = null)
    val components by vm.components(installationId).collectAsState(initial = emptyList())

    // Подтянем сайт (нужен clientId). Если у тебя другой метод — поправь здесь.
    val siteId = installation?.siteId
    val site by remember(siteId) {
        if (siteId != null) {
            vm.site(siteId)
        } else {
            flowOf<ru.wassertech.data.entities.SiteEntity?>(null)
        }
    }.collectAsState(initial = null)

    // Имя клиента берём из clientsVm.clients
    val clientName: String? = remember(site, allClients) {
        val id = site?.clientId ?: return@remember null
        allClients.firstOrNull { it.id == id }?.name
    }

    // --- Локальные UI-состояния ---
    // Используем переданное состояние редактирования из топбара
    // локальный порядок (живой) — отрисовываем список по нему
    var localOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Инициализируем localOrder при первой загрузке компонентов
    LaunchedEffect(components) {
        if (localOrder.isEmpty() && components.isNotEmpty()) {
            localOrder = components.map { it.id }
        }
    }
    
    // Отслеживаем переход из режима редактирования для сохранения
    var previousIsEditing by remember { mutableStateOf(isEditing) }
    
    // Обрабатываем изменение isEditing и синхронизацию с компонентами
    LaunchedEffect(isEditing, components.map { it.id }.toSet()) {
        val componentIds = components.map { it.id }
        val componentIdsSet = componentIds.toSet()
        val currentIds = localOrder.toSet()
        
        when {
            // Выходим из режима редактирования - сохраняем порядок
            previousIsEditing && !isEditing -> {
                val orderToSave = localOrder.toList()
                if (orderToSave.isNotEmpty() && components.isNotEmpty()) {
                    // Сохраняем порядок в БД
                    vm.reorderComponents(installationId, orderToSave)
                }
                // Обновляем previousIsEditing после сохранения
                previousIsEditing = isEditing
            }
            // Входим в режим редактирования - фиксируем текущий порядок из БД
            !previousIsEditing && isEditing -> {
                if (components.isNotEmpty()) {
                    localOrder = componentIds
                }
                previousIsEditing = isEditing
            }
            // Не в режиме редактирования - синхронизируем только если набор ID изменился
            // Важно: этот блок выполняется только если предыдущие блоки не сработали
            !isEditing && components.isNotEmpty() && previousIsEditing == false -> {
                when {
                    // Набор ID изменился - обновляем localOrder (добавляем новые, удаляем старые)
                    currentIds != componentIdsSet -> {
                        val existingOrder = localOrder.filter { it in componentIdsSet }
                        val newIds = componentIdsSet - currentIds
                        localOrder = existingOrder + newIds.toList()
                    }
                    // localOrder пуст - инициализируем из БД
                    localOrder.isEmpty() -> {
                        localOrder = componentIds
                    }
                    // Набор ID не изменился - НЕ обновляем порядок, чтобы не перезаписать сохраненные изменения
                }
            }
            // Обновляем previousIsEditing для следующего цикла
            else -> {
                if (previousIsEditing != isEditing) {
                    previousIsEditing = isEditing
                }
            }
        }
    }

    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }
    var editSitePickerExpanded by remember { mutableStateOf(false) }
    var editSelectedSiteIndex by remember { mutableStateOf(0) }
    
    // Получаем список всех объектов клиента для выбора в диалоге редактирования
    var allSites by remember { mutableStateOf<List<ru.wassertech.data.entities.SiteEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    // Обновляем список объектов при изменении installation или site
    LaunchedEffect(installation, site, allClients) {
        val clientId = site?.clientId
        if (clientId != null) {
            val sitesList = vm.sites(clientId, includeArchived = false).first()
            allSites = sitesList
            // Обновляем выбранный индекс при изменении списка объектов
            val currentSiteId = installation?.siteId
            if (currentSiteId != null) {
                val index = sitesList.indexOfFirst { it.id == currentSiteId }
                if (index >= 0) {
                    editSelectedSiteIndex = index
                }
            }
        } else {
            allSites = emptyList()
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTemplate by remember { mutableStateOf<ComponentTemplateEntity?>(null) }
    var templateMenu by remember { mutableStateOf(false) }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    
    // Состояние для IconPickerDialog (для компонентов)
    var isIconPickerVisible by remember { mutableStateOf(false) }
    var iconPickerState by remember { mutableStateOf<ru.wassertech.core.ui.icons.IconPickerUiState?>(null) }
    var iconPickerComponentId by remember { mutableStateOf<String?>(null) }
    
    // Состояние для IconPickerDialog (для установки)
    var isIconPickerVisibleForInstallation by remember { mutableStateOf(false) }
    var iconPickerStateForInstallation by remember { mutableStateOf<ru.wassertech.core.ui.icons.IconPickerUiState?>(null) }
    
    // Иконки для компонентов
    val iconRepository = remember { IconRepository(context) }
    var componentIcons by remember {
        mutableStateOf<Map<String, ru.wassertech.data.entities.IconEntity?>>(emptyMap())
    }
    LaunchedEffect(components) {
        scope.launch(Dispatchers.IO) {
            val iconsMap = mutableMapOf<String, ru.wassertech.data.entities.IconEntity?>()
            components.forEach { component ->
                if (component.iconId != null) {
                    val icon = vm.getIcon(component.iconId)
                    iconsMap[component.id] = icon
                } else {
                    iconsMap[component.id] = null
                }
            }
            componentIcons = iconsMap
        }
    }
    
    // Преобразуем в UI State для shared-экрана
    var uiState by remember {
        mutableStateOf<InstallationComponentsUiState?>(null)
    }
    LaunchedEffect(components, componentIcons, templateTitleById) {
        scope.launch(Dispatchers.IO) {
            val items = components.map { component ->
                val icon = componentIcons[component.id]
                val templateName = component.templateId?.let { templateTitleById[it] }
                withContext(Dispatchers.IO) {
                    HierarchyUiStateMapper.run {
                        component.toComponentItemUi(iconRepository, icon, templateName)
                    }
                }
            }
            uiState = InstallationComponentsUiState(
                installationId = installationId,
                installationName = installation?.name ?: "Установка",
                siteName = site?.name,
                clientName = clientName,
                components = items,
                canAddComponent = true, // В CRM всегда можно добавлять
                canEditInstallation = true,
                isLoading = false
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Убираем системные отступы
        // FAB теперь в shared-экране
        // Ботомбар убран - используется переключатель в топбаре
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current

        Column(
            Modifier
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = 0.dp,
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding()
                )
                .fillMaxSize()
        ) {
            // ===== Заголовок установки =====
            val instName = installation?.name?.takeIf { it.isNotBlank() } ?: "Без названия"
            val siteSubtitle: String? = when {
                clientName != null && site?.name != null ->
                    "Объект: $clientName — ${site!!.name}"
                site?.name != null ->
                    "Объект: ${site!!.name}"
                else -> null
            }

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
                            // Иконка установки из БД
                            val installationIcon by vm.icon(installation?.iconId).collectAsState(initial = null)
                            val iconRepository = remember { ru.wassertech.data.repository.IconRepository(context) }
                            val installationIconLocalPath by remember(installationIcon?.id) {
                                kotlinx.coroutines.flow.flow {
                                    val path = installationIcon?.id?.let { iconRepository.getLocalIconPath(it) }
                                    emit(path)
                                }
                            }.collectAsState(initial = null)
                            IconResolver.IconImage(
                                androidResName = installationIcon?.androidResName,
                                entityType = IconEntityType.INSTALLATION,
                                contentDescription = "Установка",
                                size = ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize * 2,
                                code = installationIcon?.code, // Передаем code для fallback
                                localImagePath = installationIconLocalPath // Передаем локальный путь к файлу изображения
                            )
                            Spacer(Modifier.width(8.dp))
                            // Используем ScreenTitleWithSubtitle для текстовой части заголовка
                            ScreenTitleWithSubtitle(
                                title = instName,
                                subtitle = siteSubtitle,
                                titleStyle = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                                subtitleStyle = MaterialTheme.typography.bodySmall,
                                titleColor = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor,
                                subtitleColor = Color.White, // Белый цвет для подзаголовка на графитовом фоне
                                modifier = Modifier.weight(1f)
                            )
                            if (isEditing) {
                                // Кнопка смены иконки установки
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            iconPickerStateForInstallation = vm.loadIconPacksAndIconsFor(IconEntityType.INSTALLATION)
                                            isIconPickerVisibleForInstallation = true
                                        }
                                    },
                                    enabled = installation != null
                                ) {
                                    Icon(
                                        Icons.Filled.Image,
                                        contentDescription = "Изменить иконку",
                                        tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                    )
                                }
                                // Кнопка редактирования установки
                                IconButton(
                                    onClick = {
                                        editName = TextFieldValue(installation?.name ?: "")
                                        // Находим индекс текущего объекта в списке всех объектов
                                        val currentSiteId = installation?.siteId
                                        editSelectedSiteIndex = allSites.indexOfFirst { it.id == currentSiteId }
                                            .takeIf { it >= 0 } ?: 0
                                        showEdit = true
                                    },
                                    enabled = installation != null
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Редактировать установку",
                                        tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                    )
                                }
                            }
                        }
                    }
                }
                // Бордер снизу как у групп клиентов
                HorizontalDivider(
                    color = ru.wassertech.core.ui.theme.HeaderCardStyle.borderColor,
                    thickness = ru.wassertech.core.ui.theme.HeaderCardStyle.borderThickness
                )
            }

            // ===== Кнопки действий под заголовком (SegmentedButtons) =====
            Spacer(Modifier.height(8.dp))
            val firstComp = components.firstOrNull()
            var selectedButton by remember { mutableStateOf<Int?>(null) }
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SegmentedButton(
                    selected = selectedButton == 0,
                    onClick = {
                        selectedButton = 0
                        val inst = installation
                        if (inst != null && firstComp != null) {
                            onStartMaintenanceAll(inst.siteId, inst.name ?: "")
                        }
                    },
                    enabled = installation != null && firstComp != null,
                    shape = SegmentedButtonStyle.getShape(index = 0, count = 2),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Провести ТО")
                }
                SegmentedButton(
                    selected = selectedButton == 1,
                    onClick = {
                        selectedButton = 1
                        onOpenMaintenanceHistoryForInstallation(installationId)
                    },
                    shape = SegmentedButtonStyle.getShape(index = 1, count = 2),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("История ТО")
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== Список компонентов через shared-экран =====
            uiState?.let { state ->
                Box(modifier = Modifier.weight(1f)) {
                    InstallationComponentsScreenShared(
                        state = state,
                        onComponentClick = null, // Компоненты не кликабельны в CRM
                        onAddComponentClick = {
                            showAdd = true
                            newName = TextFieldValue("")
                            selectedTemplate = allTemplates.firstOrNull()
                        },
                        onComponentArchive = { _ -> }, // Архивирование не используется для компонентов в CRM
                        onComponentRestore = { _ -> }, // Восстановление не используется для компонентов в CRM
                        onComponentDelete = { componentId ->
                            pendingDeleteId = componentId
                        },
                        onChangeComponentIcon = { componentId ->
                            scope.launch {
                                iconPickerComponentId = componentId
                                iconPickerState = vm.loadIconPacksAndIconsFor(IconEntityType.COMPONENT)
                                isIconPickerVisible = true
                            }
                        },
                        onComponentsReordered = { newOrder ->
                            vm.reorderComponents(installationId, newOrder)
                        },
                        isEditing = isEditing,
                        onToggleEdit = onToggleEdit
                    )
                }
            }
        }
    }

    // ===== Диалог переименования установки =====
    if (showEdit && installation != null) {
        CommonAddDialog(
            title = "Редактировать установку",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Название установки") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = editSitePickerExpanded,
                        onExpandedChange = { editSitePickerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = allSites.getOrNull(editSelectedSiteIndex)?.name ?: "Выберите объект",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Объект") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editSitePickerExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = editSitePickerExpanded,
                            onDismissRequest = { editSitePickerExpanded = false },
                            modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground) // Практически белый фон для выпадающих меню
                        ) {
                            allSites.forEachIndexed { index, s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        editSelectedSiteIndex = index
                                        editSitePickerExpanded = false
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
            onDismissRequest = { showEdit = false },
            confirmText = "Сохранить",
            onConfirm = {
                val newTitle = editName.text.trim()
                val newSiteId = allSites.getOrNull(editSelectedSiteIndex)?.id
                if (newTitle.isNotEmpty() && newSiteId != null) {
                    vm.updateInstallation(installationId, newTitle, newSiteId)
                }
                showEdit = false
            },
            confirmEnabled = editName.text.trim().isNotEmpty() && allSites.getOrNull(editSelectedSiteIndex)?.id != null
        )
    }

    // ===== Диалог добавления компонента =====
    if (showAdd) {
        CommonAddDialog(
            title = "Новый компонент",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Название (опц.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(
                        expanded = templateMenu,
                        onExpandedChange = { templateMenu = it }
                    ) {
                        val selectedTitle = selectedTemplate?.name
                            ?: if (allTemplates.isEmpty()) "Нет шаблонов" else "Выберите шаблон"
                        OutlinedTextField(
                            value = selectedTitle,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Шаблон") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateMenu) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = templateMenu,
                            onDismissRequest = { templateMenu = false },
                            modifier = Modifier.background(ru.wassertech.core.ui.theme.DropdownMenuBackground) // Практически белый фон для выпадающих меню
                        ) {
                            if (allTemplates.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Шаблонов нет") },
                                    onClick = { templateMenu = false },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            } else {
                                allTemplates.forEach { tmpl ->
                                    DropdownMenuItem(
                                        text = { Text(tmpl.name) },
                                        onClick = {
                                            selectedTemplate = tmpl
                                            templateMenu = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            onDismissRequest = { showAdd = false },
            onConfirm = {
                val tmpl = selectedTemplate
                val compName = if (newName.text.isNotBlank()) newName.text.trim()
                else tmpl?.name ?: "Компонент"
                vm.addComponentFromTemplate(
                    installationId = installationId,
                    name = compName,
                    type = ComponentType.COMMON, // дефолтный тип
                    templateId = tmpl?.id
                )
                showAdd = false
            },
            confirmEnabled = selectedTemplate != null || allTemplates.isNotEmpty()
        )
    }
    
    // ===== Диалог выбора иконки компонента =====
    iconPickerState?.let { state ->
        val component = iconPickerComponentId?.let { compId ->
            components.firstOrNull { it.id == compId }
        }
        IconPickerDialog(
            visible = isIconPickerVisible,
            onDismissRequest = { 
                isIconPickerVisible = false
                iconPickerComponentId = null
                iconPickerState = null
            },
            entityType = IconEntityType.COMPONENT,
            packs = state.packs,
            iconsByPack = state.iconsByPack,
            selectedIconId = component?.iconId,
            onIconSelected = { newIconId ->
                iconPickerComponentId?.let { compId ->
                    vm.updateComponentIcon(compId, newIconId)
                }
                isIconPickerVisible = false
                iconPickerComponentId = null
                iconPickerState = null
            }
        )
    }
    
    // ===== Диалог выбора иконки установки =====
    iconPickerStateForInstallation?.let { state ->
        IconPickerDialog(
            visible = isIconPickerVisibleForInstallation,
            onDismissRequest = { 
                isIconPickerVisibleForInstallation = false
                iconPickerStateForInstallation = null
            },
            entityType = IconEntityType.INSTALLATION,
            packs = state.packs,
            iconsByPack = state.iconsByPack,
            selectedIconId = installation?.iconId,
            onIconSelected = { newIconId ->
                vm.updateInstallationIcon(installationId, newIconId)
                isIconPickerVisibleForInstallation = false
                iconPickerStateForInstallation = null
            }
        )
    }

    // ===== Диалог подтверждения удаления компонента =====
    pendingDeleteId?.let { compId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Удалить компонент?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteComponent(compId)
                    pendingDeleteId = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Отмена") }
            }
        )
    }
}

/* ---------- Вспомогательные UI-компоненты ---------- */

@Composable
private fun ComponentRowWithEdit(
    component: ru.wassertech.data.entities.ComponentEntity,
    templateTitle: String,
    isEditMode: Boolean,
    icon: ru.wassertech.data.entities.IconEntity? = null,
    onDelete: () -> Unit,
    onChangeIcon: (() -> Unit)? = null,
    isDragging: Boolean,
    reorderableState: ReorderableState?,
    onToggleEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    EntityRowWithMenu(
        title = component.name,
        subtitle = templateTitle,
        leadingIcon = {
            val context = LocalContext.current
            val iconRepository = remember { ru.wassertech.data.repository.IconRepository(context) }
            val componentIconLocalPath by remember(icon?.id) {
                kotlinx.coroutines.flow.flow {
                    val path = icon?.id?.let { iconRepository.getLocalIconPath(it) }
                    emit(path)
                }
            }.collectAsState(initial = null)
            IconResolver.IconImage(
                androidResName = icon?.androidResName,
                entityType = IconEntityType.COMPONENT,
                contentDescription = "Компонент",
                size = 48.dp,
                code = icon?.code, // Передаем code для fallback
                localImagePath = componentIconLocalPath // Передаем локальный путь к файлу изображения
            )
        },
        trailingIcon = null,
        isEditMode = isEditMode,
        isArchived = component.isArchived == true,
        onClick = null, // Компоненты не кликабельны
        onRestore = null,
        onArchive = null,
        onDelete = onDelete,
        onEdit = onChangeIcon, // Используем onEdit для изменения иконки
        onMoveToGroup = null,
        availableGroups = emptyList(),
        modifier = modifier,
        reorderableState = reorderableState,
        showDragHandle = isEditMode && !component.isArchived,
        isDragging = isDragging,
        onToggleEdit = onToggleEdit
    )
}
