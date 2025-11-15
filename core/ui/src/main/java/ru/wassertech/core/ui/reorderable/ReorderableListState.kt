package ru.wassertech.core.ui.reorderable

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState

/**
 * Состояние для перетаскиваемого списка.
 * 
 * Объединяет состояние LazyListState и ReorderableLazyListState из библиотеки compose-reorderable.
 * 
 * @param listState Состояние ленивого списка для управления прокруткой и позицией
 * @param reorderableState Состояние для управления перетаскиванием элементов
 * @param onMove Callback, вызываемый при перемещении элемента с позиции fromIndex на позицию toIndex
 */
@androidx.compose.runtime.Stable
data class ReorderableListState<T>(
    val listState: LazyListState,
    val reorderableState: ReorderableLazyListState,
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit
)

/**
 * Создает и запоминает состояние для перетаскиваемого списка.
 * 
 * @param onMove Callback, вызываемый при перемещении элемента с позиции fromIndex на позицию toIndex
 * @return Состояние для использования в ReorderableLazyColumn
 */
@Composable
fun <T> rememberReorderableListState(
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): ReorderableListState<T> {
    val reorderableState: ReorderableLazyListState = rememberReorderableLazyListState(
        onMove = { from, to ->
            onMove(from.index, to.index)
        }
    )
    
    return remember(reorderableState, onMove) {
        ReorderableListState(
            listState = reorderableState.listState,
            reorderableState = reorderableState,
            onMove = onMove
        )
    }
}

