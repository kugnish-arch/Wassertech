package ru.wassertech.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import ru.wassertech.client.ui.settings.SettingsScreen
import ru.wassertech.client.ui.sites.SitesScreen
import ru.wassertech.core.auth.SessionManager
import ru.wassertech.navigation.AppRoutes
import ru.wassertech.feature.auth.AuthRoutes
import ru.wassertech.client.sync.SyncEngine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * Главный экран с табами
 */
@Composable
fun HomeScreen(
    navController: NavController? = null,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTabIndex by remember { mutableStateOf(initialTab) }
    
    // Обновляем selectedTabIndex при изменении initialTab
    LaunchedEffect(initialTab) {
        selectedTabIndex = initialTab
    }
    
    // Получаем текущую сессию пользователя для получения clientId
    val currentUser = remember { SessionManager.getInstance(context).getCurrentSession() }
    val clientId = currentUser?.clientId
    
    // Автоматическая синхронизация при первом запуске экрана
    LaunchedEffect(clientId) {
        if (clientId != null) {
            android.util.Log.d("HomeScreen", "Начинаю автоматическую синхронизацию для clientId: $clientId")
            scope.launch(Dispatchers.IO) {
                try {
                    val syncEngine = SyncEngine(context)
                    val result = syncEngine.syncFull() // Используем полную синхронизацию (push + pull)
                    android.util.Log.d("HomeScreen", "Автоматическая синхронизация завершена: ${result.message}")
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Ошибка автоматической синхронизации", e)
                }
            }
        } else {
            android.util.Log.w("HomeScreen", "clientId отсутствует, синхронизация не выполняется")
        }
    }
    
    when (selectedTabIndex) {
        0 -> {
            // Экран списка объектов
            if (clientId != null) {
                // Для SitesScreen передаем paddingValues напрямую, так как он сам применяет их к заголовку
                SitesScreen(
                    clientId = clientId,
                    onOpenSite = { siteId ->
                        navController?.navigate(AppRoutes.siteDetail(siteId))
                    },
                    paddingValues = paddingValues
                )
            } else {
                // Если нет clientId (например, для ADMIN/ENGINEER), показываем сообщение
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (currentUser?.isAdmin() == true || currentUser?.isEngineer() == true) {
                                "Приложение предназначено для клиентов.\nДля работы с системой используйте CRM-приложение."
                            } else {
                                "Ошибка: не удалось определить ID клиента"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        1 -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingsScreen(
                navController = navController as? NavHostController,
                onLogout = {
                    // Навигация на экран логина с очисткой стека
                    (navController as? NavHostController)?.navigate(AuthRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}