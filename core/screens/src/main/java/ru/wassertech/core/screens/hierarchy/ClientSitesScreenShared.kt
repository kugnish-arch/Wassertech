package ru.wassertech.core.screens.hierarchy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.wassertech.core.screens.hierarchy.ui.ClientSitesUiState
import ru.wassertech.core.screens.hierarchy.ui.SiteItemUi
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType

/**
 * Shared-экран для отображения списка объектов клиента.
 * Используется как в app-crm, так и в app-client.
 * 
 * @param state UI State с данными объектов и правами доступа
 * @param onSiteClick Коллбек при клике на объект
 * @param onAddSiteClick Коллбек при клике на добавление объекта
 * @param onSiteArchive Коллбек для архивации объекта
 * @param onSiteRestore Коллбек для восстановления объекта
 * @param onSiteDelete Коллбек для удаления объекта
 * @param onChangeSiteIcon Коллбек для изменения иконки объекта
 * @param onSitesReordered Коллбек при изменении порядка объектов (drag-and-drop)
 * @param isEditing Режим редактирования
 * @param onToggleEdit Коллбек для переключения режима редактирования
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientSitesScreenShared(
    state: ClientSitesUiState,
    onSiteClick: (String) -> Unit,
    onAddSiteClick: () -> Unit,
    onSiteArchive: (String) -> Unit = {},
    onSiteRestore: (String) -> Unit = {},
    onSiteDelete: (String) -> Unit = {},
    onChangeSiteIcon: ((String) -> Unit)? = null,
    onSitesReordered: ((List<String>) -> Unit)? = null,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null
) {
    // Локальный порядок для drag-and-drop
    var localOrder by remember(state.sites.map { it.id }) { 
        mutableStateOf(state.sites.map { it.id }) 
    }
    
    // Синхронизируем локальный порядок с данными из state
    LaunchedEffect(state.sites.map { it.id }) {
        if (!isEditing) {
            localOrder = state.sites.map { it.id }
        }
    }
    
    // Сохраняем порядок при выходе из режима редактирования
    LaunchedEffect(isEditing) {
        if (!isEditing && localOrder.isNotEmpty() && onSitesReordered != null) {
            onSitesReordered(localOrder)
        }
    }
    
    // Состояние диалога удаления
    var deleteDialogState by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (!isEditing && state.canAddSite) {
                FloatingActionButton(
                    onClick = onAddSiteClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить объект")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Заголовок клиента (если нужен)
            // Можно добавить здесь, если требуется
            
            if (state.sites.isEmpty() && !state.isLoading) {
                AppEmptyState(
                    icon = Icons.Filled.Business,
                    title = "Нет объектов",
                    description = "Добавьте первый объект, нажав на кнопку ниже"
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
                ) { siteId, isDragging, reorderableState ->
                    val site = state.sites.find { it.id == siteId } ?: return@ReorderableLazyColumn
                    val index = localOrder.indexOf(siteId)
                    
                    SiteRowShared(
                        site = site,
                        index = index,
                        isDragging = isDragging,
                        reorderableState = reorderableState,
                        isEditing = isEditing,
                        onToggleEdit = onToggleEdit,
                        onClick = if (!isEditing) { { onSiteClick(site.id) } } else null,
                        onArchive = { onSiteArchive(site.id) },
                        onRestore = { onSiteRestore(site.id) },
                        onDelete = { deleteDialogState = Pair(site.id, site.name) },
                        onChangeIcon = if (site.canChangeIcon && isEditing) {
                            { onChangeSiteIcon?.invoke(site.id) }
                        } else null
                    )
                }
            }
        }
    }
    
    // Диалог подтверждения удаления
    deleteDialogState?.let { (siteId, siteName) ->
        AlertDialog(
            onDismissRequest = { deleteDialogState = null },
            title = { Text("Подтверждение удаления") },
            text = {
                Text(
                    "Вы уверены, что хотите удалить объект \"$siteName\"?\n\nЭто действие нельзя отменить."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSiteDelete(siteId)
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
 * Компонент строки объекта для shared-экрана.
 */
@Composable
private fun SiteRowShared(
    site: SiteItemUi,
    index: Int,
    isDragging: Boolean,
    reorderableState: ReorderableState?,
    isEditing: Boolean,
    onToggleEdit: (() -> Unit)?,
    onClick: (() -> Unit)?,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onChangeIcon: (() -> Unit)? = null
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
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (site.isArchived)
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ручка для перетаскивания (только в режиме редактирования и для неархивных)
            if (isEditing && !site.isArchived && site.canReorder) {
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
            
            // Иконка объекта
            IconResolver.IconImage(
                androidResName = site.iconAndroidResName,
                entityType = IconEntityType.SITE,
                contentDescription = "Объект",
                size = 48.dp,
                code = site.iconCode,
                localImagePath = site.iconLocalImagePath
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Название объекта
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${index + 1}. ${site.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (site.isArchived) 
                        MaterialTheme.colorScheme.outline 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                if (!site.address.isNullOrBlank()) {
                    Text(
                        site.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Кнопки действий (только в режиме редактирования)
            if (isEditing) {
                if (site.isArchived) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (site.canEdit) {
                            IconButton(onClick = onRestore) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = "Восстановить объект",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (site.canDelete) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                    contentDescription = "Удалить объект",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    // Кнопка изменения иконки
                    if (site.canChangeIcon && onChangeIcon != null) {
                        IconButton(onClick = onChangeIcon) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Изменить иконку",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // Кнопка архивации
                    if (site.canEdit) {
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
    }
}
