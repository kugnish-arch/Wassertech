package ru.wassertech.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Компонент для отображения бейджей свойств икон-пака.
 * Используется для показа статусов пака: системный, доступен клиентам, по умолчанию.
 * 
 * @param isSystem Системный ли пак (isBuiltin)
 * @param isVisibleInClient Доступен ли пак клиентам (опционально, если поле отсутствует в БД)
 * @param isDefaultForAllClients По умолчанию для всех клиентов (опционально, если поле отсутствует в БД)
 * @param modifier Модификатор для применения к компоненту
 */
@Composable
fun IconPackBadgeRow(
    isSystem: Boolean,
    isVisibleInClient: Boolean? = null,
    isDefaultForAllClients: Boolean? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isSystem) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = "Системный",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        // Показываем бейдж "Доступен клиентам" только если значение явно указано
        if (isVisibleInClient == true) {
            Badge(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = "Доступен клиентам",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        // Показываем бейдж "По умолчанию" только если значение явно указано
        if (isDefaultForAllClients == true) {
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(
                    text = "По умолчанию",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}





