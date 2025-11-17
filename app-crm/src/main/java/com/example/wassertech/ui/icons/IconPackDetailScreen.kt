package ru.wassertech.ui.icons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.components.IconGrid
import ru.wassertech.core.ui.components.IconPackBadgeRow
import ru.wassertech.core.ui.components.IconUiModel
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.data.entities.IconEntity
import ru.wassertech.viewmodel.IconPacksViewModel

/**
 * Экран детального просмотра икон-пака.
 * Показывает информацию о паке и все его иконки в виде сетки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackDetailScreen(
    packId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { IconPacksViewModel(context.applicationContext as android.app.Application) }
    val uiState by viewModel.detailState.collectAsState()
    
    // Фильтр по типу сущности
    var selectedEntityType by remember { mutableStateOf<String?>(null) }
    
    // Загружаем детали пака при первом показе
    LaunchedEffect(packId) {
        viewModel.loadPackDetail(packId)
    }
    
    // Очищаем состояние при уходе с экрана
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearDetailState()
        }
    }
    
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
                
                uiState.pack == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Пак не найден",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                else -> {
                    val pack = uiState.pack ?: return@Box
                    val allIcons = uiState.icons
                    
                    // Фильтруем иконки по типу сущности
                    val filteredIcons = if (selectedEntityType != null) {
                        allIcons.filter { icon ->
                            icon.entityType == selectedEntityType || icon.entityType == "ANY"
                        }
                    } else {
                        allIcons
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Описание пака (название уже в TopAppBar)
                        if (pack.description != null && pack.description.isNotBlank()) {
                            Text(
                                text = pack.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "${allIcons.size} ${if (allIcons.size == 1) "иконка" else if (allIcons.size in 2..4) "иконки" else "иконок"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Бейджи статусов
                        IconPackBadgeRow(
                            isSystem = pack.isBuiltin,
                            isVisibleInClient = null, // Поле отсутствует в текущей версии БД
                            isDefaultForAllClients = null // Поле отсутствует в текущей версии БД
                        )
                        
                        // Фильтр по типу сущности
                        if (allIcons.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedEntityType == null,
                                    onClick = { selectedEntityType = null },
                                    label = { Text("Все") }
                                )
                                FilterChip(
                                    selected = selectedEntityType == "SITE",
                                    onClick = { selectedEntityType = if (selectedEntityType == "SITE") null else "SITE" },
                                    label = { Text("Объекты") }
                                )
                                FilterChip(
                                    selected = selectedEntityType == "INSTALLATION",
                                    onClick = { selectedEntityType = if (selectedEntityType == "INSTALLATION") null else "INSTALLATION" },
                                    label = { Text("Установки") }
                                )
                                FilterChip(
                                    selected = selectedEntityType == "COMPONENT",
                                    onClick = { selectedEntityType = if (selectedEntityType == "COMPONENT") null else "COMPONENT" },
                                    label = { Text("Компоненты") }
                                )
                            }
                        }
                        
                        // Сетка иконок
                        val iconUiModels = remember(filteredIcons) {
                            filteredIcons.map { icon ->
                                IconUiModel(
                                    id = icon.id,
                                    title = icon.label,
                                    entityType = icon.entityType,
                                    androidResName = icon.androidResName,
                                    imageUrl = icon.imageUrl,
                                    localImagePath = viewModel.getLocalIconPath(icon)
                                )
                            }
                        }
                        
                        IconGrid(
                            icons = iconUiModels,
                            columns = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }




