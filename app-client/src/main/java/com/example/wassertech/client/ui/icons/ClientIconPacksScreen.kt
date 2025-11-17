package ru.wassertech.client.ui.icons

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import ru.wassertech.core.ui.components.AppEmptyState
import ru.wassertech.core.ui.components.IconPackCard
import ru.wassertech.core.ui.icons.IconEntityType
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.viewmodel.ClientIconPacksViewModel

/**
 * Экран списка доступных икон-паков для клиента.
 * Показывает все паки, которые синхронизированы с сервера и доступны данному клиенту.
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
        topBar = {
            TopAppBar(
                title = { Text("Мои икон-паки") },
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.packs,
                            key = { it.pack.id }
                        ) { packWithCount ->
                            val pack = packWithCount.pack
                            
                            // Находим первую иконку пака для превью
                            val previewIcon = allIcons
                                .firstOrNull { it.packId == pack.id }
                                ?.androidResName
                            
                            IconPackCard(
                                title = pack.name,
                                description = pack.description,
                                iconsCount = packWithCount.iconsCount,
                                previewIconResName = previewIcon,
                                entityType = IconEntityType.ANY,
                                isSystem = pack.isBuiltin,
                                // Для клиента показываем статусы, но тексты адаптированы
                                isVisibleInClient = null, // Поле отсутствует в текущей версии БД
                                isDefaultForAllClients = null, // Поле отсутствует в текущей версии БД
                                onClick = { onPackClick(pack.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}


