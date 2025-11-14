package ru.wassertech.client.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
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
import ru.wassertech.core.auth.UserAuthService
import ru.wassertech.client.repository.InstallationsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    val installationsRepository = remember { InstallationsRepository(context) }
    
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
                text = "Синхронизация данных",
                style = MaterialTheme.typography.titleLarge
            )
            
            Text(
                text = "Синхронизация данных с сервером через REST API",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Кнопка "Синхронизировать данные"
            Button(
                onClick = {
                    scope.launch {
                        syncing = true
                        syncMessage = null
                        try {
                            if (!UserAuthService.isLoggedIn(context)) {
                                syncMessage = "Необходимо войти в систему для синхронизации"
                                snackbarHostState.showSnackbar(syncMessage!!)
                                return@launch
                            }
                            
                            val result = withContext(Dispatchers.IO) {
                                installationsRepository.syncInstallations()
                            }
                            
                            result.fold(
                                onSuccess = { count ->
                                    syncMessage = "Синхронизация завершена. Загружено установок: $count"
                                    snackbarHostState.showSnackbar(syncMessage!!)
                                },
                                onFailure = { error ->
                                    val errorMsg = "Ошибка при синхронизации: ${error.message}"
                                    syncMessage = errorMsg
                                    snackbarHostState.showSnackbar(errorMsg)
                                }
                            )
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при синхронизации: ${e.message}"
                            syncMessage = errorMsg
                            snackbarHostState.showSnackbar(errorMsg)
                        } finally {
                            syncing = false
                        }
                    }
                },
                enabled = !syncing && UserAuthService.isLoggedIn(context),
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
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Синхронизировать данные")
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
            
            // Разделитель перед кнопкой выхода
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Кнопка "Выйти из учетной записи"
            OutlinedButton(
                onClick = {
                    showLogoutDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Выйти из учетной записи")
            }
        }
    }
    
    // Диалог подтверждения выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Выход из учетной записи")
            },
            text = {
                Text("Вы уверены, что хотите выйти из учетной записи?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Выполняем logout через UserAuthService
                        UserAuthService.logout(context)
                        // Закрываем диалог
                        showLogoutDialog = false
                        // Вызываем callback для навигации
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

