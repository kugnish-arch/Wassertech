package ru.wassertech.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Плейсхолдер для пустой группы/секции.
 *
 * @param text Текст плейсхолдера
 * @param indent Отступ слева
 * @param modifier Модификатор для применения к компоненту
 */
@Composable
fun EmptyGroupPlaceholder(
    text: String,
    indent: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indent, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


