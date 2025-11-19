package ru.wassertech.core.screens.hierarchy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.wassertech.core.screens.hierarchy.ui.SiteInstallationsUiState
import ru.wassertech.core.screens.hierarchy.ui.InstallationItemUi
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.theme.SegmentedButtonStyle

/**
 * Shared-экран для отображения списка установок объекта.
 * Используется как в app-crm, так и в app-client.
 * 
 * @param state UI State с данными установок и правами доступа
 * @param onInstallationClick Коллбек при клике на установку
 * @param onAddInstallationClick Коллбек при клике на добавление установки
 * @param onInstallationArchive Коллбек для архивации установки
 * @param onInstallationRestore Коллбек для восстановления установки
 * @param onInstallationDelete Коллбек для удаления установки
 * @param onChangeInstallationIcon Коллбек для изменения иконки установки
 * @param onInstallationsReordered Коллбек при изменении порядка установок (drag-and-drop)
 * @param onStartMaintenance Коллбек для начала ТО (siteId, installationId, installationName)
 * @param onOpenMaintenanceHistory Коллбек для открытия истории ТО (installationId)
 * @param isEditing Режим редактирования
 * @param onToggleEdit Коллбек для переключения режима редактирования
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteInstallationsScreenShared(
    state: SiteInstallationsUiState,
    onInstallationClick: (String) -> Unit,
    onAddInstallationClick: () -> Unit,
    onInstallationArchive: (String) -> Unit = {},
    onInstallationRestore: (String) -> Unit = {},
    onInstallationDelete: (String) -> Unit = {},
    onChangeInstallationIcon: ((String) -> Unit)? = null,
    onInstallationsReordered: ((List<String>) -> Unit)? = null,
    onStartMaintenance: ((String, String, String) -> Unit)? = null, // siteId, installationId, installationName
    onOpenMaintenanceHistory: ((String) -> Unit)? = null, // installationId
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null
) {
    // Локальный порядок для drag-and-drop
    var localOrder by remember(state.installations.map { it.id }) { 
        mutableStateOf(state.installations.map { it.id }) 
    }
    
    // Синхронизируем локальный порядок с данными из state
    LaunchedEffect(state.installations.map { it.id }) {
        if (!isEditing) {
            localOrder = state.installations.map { it.id }
        }
    }
    
    // Сохраняем порядок при выходе из режима редактирования
    LaunchedEffect(isEditing) {
        if (!isEditing && localOrder.isNotEmpty() && onInstallationsReordered != null) {
            onInstallationsReordered(localOrder)
        }
    }
    
    // Состояние диалога удаления
    var deleteDialogState by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isEditing && state.canAddInstallation) {
                FloatingActionButton(
                    onClick = onAddInstallationClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить установку")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.installations.isEmpty() && !state.isLoading) {
                AppEmptyState(
                    icon = Icons.Filled.SettingsApplications,
                    title = "Нет установок",
                    description = "Добавьте первую установку, нажав на кнопку ниже"
                )
            } else {
                // Используем ReorderableLazyColumn для drag-and-drop
                ReorderableLazyColumn(
                    items = localOrder,
                    onMove = { fromIndex, toIndex ->
                        if (localOrder.isNotEmpty() && fromIndex in localOrder.indices && toIndex in localOrder.indices) {
                            val mutable = localOrder.toMutableList()
                            val item = mutable.removeAt(fromIndex)
                            mutable.add(toIndex, item)
                            localOrder = mutable
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { installationId, isDragging, reorderableState ->
                    val installation = state.installations.find { it.id == installationId } 
                        ?: return@ReorderableLazyColumn
                    val index = localOrder.indexOf(installationId)
                    
                    InstallationRowShared(
                        installation = installation,
                        index = index,
                        siteId = state.siteId,
                        isDragging = isDragging,
                        reorderableState = reorderableState,
                        isEditing = isEditing,
                        onToggleEdit = onToggleEdit,
                        onClick = if (!isEditing) { { onInstallationClick(installation.id) } } else null,
                        onArchive = { onInstallationArchive(installation.id) },
                        onRestore = { onInstallationRestore(installation.id) },
                        onDelete = { deleteDialogState = Pair(installation.id, installation.name) },
                        onChangeIcon = if (installation.canChangeIcon && isEditing) {
                            { onChangeInstallationIcon?.invoke(installation.id) }
                        } else null,
                        onStartMaintenance = if (installation.canStartMaintenance && !isEditing) {
                            { onStartMaintenance?.invoke(state.siteId, installation.id, installation.name) }
                        } else null,
                        onOpenMaintenanceHistory = if (installation.canViewMaintenanceHistory && !isEditing) {
                            { onOpenMaintenanceHistory?.invoke(installation.id) }
                        } else null
                    )
                }
            }
        }
    }
    
    // Диалог подтверждения удаления
    deleteDialogState?.let { (installationId, installationName) ->
        AlertDialog(
            onDismissRequest = { deleteDialogState = null },
            title = { Text("Подтверждение удаления") },
            text = {
                Text(
                    "Вы уверены, что хотите удалить установку \"$installationName\"?\n\nЭто действие нельзя отменить."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onInstallationDelete(installationId)
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

/**
 * Компонент строки установки для shared-экрана.
 */
@Composable
private fun InstallationRowShared(
    installation: InstallationItemUi,
    index: Int,
    siteId: String,
    isDragging: Boolean,
    reorderableState: ReorderableState?,
    isEditing: Boolean,
    onToggleEdit: (() -> Unit)?,
    onClick: (() -> Unit)?,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onChangeIcon: (() -> Unit)? = null,
    onStartMaintenance: (() -> Unit)? = null,
    onOpenMaintenanceHistory: (() -> Unit)? = null
) {
    var hasTriggeredEditMode by remember { mutableStateOf(false) }
    
    // Автоматически включаем режим редактирования при начале перетаскивания
    LaunchedEffect(isDragging, isEditing) {
        if (isDragging && !isEditing && !hasTriggeredEditMode && onToggleEdit != null) {
            hasTriggeredEditMode = true
            onToggleEdit()
        }
        if (!isDragging || isEditing) {
            hasTriggeredEditMode = false
        }
    }
    
    // Состояние для сегментированных кнопок ТО
    var selectedButton by remember(installation.id) { mutableStateOf<Int?>(null) }
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (installation.isArchived)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ручка для перетаскивания (только в режиме редактирования и для неархивных)
                if (isEditing && !installation.isArchived && installation.canReorder) {
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
                
                // Иконка установки
                IconResolver.IconImage(
                    androidResName = installation.iconAndroidResName,
                    entityType = IconEntityType.INSTALLATION,
                    contentDescription = "Установка",
                    size = 48.dp,
                    code = installation.iconCode,
                    localImagePath = installation.iconLocalImagePath
                )
                
                Spacer(Modifier.width(12.dp))
                
                // Название установки
                Text(
                    "${index + 1}. ${installation.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (installation.isArchived) 
                        MaterialTheme.colorScheme.outline 
                    else 
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Кнопки действий (только в режиме редактирования)
                if (isEditing) {
                    if (installation.isArchived) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (installation.canEdit) {
                                IconButton(onClick = onRestore) {
                                    Icon(
                                        Icons.Filled.Unarchive,
                                        contentDescription = "Восстановить установку",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (installation.canDelete) {
                                IconButton(onClick = onDelete) {
                                    Icon(
                                        imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                        contentDescription = "Удалить установку",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Кнопка изменения иконки
                        if (installation.canChangeIcon && onChangeIcon != null) {
                            IconButton(onClick = onChangeIcon) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = "Изменить иконку",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        // Кнопка архивации
                        if (installation.canEdit) {
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
            
            // Сегментированные кнопки для ТО (только вне режима редактирования)
            if (!isEditing && !installation.isArchived && 
                (onStartMaintenance != null || onOpenMaintenanceHistory != null)) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onStartMaintenance != null) {
                        SegmentedButton(
                            selected = selectedButton == 0,
                            onClick = {
                                selectedButton = 0
                                onStartMaintenance()
                            },
                            shape = SegmentedButtonStyle.getShape(index = 0, count = 2),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Провести ТО")
                        }
                    }
                    if (onOpenMaintenanceHistory != null) {
                        SegmentedButton(
                            selected = selectedButton == 1,
                            onClick = {
                                selectedButton = 1
                                onOpenMaintenanceHistory()
                            },
                            shape = SegmentedButtonStyle.getShape(
                                index = if (onStartMaintenance != null) 1 else 0, 
                                count = if (onStartMaintenance != null) 2 else 1
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("История ТО")
                        }
                    }
                }
            }
        }
    }
}

