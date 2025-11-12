package ru.wassertech.client.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.data.entities.ClientEntity
import ru.wassertech.client.sync.MySqlSyncService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var syncing by remember { mutableStateOf(false) }
    var loadingClients by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var clients by remember { mutableStateOf<List<ClientEntity>>(emptyList()) }
    var selectedClientId by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    
    // Загружаем список клиентов из удалённой БД при первом открытии экрана
    LaunchedEffect(Unit) {
        loadingClients = true
        try {
            withContext(Dispatchers.IO) {
                MySqlSyncService.pullClientsFromRemote(db)
            }
            // После загрузки из удалённой БД, читаем из локальной БД
            withContext(Dispatchers.IO) {
                clients = db.clientDao().getAllClientsNow()
            }
        } catch (e: Exception) {
            // Если не удалось загрузить из удалённой БД, загружаем из локальной
            withContext(Dispatchers.IO) {
                clients = db.clientDao().getAllClientsNow()
            }
            scope.launch {
                snackbarHostState.showSnackbar("Не удалось загрузить клиентов из удалённой БД. Используется локальный список.")
            }
        } finally {
            loadingClients = false
        }
    }
    
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
            
            Text(
                text = "Синхронизация с удалённой БД",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Индикатор загрузки клиентов
            if (loadingClients) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Загрузка клиентов...")
                }
            }
            
            // Выпадающий список клиентов
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Выберите клиента",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = clients.find { it.id == selectedClientId }?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Клиент") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text(client.name) },
                                    onClick = {
                                        selectedClientId = client.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Кнопка "Получить из удалённой БД"
            Button(
                onClick = {
                    if (selectedClientId == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Пожалуйста, выберите клиента")
                        }
                        return@Button
                    }
                    
                    scope.launch {
                        syncing = true
                        syncMessage = null
                        try {
                            val result = withContext(Dispatchers.IO) {
                                MySqlSyncService.pullClientDataFromRemote(db, selectedClientId!!)
                            }
                            syncMessage = result
                            snackbarHostState.showSnackbar(result)
                            
                            // Обновляем список клиентов после синхронизации
                            clients = db.clientDao().getAllClientsNow()
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при получении: ${e.message}"
                            syncMessage = errorMsg
                            snackbarHostState.showSnackbar(errorMsg)
                        } finally {
                            syncing = false
                        }
                    }
                },
                enabled = !syncing && selectedClientId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (syncing) {
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
                    Text("Получить из удалённой БД")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Статус синхронизации
            syncMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

