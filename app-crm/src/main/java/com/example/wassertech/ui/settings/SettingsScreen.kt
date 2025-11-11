package com.example.wassertech.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.wassertech.R
import com.example.wassertech.auth.UserAuthService
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.sync.MySqlSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
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
                                    com.example.wassertech.data.entities.SettingsEntity(
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
            
            // Кнопка "Отправить в удалённую БД"
            Button(
                onClick = {
                    scope.launch {
                        syncing = true
                        syncMessage = null
                        try {
                            val result = withContext(Dispatchers.IO) {
                                MySqlSyncService.pushToRemote(db)
                            }
                            syncMessage = result
                            snackbarHostState.showSnackbar(result)
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при отправке: ${e.message}"
                            syncMessage = errorMsg
                            snackbarHostState.showSnackbar(errorMsg)
                        } finally {
                            syncing = false
                        }
                    }
                },
                enabled = !syncing && !isOfflineMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (syncing && syncMessage?.contains("Отправка") == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Отправка...")
                } else {
                    Icon(
                        Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить в удалённую БД")
                }
            }
            
            // Кнопка "Получить из удаленной БД"
            Button(
                onClick = {
                    scope.launch {
                        syncing = true
                        syncMessage = null
                        try {
                            val result = withContext(Dispatchers.IO) {
                                MySqlSyncService.pullFromRemote(db)
                            }
                            syncMessage = result
                            snackbarHostState.showSnackbar(result)
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при получении: ${e.message}"
                            syncMessage = errorMsg
                            snackbarHostState.showSnackbar(errorMsg)
                        } finally {
                            syncing = false
                        }
                    }
                },
                enabled = !syncing && !isOfflineMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (syncing && syncMessage?.contains("Получение") == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Получение...")
                } else {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Получить из удаленной БД")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Кнопка "Мигрировать удалённую БД"
            OutlinedButton(
                onClick = {
                    scope.launch {
                        syncing = true
                        syncMessage = null
                        try {
                            val result = withContext(Dispatchers.IO) {
                                MySqlSyncService.migrateRemoteDatabase()
                            }
                            syncMessage = result
                            snackbarHostState.showSnackbar(
                                message = result,
                                duration = SnackbarDuration.Short
                            )
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при миграции: ${e.message}"
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
                if (syncing && syncMessage?.contains("Миграция") == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Миграция...")
                } else {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Мигрировать удалённую БД")
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


