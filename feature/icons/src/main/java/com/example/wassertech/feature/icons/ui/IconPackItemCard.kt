package ru.wassertech.feature.icons.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.icons.IconResolver

/**
 * Данные для отображения карточки икон-пака.
 */
data class IconPackItemData(
    val id: String,
    val name: String,
    val description: String?,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long,
    val isDownloaded: Boolean,
    val hasUpdate: Boolean,
    val isNew: Boolean,
    val previewIconResName: String?,
    val previewIconLocalPath: String?,
    val previewIconCode: String?
)

/**
 * Компонент карточки икон-пака с новой вёрсткой:
 * 1я Строка - только чек-бокс и название пака
 * 2я строка - тэги (Загружен, Новый, Обновлён)
 * 3я строка - описание и правее - иконка размером 48dp
 * 4я строка - Создан / Обновлён
 */
@Composable
fun IconPackItemCard(
    data: IconPackItemData,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onPackClick: () -> Unit,
    formatEpoch: (Long) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPackClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1я Строка - только чек-бокс и название пака
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() }
                )
                Text(
                    text = data.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 2я строка - тэги (Загружен, Новый, Обновлён)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (data.isDownloaded) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Загружен", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                if (data.isNew) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Новый", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                }
                if (data.hasUpdate) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Обновлён", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
            
            // 3я строка - описание и правее - иконка размером 48dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!data.description.isNullOrBlank()) {
                    Text(
                        text = data.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                IconResolver.IconImage(
                    androidResName = data.previewIconResName,
                    entityType = IconEntityType.ANY,
                    contentDescription = data.name,
                    size = 48.dp,
                    code = data.previewIconCode,
                    localImagePath = data.previewIconLocalPath
                )
            }
            
            // 4я строка - Создан / Обновлён
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (data.createdAtEpoch > 0) {
                    Text(
                        text = "Создан: ${formatEpoch(data.createdAtEpoch)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (data.updatedAtEpoch > 0) {
                    Text(
                        text = "Обновлён: ${formatEpoch(data.updatedAtEpoch)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

