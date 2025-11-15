package ru.wassertech.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Компонент для отображения заголовка экрана с подзаголовком.
 * Используется для экранов, где нужно показать основное название и дополнительную информацию
 * (например, название установки с указанием объекта, к которому она принадлежит).
 *
 * @param title Основной заголовок
 * @param subtitle Подзаголовок (опционально)
 * @param titleStyle Стиль текста для заголовка (по умолчанию используется MaterialTheme.typography.titleLarge)
 * @param subtitleStyle Стиль текста для подзаголовка (по умолчанию используется MaterialTheme.typography.bodySmall)
 * @param titleColor Цвет заголовка (по умолчанию используется MaterialTheme.colorScheme.onSurface)
 * @param subtitleColor Цвет подзаголовка (по умолчанию используется MaterialTheme.colorScheme.onSurfaceVariant)
 * @param modifier Модификатор для применения к компоненту
 */
@Composable
fun ScreenTitleWithSubtitle(
    title: String,
    subtitle: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodySmall,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = titleStyle,
            color = titleColor
        )
        if (subtitle != null && subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = subtitleStyle,
                color = subtitleColor
            )
        }
    }
}

