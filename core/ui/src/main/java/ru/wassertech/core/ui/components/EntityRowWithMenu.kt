package ru.wassertech.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.reorderable.ReorderableState
import ru.wassertech.core.ui.reorderable.detectReorder
import ru.wassertech.core.ui.theme.DeleteIcon
import ru.wassertech.core.ui.theme.DropdownMenuBackground
import ru.wassertech.core.ui.theme.NavigationIcons

/**
 * Универсальная строка сущности с поддержкой drag-n-drop, меню действий и архивации.
 *
 * @param title Основной заголовок строки
 * @param subtitle Подзаголовок (опционально)
 * @param leadingIcon Композиция для отображения ведущей иконки (слева)
 * @param trailingIcon Композиция для отображения завершающей иконки (справа, в обычном режиме)
 * @param isEditMode Режим редактирования
 * @param isArchived Архивирована ли сущность
 * @param onClick Обработчик клика (используется только в обычном режиме)
 * @param onRestore Обработчик восстановления из архива
 * @param onArchive Обработчик архивации
 * @param onDelete Обработчик удаления
 * @param onEdit Обработчик редактирования
 * @param onMoveToGroup Обработчик перемещения в группу (принимает ID группы или null для "Без группы")
 * @param availableGroups Список доступных групп в формате (id, title)
 * @param modifier Модификатор для применения к компоненту
 * @param reorderableState Состояние для drag-n-drop (если null, drag-n-drop отключен)
 * @param showDragHandle Показывать ли drag handle (иконку меню для перетаскивания)
 */
@Composable
fun EntityRowWithMenu(
    title: String,
    subtitle: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isEditMode: Boolean,
    isArchived: Boolean,
    onClick: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onMoveToGroup: ((String?) -> Unit)? = null,
    availableGroups: List<Pair<String, String>> = emptyList(), // (id, title)
    modifier: Modifier = Modifier,
    reorderableState: ReorderableState? = null,
    showDragHandle: Boolean = false
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!isEditMode && onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        leadingIcon?.invoke()
        if (leadingIcon != null) {
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null && subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isEditMode && trailingIcon != null) {
            trailingIcon()
        }

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
                                imageVector = DeleteIcon,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                onEdit?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (onMoveToGroup != null || onArchive != null) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Ещё",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.background(DropdownMenuBackground)
                        ) {
                            if (onMoveToGroup != null) {
                                DropdownMenuItem(
                                    text = { Text("Переместить в: Без группы") },
                                    onClick = {
                                        menuOpen = false
                                        onMoveToGroup(null)
                                    }
                                )
                                if (availableGroups.isNotEmpty()) {
                                    HorizontalDivider()
                                }
                                availableGroups.forEach { (groupId, groupTitle) ->
                                    DropdownMenuItem(
                                        text = { Text("Переместить в: $groupTitle") },
                                        onClick = {
                                            menuOpen = false
                                            onMoveToGroup(groupId)
                                        }
                                    )
                                }
                            }
                            if (onMoveToGroup != null && onArchive != null) {
                                HorizontalDivider()
                            }
                            onArchive?.let {
                                DropdownMenuItem(
                                    text = { Text("Архивировать") },
                                    onClick = {
                                        menuOpen = false
                                        it()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

