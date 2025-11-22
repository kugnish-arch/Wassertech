package ru.wassertech.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.theme.ClientsGroupCollapsedBackground
import ru.wassertech.core.ui.theme.ClientsGroupExpandedBackground
import ru.wassertech.core.ui.theme.ClientsGroupExpandedText
import ru.wassertech.core.ui.theme.ClientsGroupBorder
import ru.wassertech.core.ui.theme.NavigationIcons
import ru.wassertech.core.ui.theme.DeleteIcon

/**
 * Универсальный заголовок группы/секции с поддержкой разворачивания/сворачивания,
 * архивации, редактирования и удаления.
 *
 * @param title Название группы
 * @param count Количество элементов в группе
 * @param isExpanded Развернута ли группа
 * @param isArchived Архивирована ли группа
 * @param canArchive Может ли группа быть архивирована (false для системных групп, например "Общая")
 * @param showActions Показывать ли действия (редактирование, архивация и т.д.)
 * @param onArchive Обработчик архивации (null, если действие недоступно)
 * @param onRestore Обработчик восстановления из архива (null, если действие недоступно)
 * @param onToggle Обработчик переключения развернутости
 * @param onMoveUp Обработчик перемещения вверх (null, если действие недоступно)
 * @param onMoveDown Обработчик перемещения вниз (null, если действие недоступно)
 * @param onEdit Обработчик редактирования (null, если действие недоступно)
 * @param onDelete Обработчик удаления (null, если действие недоступно)
 * @param modifier Модификатор для применения к компоненту
 */
@Composable
fun EntityGroupHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    isArchived: Boolean,
    canArchive: Boolean,
    showActions: Boolean,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onToggle: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bg = if (isExpanded) ClientsGroupExpandedBackground else ClientsGroupCollapsedBackground
    val contentColor =
        if (isExpanded) ClientsGroupExpandedText else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showActions && !isArchived && canArchive && (onMoveUp != null || onMoveDown != null)) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Перетащить",
                    tint = contentColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "$title ($count)",
                color = contentColor,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (showActions) {
                if (!isArchived && canArchive) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        onEdit?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Редактировать",
                                    tint = contentColor
                                )
                            }
                        }
                        onArchive?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    Icons.Filled.Archive,
                                    contentDescription = "Архивировать",
                                    tint = contentColor
                                )
                            }
                        }
                    }
                } else if (isArchived) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        onRestore?.let {
                            IconButton(onClick = it) {
                                Icon(
                                    Icons.Filled.Unarchive,
                                    contentDescription = "Восстановить",
                                    tint = contentColor
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
                }
            }
            Icon(
                imageVector = if (isExpanded) NavigationIcons.CollapseMenuIcon else NavigationIcons.ExpandMenuIcon,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = contentColor
            )
        }
        HorizontalDivider(
            color = ClientsGroupBorder,
            thickness = 1.dp
        )
    }
}





