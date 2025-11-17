package ru.wassertech.ui.icons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.IconPackCard
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.data.AppDatabase
import ru.wassertech.viewmodel.IconPacksViewModel

/**
 * Экран списка всех икон-паков.
 * Показывает все паки, загруженные из локальной БД через синхронизацию.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPacksScreen(
    onPackClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { IconPacksViewModel(context.applicationContext as android.app.Application) }
    val uiState by viewModel.packsState.collectAsState()
    
    // Загружаем все иконки для превью
    val db = remember { AppDatabase.getInstance(context) }
    val allIcons by db.iconDao().observeAllActive().collectAsState(initial = emptyList())
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                val errorMessage = uiState.error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ошибка",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = errorMessage ?: "Неизвестная ошибка",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            uiState.packs.isEmpty() -> {
                AppEmptyState(
                    icon = Icons.Filled.Image,
                    title = "Икон-паки отсутствуют",
                    description = "Синхронизируйте данные, чтобы загрузить пакеты иконок."
                )
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uiState.packs,
                        key = { it.pack.id }
                    ) { packWithCount ->
                        val pack = packWithCount.pack
                        val progress = uiState.downloadProgress[pack.id]
                        
                        // Находим первую иконку пака для превью
                        val previewIcon = allIcons
                            .firstOrNull { it.packId == pack.id }
                        
                        // Получаем локальный путь к превью-иконке, если она загружена
                        var previewIconLocalPath by remember(pack.id, previewIcon?.id) { mutableStateOf<String?>(null) }
                        LaunchedEffect(pack.id, previewIcon?.id) {
                            previewIconLocalPath = previewIcon?.let { icon ->
                                viewModel.getLocalIconPath(icon)
                            }
                        }
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconPackCard(
                                title = pack.name,
                                description = pack.description,
                                iconsCount = packWithCount.iconsCount,
                                previewIconResName = previewIcon?.androidResName,
                                previewIconLocalPath = previewIconLocalPath,
                                previewIconImageUrl = previewIcon?.imageUrl,
                                entityType = IconEntityType.ANY,
                                isSystem = pack.isBuiltin,
                                isVisibleInClient = null,
                                isDefaultForAllClients = null,
                                onClick = { onPackClick(pack.id) }
                            )
                            
                            // Прогресс загрузки
                            if (progress != null && progress.second > 0) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { progress.first.toFloat() / progress.second },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "Загружено: ${progress.first}/${progress.second}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            // Кнопка загрузки
                            Button(
                                onClick = { viewModel.downloadPackImages(pack.id) },
                                enabled = progress == null || progress.first == progress.second,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (progress != null && progress.first == progress.second) {
                                        "Обновить иконки"
                                    } else {
                                        "Загрузить иконки"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
