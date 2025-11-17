package ru.wassertech.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import ru.wassertech.crm.R
import ru.wassertech.auth.UserAuthService
import ru.wassertech.data.AppDatabase
import ru.wassertech.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.NavHostController

@Composable
fun SettingsScreen(
    navController: NavHostController? = null
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    
    // Проверяем оффлайн режим
    val isOfflineMode = UserAuthService.isOfflineMode(context)
    
    // Настройка сохранения HTML
    val saveHtmlFlow = db.settingsDao().getValue("save_html")
    val saveHtml by saveHtmlFlow.collectAsState(initial = null)
    val saveHtmlValue = saveHtml?.toBoolean() ?: false
    
    // Переключатель метода рендеринга PDF отключен - всегда используется bitmap метод
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Пункт для перехода к икон-пакам
            if (navController != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("icon_packs") {
                                launchSingleTop = true
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Икон-паки",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Просмотр всех доступных пакетов иконок",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = ru.wassertech.core.ui.theme.NavigationIcons.NavigateIcon,
                            contentDescription = "Открыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Настройка сохранения HTML
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сохранять HTML",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "При генерации PDF отчёта также сохранять HTML файл",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = saveHtmlValue,
                        onCheckedChange = { newValue ->
                            scope.launch(Dispatchers.IO) {
                                db.settingsDao().setValue(
                                    ru.wassertech.data.entities.SettingsEntity(
                                        key = "save_html",
                                        value = newValue.toString()
                                    )
                                )
                            }
                        }
                    )
                }
            }
            
            Text(
                text = "Синхронизация с удалённой БД",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Тултип оффлайн режима (если включен)
            if (isOfflineMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.cloud_offline_outline),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Для использования функций синхронизации требуется использовать онлайн режим. Просьба выйти из приложения и повторить вход.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Кнопка "Отправить на сервер" (push + pull)
            Button(
                onClick = {
                    scope.launch {
                        syncing = true
                        syncMessage = null
                        try {
                            val syncEngine = SyncEngine(context)
                            val result = withContext(Dispatchers.IO) {
                                syncEngine.syncFull()
                            }
                            syncMessage = result.message
                            if (result.success) {
                                snackbarHostState.showSnackbar(
                                    message = result.message,
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = result.message,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при синхронизации: ${e.message}"
                            syncMessage = errorMsg
                            snackbarHostState.showSnackbar(
                                message = errorMsg,
                                duration = SnackbarDuration.Long
                            )
                        } finally {
                            syncing = false
                        }
                    }
                },
                enabled = !syncing && !isOfflineMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Синхронизация...")
                } else {
                    Icon(
                        Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить на сервер")
                }
            }
            
            // Кнопка "Загрузить с сервера" (только pull)
            Button(
                onClick = {
                    scope.launch {
                        syncing = true
                        syncMessage = null
                        try {
                            val syncEngine = SyncEngine(context)
                            val result = withContext(Dispatchers.IO) {
                                syncEngine.syncPull()
                            }
                            syncMessage = result.message
                            if (result.success) {
                                snackbarHostState.showSnackbar(
                                    message = result.message,
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = result.message,
                                    duration = SnackbarDuration.Long
                                )
                            }
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при загрузке: ${e.message}"
                            syncMessage = errorMsg
                            snackbarHostState.showSnackbar(
                                message = errorMsg,
                                duration = SnackbarDuration.Long
                            )
                        } finally {
                            syncing = false
                        }
                    }
                },
                enabled = !syncing && !isOfflineMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузка...")
                } else {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузить с сервера")
                }
            }
            
            // Статус синхронизации
            syncMessage?.let { message ->
                val scrollState = rememberScrollState()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Результат синхронизации:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}


