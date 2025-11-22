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
import ru.wassertech.core.screens.hierarchy.ui.InstallationComponentsUiState
import ru.wassertech.core.screens.hierarchy.ui.ComponentItemUi
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.reorderable.ReorderableLazyColumn
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.icons.IconEntityType

/**
 * Shared-экран для отображения списка компонентов установки.
 * Используется как в app-crm, так и в app-client.
 * 
 * @param state UI State с данными компонентов и правами доступа
 * @param onComponentClick Коллбек при клике на компонент (опционально, если нужны детали)
 * @param onAddComponentClick Коллбек при клике на добавление компонента
 * @param onComponentArchive Коллбек для архивации компонента
 * @param onComponentRestore Коллбек для восстановления компонента
 * @param onComponentDelete Коллбек для удаления компонента
 * @param onChangeComponentIcon Коллбек для изменения иконки компонента
 * @param onComponentsReordered Коллбек при изменении порядка компонентов (drag-and-drop)
 * @param isEditing Режим редактирования
 * @param onToggleEdit Коллбек для переключения режима редактирования
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallationComponentsScreenShared(
    state: InstallationComponentsUiState,
    onComponentClick: ((String) -> Unit)? = null,
    onAddComponentClick: () -> Unit,
    onComponentArchive: (String) -> Unit = {},
    onComponentRestore: (String) -> Unit = {},
    onComponentDelete: (String) -> Unit = {},
    onChangeComponentIcon: ((String) -> Unit)? = null,
    onComponentsReordered: ((List<String>) -> Unit)? = null,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null
) {
    // Локальный порядок для drag-and-drop
    var localOrder by remember(state.components.map { it.id }) { 
        mutableStateOf(state.components.map { it.id }) 
    }
    
    // Синхронизируем локальный порядок с данными из state
    LaunchedEffect(state.components.map { it.id }) {
        if (!isEditing) {
            localOrder = state.components.map { it.id }
        }
    }
    
    // Сохраняем порядок при выходе из режима редактирования
    LaunchedEffect(isEditing) {
        if (!isEditing && localOrder.isNotEmpty() && onComponentsReordered != null) {
            onComponentsReordered(localOrder)
        }
    }
    
    // Состояние диалога удаления
    var deleteDialogState by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
            if (state.components.isEmpty() && !state.isLoading) {
                AppEmptyState(
                    icon = Icons.Filled.Lightbulb,
                    title = "Нет компонентов",
                    description = "Добавьте первый компонент, нажав на кнопку ниже"
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
                ) { componentId, isDragging, reorderableState ->
                    val component = state.components.find { it.id == componentId } 
                        ?: return@ReorderableLazyColumn
                    val index = localOrder.indexOf(componentId)
                    
                    ComponentRowShared(
                        component = component,
                        index = index,
                        isDragging = isDragging,
                        reorderableState = reorderableState,
                        isEditing = isEditing,
                        onToggleEdit = onToggleEdit,
                        onClick = if (!isEditing && onComponentClick != null) { 
                            { onComponentClick(component.id) } 
                        } else null,
                        onArchive = { onComponentArchive(component.id) },
                        onRestore = { onComponentRestore(component.id) },
                        onDelete = { deleteDialogState = Pair(component.id, component.name) },
                        onChangeIcon = if (component.canChangeIcon && isEditing) {
                            { onChangeComponentIcon?.invoke(component.id) }
                        } else null
                    )
                }
            }
        }


    // Диалог подтверждения удаления
    deleteDialogState?.let { (componentId, componentName) ->
        AlertDialog(
            onDismissRequest = { deleteDialogState = null },
            title = { Text("Подтверждение удаления") },
            text = {
                Text(
                    "Вы уверены, что хотите удалить компонент \"$componentName\"?\n\nЭто действие нельзя отменить."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onComponentDelete(componentId)
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
 * Компонент строки компонента для shared-экрана.
 */
@Composable
private fun ComponentRowShared(
    component: ComponentItemUi,
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
            containerColor = if (component.isArchived)
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
            if (isEditing && !component.isArchived && component.canReorder) {
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
            
            // Иконка компонента
            IconResolver.IconImage(
                androidResName = component.iconAndroidResName,
                entityType = IconEntityType.COMPONENT,
                contentDescription = "Компонент",
                size = 48.dp,
                code = component.iconCode,
                localImagePath = component.iconLocalImagePath
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Название и тип компонента
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    component.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (component.isArchived) 
                        MaterialTheme.colorScheme.outline 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        component.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!component.templateName.isNullOrBlank()) {
                        Text(
                            "• ${component.templateName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Отображение температуры для SENSOR компонентов (справа крупными зелеными цифрами)
            if (component.type == "SENSOR" && !isEditing) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = component.temperatureValue?.let { 
                        String.format(java.util.Locale.getDefault(), "%.1f°C", it)
                    } ?: "—°C",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = Color(0xFF4CAF50), // Зеленый цвет
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            
            // Кнопки действий (только в режиме редактирования)
            if (isEditing) {
                if (component.isArchived) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (component.canEdit) {
                            IconButton(onClick = onRestore) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = "Восстановить компонент",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (component.canDelete) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                    contentDescription = "Удалить компонент",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                } else {
                    // Кнопка изменения иконки
                    if (component.canChangeIcon && onChangeIcon != null) {
                        IconButton(onClick = onChangeIcon) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Изменить иконку",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // Кнопка архивации
                    if (component.canEdit) {
                        IconButton(onClick = onArchive) {
                            Icon(
                                Icons.Filled.Archive,
                                contentDescription = "Архивировать компонент",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

