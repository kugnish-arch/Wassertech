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
import com.example.wassertech.data.AppDatabase
import com.example.wassertech.data.entities.SettingsEntity
import com.example.wassertech.sync.MySqlSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    
    // Настройка метода рендеринга PDF
    val pdfMethodFlow = remember { db.settingsDao().getValue("pdf_render_method") }
    var pdfMethod by remember { mutableStateOf<String?>(null) }
    var pdfMethodLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        pdfMethod = pdfMethodFlow.first() ?: "bitmap" // По умолчанию "bitmap"
        pdfMethodLoading = false
    }
    
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Переключатель метода рендеринга PDF (debug-фича)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Метод рендеринга PDF (debug)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Выберите метод конвертации HTML в PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (pdfMethodLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (pdfMethod == "direct") "Напрямую" else "Через Bitmap",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = pdfMethod == "direct",
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        val newValue = if (checked) "direct" else "bitmap"
                                        withContext(Dispatchers.IO) {
                                            db.settingsDao().setValue(
                                                SettingsEntity("pdf_render_method", newValue)
                                            )
                                        }
                                        pdfMethod = newValue
                                        snackbarHostState.showSnackbar(
                                            "Метод рендеринга изменён на: ${if (checked) "Напрямую" else "Через Bitmap"}"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "Синхронизация с удалённой БД",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(Modifier.height(24.dp))
            
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
                enabled = !syncing,
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
                enabled = !syncing,
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


