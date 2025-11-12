@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.wassertech.ui.hierarchy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsApplications
import androidx.compose.material3.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.viewmodel.HierarchyViewModel
import ru.wassertech.viewmodel.TemplatesViewModel
import ru.wassertech.data.entities.ChecklistTemplateEntity
import ru.wassertech.data.types.ComponentType
import ru.wassertech.data.AppDatabase
import ru.wassertech.ui.common.EditDoneBottomBar
import ru.wassertech.ui.common.CommonAddDialog
import ru.wassertech.viewmodel.ClientsViewModel
import ru.wassertech.viewmodel.ClientsViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.core.ui.theme.SegmentedButtonStyle


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
    val allTemplatesFlow: Flow<List<ChecklistTemplateEntity>> =
        remember { db.templatesDao().observeAllTemplates() }
    val allTemplates by allTemplatesFlow.collectAsState(initial = emptyList())
    val templateTitleById = remember(allTemplates) { allTemplates.associate { it.id to it.title } }

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
    var selectedTemplate by remember { mutableStateOf<ChecklistTemplateEntity?>(null) }
    var templateMenu by remember { mutableStateOf(false) }

    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Убираем системные отступы
        floatingActionButton = {
            AppFloatingActionButton(
                template = FABTemplate(
                    icon = Icons.Filled.Add,
                    containerColor = Color(0xFFD32F2F), // Красный цвет
                    contentColor = Color.White,
                    onClick = {
                        showAdd = true
                        newName = TextFieldValue("")
                        selectedTemplate = allTemplates.firstOrNull()
                    }
                )
            )
        }
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
            val metaText: String? = when {
                clientName != null && site?.name != null ->
                    "Объект: $clientName — ${site!!.name}"
                site?.name != null ->
                    "Объект: ${site!!.name}"
                else -> null
            }

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
                        // Иконка убрана по требованию
                        Text(
                            text = instName,
                            style = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                            color = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isEditing) {
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
                                    tint = Color(0xFF1E1E1E) // Иконка на плашке заголовка
                                )
                            }
                        }
                    }
                    metaText?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // Серый цвет для подзаголовка (адрес/объект)
                        )
                    }
                }
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

            // ===== Список компонентов (отрисовываем в порядке localOrder) =====
            val componentsById = remember(components) { components.associateBy { it.id } }
            val orderedComponents = remember(localOrder, componentsById) {
                localOrder.mapNotNull { componentsById[it] }
            }

            if (orderedComponents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет компонентов. Нажмите «Компонент».")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orderedComponents, key = { it.id }) { comp ->
                        val tmplTitle = comp.templateId?.let { templateTitleById[it] } ?: "Без шаблона"
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color(0xFFFFFFFF) // Почти белый фон для карточек
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Увеличенная тень
                        ) {
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = null
                                    )
                                },
                                headlineContent = { Text(comp.name) },
                                supportingContent = { Text(tmplTitle) },
                                trailingContent = {
                                    if (isEditing) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val i = localOrder.indexOf(comp.id)
                                                    if (i > 0) {
                                                        val m = localOrder.toMutableList()
                                                        m[i - 1] = m[i].also { _ -> m[i] = m[i - 1] }
                                                        localOrder = m
                                                    }
                                                }
                                            ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Вверх") }

                                            IconButton(
                                                onClick = {
                                                    val i = localOrder.indexOf(comp.id)
                                                    if (i != -1 && i < localOrder.lastIndex) {
                                                        val m = localOrder.toMutableList()
                                                        m[i + 1] = m[i].also { _ -> m[i] = m[i + 1] }
                                                        localOrder = m
                                                    }
                                                }
                                            ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Вниз") }

                                            Spacer(Modifier.width(4.dp))
                                            IconButton(onClick = { pendingDeleteId = comp.id }) {
                                                Icon(
                                                    imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                                    contentDescription = "Удалить компонент"
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
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
                        val selectedTitle = selectedTemplate?.title
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
                                        text = { Text(tmpl.title) },
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
                else tmpl?.title ?: "Компонент"
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
