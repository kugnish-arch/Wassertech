package ru.wassertech.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.core.ui.icons.IconResolver
import ru.wassertech.core.ui.theme.DialogStyle

/**
 * Простые data-классы для передачи данных в диалог без зависимости от конкретных Entity.
 */
data class IconPackUiData(
    val id: String,
    val name: String
)

data class IconUiData(
    val id: String,
    val packId: String,
    val label: String,
    val entityType: String,
    val androidResName: String?,
    val code: String? = null, // Код иконки для fallback поиска ресурса
    val localImagePath: String? = null // Локальный путь к файлу изображения (если загружено с сервера)
)

/**
 * Диалог выбора иконки для сущности (Site, Installation, Component).
 * 
 * @param visible Видимость диалога
 * @param onDismissRequest Обработчик закрытия диалога
 * @param entityType Тип сущности (SITE, INSTALLATION, COMPONENT)
 * @param packs Список доступных паков иконок
 * @param iconsByPack Map: packId -> список иконок в этом паке
 * @param selectedIconId ID выбранной иконки (может быть null)
 * @param onIconSelected Обработчик выбора иконки (iconId может быть null для сброса)
 * 
 * Примечание: Данные должны быть подготовлены в ViewModel из IconPackEntity и IconEntity.
 */
@Composable
fun IconPickerDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    entityType: IconEntityType,
    packs: List<IconPackUiData>,
    iconsByPack: Map<String, List<IconUiData>>,
    selectedIconId: String?,
    onIconSelected: (iconId: String?) -> Unit
) {
    if (!visible) return
    
    var selectedPackId by remember { mutableStateOf<String?>(null) }
    
    // Выбираем первый пак, если не выбран
    LaunchedEffect(packs) {
        if (selectedPackId == null && packs.isNotEmpty()) {
            selectedPackId = packs.first().id
        }
    }
    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = DialogStyle.shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = DialogStyle.elevation)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DialogStyle.padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Text(
                    text = "Выбор иконки",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Фильтр по пакетам (Chips)
                if (packs.isNotEmpty()) {
                    val filteredPacks = packs.filter { pack ->
                        iconsByPack[pack.id]?.any { icon ->
                            icon.entityType == "ANY" || icon.entityType == entityType.name
                        } == true
                    }
                    
                    if (filteredPacks.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filteredPacks.forEach { pack ->
                                FilterChip(
                                    selected = selectedPackId == pack.id,
                                    onClick = { selectedPackId = pack.id },
                                    label = { Text(pack.name) }
                                )
                            }
                        }
                    }
                }
                
                // Сетка иконок для выбранного пака
                val currentIcons = remember(selectedPackId, iconsByPack, entityType) {
                    selectedPackId?.let { packId ->
                        iconsByPack[packId]?.filter { icon ->
                            icon.entityType == "ANY" || icon.entityType == entityType.name
                        } ?: emptyList()
                    } ?: emptyList()
                }
                
                if (currentIcons.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Иконки не найдены",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(currentIcons) { icon ->
                            val isSelected = icon.id == selectedIconId
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        android.util.Log.d("IconPickerDialog", "Icon clicked: id=${icon.id}, label=${icon.label}")
                                        onIconSelected(icon.id)
                                        onDismissRequest()
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Card(
                                    modifier = Modifier.size(64.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    } else null
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Используем правильный entityType из icon.entityType для корректного отображения
                                        // Если androidResName не найден, будет использована дефолтная иконка для этого типа
                                        val iconEntityType = try {
                                            IconEntityType.valueOf(icon.entityType)
                                        } catch (e: IllegalArgumentException) {
                                            entityType // Fallback на переданный entityType
                                        }
                                        // Логирование для отладки
                                        LaunchedEffect(icon.id, icon.androidResName, icon.code, iconEntityType) {
                                            android.util.Log.d("IconPickerDialog", 
                                                "Icon: id=${icon.id}, label=${icon.label}, " +
                                                "androidResName=${icon.androidResName}, " +
                                                "code=${icon.code}, " +
                                                "entityType=${icon.entityType}, " +
                                                "resolvedEntityType=${iconEntityType.name}"
                                            )
                                        }
                                        IconResolver.IconImage(
                                            androidResName = icon.androidResName,
                                            entityType = iconEntityType,
                                            contentDescription = icon.label,
                                            size = 48.dp,
                                            code = icon.code, // Передаем code для fallback поиска ресурса
                                            localImagePath = icon.localImagePath // Передаем локальный путь к файлу изображения
                                        )
                                    }
                                }
                                Text(
                                    text = icon.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                
                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка "Без иконки"
                    TextButton(
                        onClick = {
                            onIconSelected(null)
                            onDismissRequest()
                        }
                    ) {
                        Text("Без иконки")
                    }
                    
                    // Кнопка "Отмена"
                    TextButton(onClick = onDismissRequest) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}


