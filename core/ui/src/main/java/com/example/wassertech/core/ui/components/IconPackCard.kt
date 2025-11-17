package ru.wassertech.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Компонент карточки икон-пака для отображения в списке.
 * Используется в app-crm и может быть использован в app-client.
 * 
 * @param title Название пака
 * @param description Описание пака (опционально)
 * @param iconsCount Количество иконок в паке
 * @param previewIconResName Имя ресурса Android для превью иконки (опционально)
 * @param previewIconLocalPath Локальный путь к файлу превью-иконки (если загружено локально)
 * @param previewIconImageUrl URL изображения превью-иконки (опционально)
 * @param entityType Тип сущности для превью иконки (используется для дефолтной иконки)
 * @param isSystem Системный ли пак
 * @param isVisibleInClient Доступен ли пак клиентам (опционально)
 * @param isDefaultForAllClients По умолчанию для всех клиентов (опционально)
 * @param onClick Обработчик клика на карточку
 * @param modifier Модификатор для применения к компоненту
 */
@Composable
fun IconPackCard(
    title: String,
    description: String? = null,
    iconsCount: Int,
    previewIconResName: String? = null,
    previewIconLocalPath: String? = null,
    previewIconImageUrl: String? = null,
    entityType: ru.wassertech.core.ui.icons.IconEntityType = ru.wassertech.core.ui.icons.IconEntityType.ANY,
    isSystem: Boolean = false,
    isVisibleInClient: Boolean? = null,
    isDefaultForAllClients: Boolean? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Превью иконки
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                ru.wassertech.core.ui.icons.IconResolver.IconImage(
                    androidResName = previewIconResName,
                    entityType = entityType,
                    imageUrl = previewIconImageUrl,
                    localImagePath = previewIconLocalPath,
                    contentDescription = title,
                    size = 48.dp
                )
            }
            
            // Текстовая информация
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (description != null && description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                
                Text(
                    text = "$iconsCount ${if (iconsCount == 1) "иконка" else if (iconsCount in 2..4) "иконки" else "иконок"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Бейджи статусов
                IconPackBadgeRow(
                    isSystem = isSystem,
                    isVisibleInClient = isVisibleInClient,
                    isDefaultForAllClients = isDefaultForAllClients,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Стрелка навигации
            Icon(
                imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                contentDescription = "Открыть",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


