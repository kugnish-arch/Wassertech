package ru.wassertech.client.ui.icons

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.components.IconGrid
import ru.wassertech.core.ui.components.IconPackBadgeRow
import ru.wassertech.core.ui.components.IconUiModel
import ru.wassertech.core.ui.components.ScreenTitleWithSubtitle
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.client.data.entities.IconEntity
import ru.wassertech.client.viewmodel.ClientIconPacksViewModel

/**
 * Экран детального просмотра икон-пака для клиента.
 * Показывает информацию о паке и все его иконки в виде сетки.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientIconPackDetailScreen(
    packId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { 
        ClientIconPacksViewModel(context.applicationContext as android.app.Application) 
    }
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.pack?.name ?: "Икон-пак") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                                text = uiState.error ?: "Неизвестная ошибка",
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
                    val pack = uiState.pack
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Заголовок с информацией о паке
                        pack?.let {
                            ScreenTitleWithSubtitle(
                                title = it.name,
                                subtitle = it.description ?: "${allIcons.size} ${if (allIcons.size == 1) "иконка" else if (allIcons.size in 2..4) "иконки" else "иконок"}"
                            )
                            
                            // Бейджи статусов (адаптированные для клиента)
                            if (it.isBuiltin) {
                                IconPackBadgeRow(
                                    isSystem = true,
                                    isVisibleInClient = null,
                                    isDefaultForAllClients = null
                                )
                            }
                        }
                        
                        // Информационный текст для клиента
                        Text(
                            text = "Этот пак содержит иконки для оформления объектов, установок и компонентов в вашем личном кабинете.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        val iconUiModels = filteredIcons.map { icon ->
                            IconUiModel(
                                id = icon.id,
                                title = icon.label,
                                entityType = icon.entityType,
                                androidResName = icon.androidResName
                            )
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
}


