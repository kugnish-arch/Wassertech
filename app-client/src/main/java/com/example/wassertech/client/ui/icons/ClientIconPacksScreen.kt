package ru.wassertech.client.ui.icons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.viewmodel.ClientIconPacksViewModel
import ru.wassertech.feature.icons.ui.IconPackItemCard
import ru.wassertech.feature.icons.ui.IconPackItemData
import ru.wassertech.feature.icons.ui.DownloadProgressOverlay
import ru.wassertech.feature.icons.ui.DownloadProgressData
import ru.wassertech.feature.icons.ui.formatEpoch

/**
 * Экран списка доступных икон-паков для клиента.
 * Показывает все паки, которые синхронизированы с сервера и доступны данному клиенту.
 * Позволяет выбирать паки для загрузки и загружать их.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientIconPacksScreen(
    onPackClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { 
        ClientIconPacksViewModel(context.applicationContext as android.app.Application) 
    }
    val uiState by viewModel.packsState.collectAsState()
    
    // Загружаем все иконки для превью
    val db = remember { AppDatabase.getInstance(context) }
    val allIcons by db.iconDao().observeAllActive().collectAsState(initial = emptyList())
    
    Scaffold(
        floatingActionButton = {
            if (uiState.selectedPackIds.isNotEmpty() && !uiState.isDownloading) {
                FloatingActionButton(
                    onClick = { viewModel.downloadSelectedPacks() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Загрузить выбранные"
                    )
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
                
                uiState.packs.isEmpty() -> {
                    AppEmptyState(
                        icon = Icons.Filled.Image,
                        title = "Икон-паки не найдены",
                        description = "Обратитесь к вашему подрядчику Wassertech, чтобы подключить наборы иконок для ваших объектов и установок."
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
                    progress = DownloadProgressData(
                        currentPack = downloadProgress.currentPack,
                        totalPacks = downloadProgress.totalPacks,
                        currentIcon = downloadProgress.currentIcon,
                        totalIcons = downloadProgress.totalIcons
                    ),
                    onCancel = { /* TODO: добавить отмену загрузки */ }
                )
            }
        }
    }
}

@Composable
private fun IconPackItem(
    packWithStatus: ClientIconPacksViewModel.IconPackWithStatus,
    isSelected: Boolean,
    allIcons: List<ru.wassertech.client.data.entities.IconEntity>,
    viewModel: ClientIconPacksViewModel,
    onPackClick: (String) -> Unit,
    onToggleSelection: () -> Unit
) {
    val pack = packWithStatus.pack
    val context = LocalContext.current
    val iconRepository = remember { ru.wassertech.client.data.repository.IconRepository(context) }
    
    // Находим первую иконку пака для превью
    val previewIcon = allIcons.firstOrNull { it.packId == pack.id }
    
    // Получаем локальный путь к превью-иконке, если она загружена
    val previewIconLocalPath by remember(previewIcon?.id) {
        kotlinx.coroutines.flow.flow {
            val path = previewIcon?.id?.let { iconRepository.getLocalIconPath(it) }
            emit(path)
        }
    }.collectAsState(initial = null)
    
    val itemData = IconPackItemData(
        id = pack.id,
        name = pack.name,
        description = pack.description,
        createdAtEpoch = pack.createdAtEpoch,
        updatedAtEpoch = pack.updatedAtEpoch,
        isDownloaded = packWithStatus.isDownloaded,
        hasUpdate = packWithStatus.hasUpdate,
        isNew = !packWithStatus.isDownloaded,
        previewIconResName = previewIcon?.androidResName,
        previewIconLocalPath = previewIconLocalPath,
        previewIconCode = previewIcon?.code
    )
    
    IconPackItemCard(
        data = itemData,
        isSelected = isSelected,
        onToggleSelection = onToggleSelection,
        onPackClick = { onPackClick(pack.id) },
        formatEpoch = { epoch -> formatEpoch(epoch) }
    )
}

