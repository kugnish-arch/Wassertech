package ru.wassertech.client.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wassertech.client.sync.SyncEngine
import androidx.navigation.NavHostController
import ru.wassertech.navigation.AppRoutes
import ru.wassertech.core.ui.theme.NavigationIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController? = null,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var syncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    val syncEngine = remember { SyncEngine(context) }
    
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
                            navController.navigate(AppRoutes.CLIENT_ICON_PACKS) {
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
                                    text = "Мои икон-паки",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Просмотр доступных наборов иконок",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = NavigationIcons.NavigateIcon,
                            contentDescription = "Открыть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
            }
            
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
                            val sessionManager = ru.wassertech.core.auth.SessionManager.getInstance(context)
                            if (!sessionManager.isLoggedIn()) {
                                syncMessage = "Необходимо войти в систему для синхронизации"
                                snackbarHostState.showSnackbar(syncMessage!!)
                                return@launch
                            }
                            
                            val result = withContext(Dispatchers.IO) {
                                syncEngine.syncFull() // Используем полную синхронизацию (push + pull)
                            }
                            
                            syncMessage = result.message
                            if (result.success) {
                                snackbarHostState.showSnackbar(result.message)
                            } else {
                                snackbarHostState.showSnackbar(result.message, duration = SnackbarDuration.Long)
                            }
                        } catch (e: Exception) {
                            val errorMsg = "Ошибка при синхронизации: ${e.message}"
                            syncMessage = errorMsg
                            snackbarHostState.showSnackbar(errorMsg)
                        } finally {
                            syncing = false
                        }
                    }
                },
                enabled = !syncing && ru.wassertech.core.auth.SessionManager.getInstance(context).isLoggedIn(),
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
                        // Выполняем logout через AuthRepository (очищает данные для CLIENT)
                        scope.launch {
                            try {
                                val authRepository = ru.wassertech.client.auth.AuthRepository(context)
                                authRepository.logout()
                            } catch (e: Exception) {
                                Log.e("SettingsScreen", "Ошибка при выходе", e)
                            }
                            // Закрываем диалог
                            showLogoutDialog = false
                            // Вызываем callback для навигации
                            onLogout()
                        }
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

