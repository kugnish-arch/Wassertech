package ru.wassertech.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Простая модель иконки для UI (без зависимостей от Room).
 */
data class IconUiModel(
    val id: String,
    val title: String,
    val entityType: String, // "SITE", "INSTALLATION", "COMPONENT", "ANY"
    val androidResName: String? = null,
    val imageUrl: String? = null, // URL изображения (для загрузки с сервера)
    val localImagePath: String? = null // Локальный путь к файлу (если загружено локально)
)

/**
 * Компонент для отображения сетки иконок.
 * Используется в детальном просмотре икон-пака.
 * 
 * @param icons Список иконок для отображения
 * @param columns Количество колонок в сетке (по умолчанию 3)
 * @param onIconClick Обработчик клика на иконку (опционально)
 * @param modifier Модификатор для применения к компоненту
 */
@Composable
fun IconGrid(
    icons: List<IconUiModel>,
    columns: Int = 3,
    onIconClick: ((IconUiModel) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (icons.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Иконки отсутствуют",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(icons, key = { it.id }) { icon ->
            val entityType = when (icon.entityType) {
                "SITE" -> ru.wassertech.core.ui.icons.IconEntityType.SITE
                "INSTALLATION" -> ru.wassertech.core.ui.icons.IconEntityType.INSTALLATION
                "COMPONENT" -> ru.wassertech.core.ui.icons.IconEntityType.COMPONENT
                else -> ru.wassertech.core.ui.icons.IconEntityType.ANY
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onIconClick != null) {
                            Modifier.clickable { onIconClick(icon) }
                        } else {
                            Modifier
                        }
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ru.wassertech.core.ui.icons.IconResolver.IconImage(
                        androidResName = icon.androidResName,
                        entityType = entityType,
                        imageUrl = icon.imageUrl,
                        localImagePath = icon.localImagePath,
                        contentDescription = icon.title,
                        size = 48.dp
                    )
                    
                    Text(
                        text = icon.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

