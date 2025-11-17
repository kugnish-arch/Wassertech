@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ru.wassertech.ui.hierarchy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.viewmodel.HierarchyViewModel
import kotlinx.coroutines.launch
import ru.wassertech.data.entities.InstallationEntity
import ru.wassertech.ui.common.AppFloatingActionButton
import ru.wassertech.ui.common.FABTemplate
import ru.wassertech.ui.common.CommonAddDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import ru.wassertech.core.ui.R
import ru.wassertech.core.ui.components.EmptyGroupPlaceholder
import ru.wassertech.core.ui.components.EntityRowWithMenu
import ru.wassertech.core.ui.theme.ClientsRowDivider
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.components.IconPickerDialog
import ru.wassertech.core.auth.SessionManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SiteDetailScreen(
    siteId: String,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onOpenInstallation: (String) -> Unit,
    vm: HierarchyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var siteName by remember { mutableStateOf("Объект") }
    var showEdit by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(TextFieldValue("")) }
    var editAddr by remember { mutableStateOf(TextFieldValue("")) }
    var isSiteArchived by remember { mutableStateOf(false) }
    var isCorporate by remember { mutableStateOf(false) }
    
    // Состояние для иконок
    val site by vm.site(siteId).collectAsState(initial = null)
    
    // Локальное состояние для иконки, которое обновляется явно
    var siteIconId by remember { mutableStateOf<String?>(null) }
    var siteIcon by remember { mutableStateOf<ru.wassertech.data.entities.IconEntity?>(null) }
    
    // Инициализируем siteIconId из site при первой загрузке
    LaunchedEffect(site?.iconId) {
        if (siteIconId != site?.iconId) {
            siteIconId = site?.iconId
            android.util.Log.d("SiteDetailScreen", "site.iconId changed to: ${site?.iconId}")
        }
    }
    
    // Загружаем иконку по siteIconId
    LaunchedEffect(siteIconId) {
        if (siteIconId != null) {
            val icon = withContext(kotlinx.coroutines.Dispatchers.IO) {
                vm.getIcon(siteIconId)
            }
            siteIcon = icon
            android.util.Log.d("SiteDetailScreen", "Loaded icon: id=${icon?.id}, label=${icon?.label}, androidResName=${icon?.androidResName}, code=${icon?.code}")
        } else {
            siteIcon = null
        }
    }
    
    // Состояние для IconPickerDialog (для объекта)
    var isIconPickerVisible by remember { mutableStateOf(false) }
    var iconPickerState by remember { mutableStateOf<ru.wassertech.core.ui.icons.IconPickerUiState?>(null) }
    
    // Состояние для IconPickerDialog (для установок)
    var isIconPickerVisibleForInstallation by remember { mutableStateOf(false) }
    var iconPickerStateForInstallation by remember { mutableStateOf<ru.wassertech.core.ui.icons.IconPickerUiState?>(null) }
    var iconPickerInstallationId by remember { mutableStateOf<String?>(null) }

    val installations: List<InstallationEntity> by vm.installations(siteId)
        .collectAsState(initial = emptyList())

    var showAddInst by remember { mutableStateOf(false) }
    var newInstName by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(siteId) {
        val s = vm.getSite(siteId)
        if (s != null) {
            siteName = s.name
            editName = TextFieldValue(s.name)
            editAddr = TextFieldValue(s.address ?: "")
            isSiteArchived = s.isArchived == true
            
            // Получаем информацию о клиенте для определения типа
            val client = vm.getClient(s.clientId)
            isCorporate = client?.isCorporate ?: false
        }
    }
    
    // Загружаем данные для IconPickerDialog при открытии
    LaunchedEffect(isIconPickerVisible) {
        if (isIconPickerVisible && iconPickerState == null) {
            iconPickerState = vm.loadIconPacksAndIconsFor(IconEntityType.SITE)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Убираем отступы от топБара
        floatingActionButton = {
            AppFloatingActionButton(
                template = FABTemplate(
                    icon = Icons.Filled.Add,
                    containerColor = Color(0xFFD32F2F), // Красный цвет
                    contentColor = Color.White,
                    onClick = { showAddInst = true; newInstName = TextFieldValue("") }
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ======= HEADER =======
            // Шапка в стиле из темы
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
                        // Отображаем иконку из БД или дефолтную
                        // Логирование для отладки
                        LaunchedEffect(site?.iconId, siteIcon?.id) {
                            val icon = siteIcon
                            android.util.Log.d("SiteDetailScreen", 
                                "Header icon: site.iconId=${site?.iconId}, siteIconId=$siteIconId, siteIcon=${if (icon != null) "id=${icon.id}, label=${icon.label}, androidResName=${icon.androidResName}, code=${icon.code}" else "null"}"
                            )
                        }
                        // Логирование параметров перед передачей в IconImage
                        val iconRepository = remember { ru.wassertech.data.repository.IconRepository(context) }
                        val localImagePath by remember(siteIcon?.id) {
                            kotlinx.coroutines.flow.flow {
                                val path = siteIcon?.id?.let { iconRepository.getLocalIconPath(it) }
                                emit(path)
                            }
                        }.collectAsState(initial = null)
                        LaunchedEffect(siteIcon?.id, siteIcon?.androidResName, siteIcon?.code, localImagePath) {
                            android.util.Log.d("SiteDetailScreen", 
                                "IconImage params: androidResName=${siteIcon?.androidResName}, code=${siteIcon?.code}, localImagePath=$localImagePath, siteIcon.id=${siteIcon?.id}"
                            )
                        }
                        IconResolver.IconImage(
                            androidResName = siteIcon?.androidResName,
                            entityType = IconEntityType.SITE,
                            contentDescription = "Объект",
                            size = ru.wassertech.core.ui.theme.HeaderCardStyle.iconSize * 2,
                            code = siteIcon?.code, // Передаем code для fallback
                            localImagePath = localImagePath // Передаем локальный путь к файлу изображения
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            siteName,
                            style = ru.wassertech.core.ui.theme.HeaderCardStyle.titleTextStyle,
                            color = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                        )
                        Spacer(Modifier.weight(1f))
                        // Иконки действий видны только в режиме редактирования
                        if (isEditing) {
                            // Кнопка изменения иконки
                            IconButton(onClick = {
                                scope.launch {
                                    iconPickerState = vm.loadIconPacksAndIconsFor(IconEntityType.SITE)
                                    isIconPickerVisible = true
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = "Изменить иконку",
                                    tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                )
                            }
                            // Кнопка редактирования
                            IconButton(onClick = { showEdit = true }) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Редактировать объект",
                                    tint = ru.wassertech.core.ui.theme.HeaderCardStyle.textColor
                                )
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

            // Содержимое секции установок
            if (installations.isEmpty()) {
                EmptyGroupPlaceholder(
                    text = "У этого объекта пока нет установок",
                    indent = 16.dp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(installations, key = { it.id }) { inst ->
                        val installationIcon by vm.icon(inst.iconId).collectAsState(initial = null)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            InstallationRowWithEdit(
                                installation = inst,
                                isEditMode = isEditing,
                                onClick = { onOpenInstallation(inst.id) },
                                onArchive = { vm.archiveInstallation(inst.id) },
                                onRestore = { vm.restoreInstallation(inst.id) },
                                onDelete = { vm.deleteInstallation(inst.id) },
                                onChangeIcon = if (isEditing) {
                                    {
                                        scope.launch {
                                            iconPickerStateForInstallation = vm.loadIconPacksAndIconsFor(IconEntityType.INSTALLATION)
                                            iconPickerInstallationId = inst.id
                                            isIconPickerVisibleForInstallation = true
                                        }
                                    }
                                } else null,
                                icon = installationIcon,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            )
                            // Разделительная линия между установками (кроме последней)
                            val index = installations.indexOf(inst)
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

    if (showEdit) {
        CommonAddDialog(
            title = "Редактировать объект",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Название") })
                    OutlinedTextField(value = editAddr, onValueChange = { editAddr = it }, label = { Text("Адрес (опц.)") })
                }
            },
            onDismissRequest = { showEdit = false },
            confirmText = "Сохранить",
            onConfirm = {
                scope.launch {
                    val s = vm.getSite(siteId)
                    if (s != null) {
                        vm.editSite(s.copy(name = editName.text.trim(), address = editAddr.text.trim().ifEmpty { null }))
                        siteName = editName.text.trim()
                    }
                    showEdit = false
                }
            },
            confirmEnabled = editName.text.trim().isNotEmpty()
        )
    }

    if (showAddInst) {
        CommonAddDialog(
            title = "Новая установка",
            text = {
                OutlinedTextField(
                    value = newInstName,
                    onValueChange = { newInstName = it },
                    label = { Text("Название установки") },
                    singleLine = true
                )
            },
            onDismissRequest = { showAddInst = false },
            onConfirm = {
                val name = newInstName.text.trim().ifBlank { "Новая установка" }
                vm.addInstallation(siteId, name)
                showAddInst = false
            },
            confirmEnabled = newInstName.text.trim().isNotEmpty()
        )
    }
    
    // Диалог выбора иконки объекта
    iconPickerState?.let { state ->
        IconPickerDialog(
            visible = isIconPickerVisible,
            onDismissRequest = { 
                isIconPickerVisible = false
                iconPickerState = null
            },
            entityType = IconEntityType.SITE,
            packs = state.packs,
            iconsByPack = state.iconsByPack,
            selectedIconId = site?.iconId,
            onIconSelected = { newIconId ->
                android.util.Log.d("SiteDetailScreen", "onIconSelected called: newIconId=$newIconId, siteId=$siteId")
                vm.updateSiteIcon(siteId, newIconId)
                // Явно обновляем локальное состояние иконки
                siteIconId = newIconId
                isIconPickerVisible = false
                iconPickerState = null
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
                    vm.updateInstallationIcon(instId, newIconId)
                }
                isIconPickerVisibleForInstallation = false
                iconPickerInstallationId = null
                iconPickerStateForInstallation = null
            }
        )
    }
}

/* ---------- Вспомогательные UI-компоненты ---------- */

@Composable
private fun InstallationRowWithEdit(
    installation: InstallationEntity,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onChangeIcon: (() -> Unit)? = null,
    icon: ru.wassertech.data.entities.IconEntity? = null,
    modifier: Modifier = Modifier
) {
    EntityRowWithMenu(
        title = installation.name,
        subtitle = null,
        leadingIcon = {
            val context = LocalContext.current
            val iconRepository = remember { ru.wassertech.data.repository.IconRepository(context) }
            val installationIconLocalPath by remember(icon?.id) {
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
                localImagePath = installationIconLocalPath // Передаем локальный путь к файлу изображения
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
        isArchived = installation.isArchived == true,
        onClick = onClick,
        onRestore = onRestore,
        onArchive = onArchive,
        onDelete = onDelete,
        onEdit = null, // Редактирование установки пока не поддерживается на этом экране
        onChangeIcon = onChangeIcon,
        onMoveToGroup = null,
        availableGroups = emptyList(),
        modifier = modifier,
        reorderableState = null,
        showDragHandle = false
    )
}
