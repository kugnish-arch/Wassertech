package ru.wassertech.core.ui.reorderable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable


/**
 * LazyColumn с поддержкой drag-n-drop для изменения порядка элементов.
 * 
 * Использует библиотеку compose-reorderable для реализации перетаскивания элементов.
 * Перетаскиваемый элемент автоматически отображается поверх остальных с тенью.
 * 
 * @param items Список элементов для отображения
 * @param onMove Callback, вызываемый при перемещении элемента с позиции fromIndex на позицию toIndex
 * @param modifier Модификатор для применения к LazyColumn
 * @param key Функция для получения уникального ключа элемента (обязательный параметр)
 * @param contentPadding Отступы содержимого списка
 * @param verticalArrangement Вертикальное расположение элементов
 * @param itemContent Composable функция для отрисовки элемента списка
 *   - item: элемент списка
 *   - isDragging: true, если элемент в данный момент перетаскивается
 * 
 * @sample
 * ```
 * ReorderableLazyColumn(
 *     items = clients,
 *     onMove = { from, to ->
 *         val mutable = clients.toMutableList()
 *         val item = mutable.removeAt(from)
 *         mutable.add(to, item)
 *         clients = mutable
 *     },
 *     key = { it.id }
 * ) { client, isDragging ->
 *     ClientCard(
 *         client = client,
 *         modifier = Modifier
 *             .zIndex(if (isDragging) 1f else 0f)
 *             .animateItemPlacement()
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ReorderableLazyColumn(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    key: (T) -> Any,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(0.dp),
    itemContent: @Composable (item: T, isDragging: Boolean) -> Unit
) {
    val reorderableState: ReorderableLazyListState = rememberReorderableLazyListState(
        onMove = { from, to ->
            onMove(from.index, to.index)
        }
    )

    
    LazyColumn(
        state = reorderableState.listState,
        modifier = modifier.reorderable(reorderableState),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        items(
            items = items,
            key = { item -> key(item) }
        ) { item ->
            ReorderableItem(
                reorderableState = reorderableState,
                key = key(item)
            ) { isDragging ->
                // Обертка для применения модификаторов к контенту элемента
                // zIndex и animateItemPlacement применяются здесь для правильной визуализации
                // ReorderableItem уже обрабатывает перетаскивание, detectReorder не нужен
                Box(
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .animateItemPlacement()
                ) {
                    itemContent(item, isDragging)
                }
            }
        }
    }
}

