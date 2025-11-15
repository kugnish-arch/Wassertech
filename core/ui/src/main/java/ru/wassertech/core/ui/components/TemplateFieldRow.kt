package ru.wassertech.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder

/**
 * Универсальная строка поля шаблона для отображения в списке.
 * Используется для показа полей шаблона компонента с поддержкой drag-n-drop.
 *
 * @param title Название поля (label)
 * @param subtitle Подзаголовок (например, тип поля или единицы измерения)
 * @param fieldTypeLabel Текстовая метка типа поля (например, "TXT", "CHK", "NUM")
 * @param isRequired Обязательное ли поле
 * @param isForMaintenance Используется ли поле для ТО (если false - характеристика)
 * @param isCharacteristic Является ли поле характеристикой (альтернативное название для isForMaintenance)
 * @param isArchived Архивировано ли поле
 * @param index Индекс поля в списке (для отображения номера)
 * @param onClick Обработчик клика (используется только в обычном режиме)
 * @param onDelete Обработчик удаления
 * @param onArchive Обработчик архивации
 * @param onRestore Обработчик восстановления из архива
 * @param modifier Модификатор для применения к компоненту
 * @param reorderableState Состояние для drag-n-drop (если null, drag-n-drop отключен)
 * @param showDragHandle Показывать ли drag handle (иконку меню для перетаскивания)
 * @param showIndex Показывать ли номер поля
 */
@Composable
fun TemplateFieldRow(
    title: String,
    subtitle: String? = null,
    fieldTypeLabel: String = "",
    isRequired: Boolean = false,
    isForMaintenance: Boolean = true,
    isCharacteristic: Boolean = false,
    isArchived: Boolean = false,
    index: Int? = null,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    reorderableState: ReorderableState? = null,
    showDragHandle: Boolean = false,
    showIndex: Boolean = false
) {
    val isEditMode = onDelete != null || onArchive != null || onRestore != null
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle и номер
        if (isEditMode && !isArchived && showDragHandle) {
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
        
        if (showIndex && index != null) {
            Text(
                "${index + 1}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
        }
        
        // Основной контент
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground
                )
                
                if (fieldTypeLabel.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            fieldTypeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (isRequired) {
                    Text(
                        "*",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (subtitle != null && subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Индикаторы типа поля
                    if (!isForMaintenance || isCharacteristic) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Характеристика",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Действия справа
        if (isEditMode) {
            if (isArchived) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onRestore?.let {
                        IconButton(onClick = it) {
                            Icon(
                                Icons.Filled.Unarchive,
                                contentDescription = "Восстановить",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    onDelete?.let {
                        IconButton(onClick = it) {
                            Icon(
                                imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                onDelete?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = ru.wassertech.core.ui.theme.DeleteIcon,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else if (onClick != null) {
            Icon(
                imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
