package ru.wassertech.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.IconPackCard
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.data.AppDatabase
import ru.wassertech.viewmodel.IconPacksViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран списка всех икон-паков.
 * Показывает все паки, загруженные из локальной БД через синхронизацию.
 * Позволяет выбирать паки для загрузки и загружать их.
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Икон-паки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedPackIds.isNotEmpty() && !uiState.isDownloading) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.downloadSelectedPacks() },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Загрузить выбранные (${uiState.selectedPackIds.size})")
                }
            }
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
                        ) { packWithStatus ->
                            IconPackItem(
                                packWithStatus = packWithStatus,
                                isSelected = uiState.selectedPackIds.contains(packWithStatus.pack.id),
                                allIcons = allIcons,
                                viewModel = viewModel,
                                onPackClick = onPackClick,
                                onToggleSelection = { viewModel.togglePackSelection(packWithStatus.pack.id) }
                            )
                        }
                    }
                }
            }
            
            // Оверлей с прогрессом загрузки
            val downloadProgress = uiState.downloadProgress
            if (uiState.isDownloading && downloadProgress != null) {
                DownloadProgressOverlay(
                    progress = downloadProgress,
                    onCancel = { /* TODO: добавить отмену загрузки */ }
                )
            }
        }
    }
}

@Composable
private fun IconPackItem(
    packWithStatus: IconPacksViewModel.IconPackWithStatus,
    isSelected: Boolean,
    allIcons: List<ru.wassertech.data.entities.IconEntity>,
    viewModel: IconPacksViewModel,
    onPackClick: (String) -> Unit,
    onToggleSelection: () -> Unit
) {
    val pack = packWithStatus.pack
    
    // Находим первую иконку пака для превью
    val previewIcon = allIcons.firstOrNull { it.packId == pack.id }
    
    // Получаем локальный путь к превью-иконке, если она загружена
    var previewIconLocalPath by remember(pack.id, previewIcon?.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(pack.id, previewIcon?.id) {
        previewIconLocalPath = previewIcon?.let { icon ->
            viewModel.getLocalIconPath(icon)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Чекбокс для выбора
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
            
            // Основная информация о паке
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Статусные бейджи
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (packWithStatus.isDownloaded) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Загружен", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                        if (packWithStatus.hasUpdate) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Обновление", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                        if (!packWithStatus.isDownloaded && packWithStatus.lastDownloadedEpoch == 0L) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Новый", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        }
                    }
                }
                
                if (!pack.description.isNullOrBlank()) {
                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!pack.folder.isNullOrBlank()) {
                        Text(
                            text = "Папка: ${pack.folder}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${packWithStatus.iconsCount} иконок",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Даты создания и обновления
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pack.createdAtEpoch > 0) {
                        Text(
                            text = "Создан: ${formatEpoch(pack.createdAtEpoch)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (pack.updatedAtEpoch > 0) {
                        Text(
                            text = "Обновлён: ${formatEpoch(pack.updatedAtEpoch)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Превью иконки
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                IconPackCard(
                    title = "",
                    description = null,
                    iconsCount = 0,
                    previewIconResName = previewIcon?.androidResName,
                    previewIconLocalPath = previewIconLocalPath,
                    previewIconImageUrl = previewIcon?.imageUrl,
                    entityType = IconEntityType.ANY,
                    isSystem = pack.isBuiltin,
                    isVisibleInClient = null,
                    isDefaultForAllClients = null,
                    onClick = { onPackClick(pack.id) }
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressOverlay(
    progress: IconPacksViewModel.DownloadProgress,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Загрузка икон-паков",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = "Пак ${progress.currentPack} из ${progress.totalPacks}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LinearProgressIndicator(
                    progress = { 
                        if (progress.totalIcons > 0) {
                            progress.currentIcon.toFloat() / progress.totalIcons
                        } else {
                            0f
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Иконок: ${progress.currentIcon}/${progress.totalIcons}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextButton(onClick = onCancel) {
                    Text("Отмена")
                }
            }
        }
    }
}

private fun formatEpoch(epoch: Long): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date(epoch))
}
