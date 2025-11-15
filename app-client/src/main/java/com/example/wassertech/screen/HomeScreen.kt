package ru.wassertech.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.wassertech.core.ui.theme.NavigationBarStyle
import ru.wassertech.client.ui.settings.SettingsScreen
import ru.wassertech.client.data.AppDatabase
import ru.wassertech.client.ui.reports.ReportsDatabaseProvider
import ru.wassertech.client.ui.reports.InstallationsReportsScreen
import ru.wassertech.client.ui.sites.SitesScreen
import ru.wassertech.client.auth.UserSessionManager
import ru.wassertech.feature.reports.ReportsDatabaseProvider as IReportsDatabaseProvider
import ru.wassertech.navigation.AppRoutes
import ru.wassertech.feature.auth.AuthRoutes

/**
 * Главный экран с табами
 */
@Composable
fun HomeScreen(navController: NavController? = null) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val databaseProvider = remember {
        ReportsDatabaseProvider(AppDatabase.getInstance(context)) as IReportsDatabaseProvider
    }
    
    // Получаем текущую сессию пользователя для получения clientId
    val currentUser = remember { UserSessionManager.getCurrentSession() }
    val clientId = currentUser?.clientId
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NavigationBarStyle.backgroundColor,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Объекты") },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    colors = NavigationBarStyle.itemColors()
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("Отчёты") },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    colors = NavigationBarStyle.itemColors()
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    colors = NavigationBarStyle.itemColors()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // Экран списка объектов
                    if (clientId != null) {
                        SitesScreen(
                            clientId = clientId,
                            onOpenSite = { siteId ->
                                navController?.navigate(AppRoutes.siteDetail(siteId))
                            }
                        )
                    } else {
                        // Если нет clientId, показываем сообщение об ошибке
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ошибка: не удалось определить ID клиента")
                        }
                    }
                }
                1 -> InstallationsReportsScreen(
                    databaseProvider = databaseProvider,
                    onNavigateToSessionDetail = { sessionId ->
                        navController?.navigate(AppRoutes.sessionDetail(sessionId))
                    },
                    onNavigateToLogin = {
                        // Навигация на экран логина с очисткой стека
                        navController?.navigate(AuthRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
                2 -> SettingsScreen(
                    onLogout = {
                        // Навигация на экран логина с очисткой стека
                        navController?.navigate(AuthRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}


