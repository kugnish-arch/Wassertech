package ru.wassertech.crm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ru.wassertech.auth.UserAuthService
import ru.wassertech.ui.AppNavHost
import ru.wassertech.core.ui.splash.SplashRouteFixed
import ru.wassertech.core.ui.theme.WassertechTheme
import ru.wassertech.core.ui.dialogs.SessionExpiredDialog
import ru.wassertech.core.ui.auth.SessionExpiredHandler
import ru.wassertech.core.auth.SessionManager
import ru.wassertech.core.network.ApiClient
import ru.wassertech.feature.auth.AuthRoutes
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WassertechTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
private fun MainContent() {
    var showSplash by remember { mutableStateOf(true) }
    var showSessionExpiredDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    // Устанавливаем глобальный callback для обработки истечения сессии
    LaunchedEffect(Unit) {
        ApiClient.setSessionExpiredCallback(SessionExpiredHandler)
    }
    
    // Подписываемся на события истечения сессии
    LaunchedEffect(Unit) {
        SessionExpiredHandler.sessionExpiredEvent.collect {
            showSessionExpiredDialog = true
        }
    }
    
    // Проверяем состояние входа после сплэша
    LaunchedEffect(showSplash) {
        if (!showSplash) {
            if (!UserAuthService.isLoggedIn(context)) {
                navController.navigate(AuthRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }
    
    // Диалог истечения сессии
    SessionExpiredDialog(
        visible = showSessionExpiredDialog,
        onDismissRequest = {
            showSessionExpiredDialog = false
        },
        onNavigateToLogin = {
            scope.launch {
                // Очищаем сессию
                SessionManager.getInstance(context).clearSession()
                UserAuthService.logout(context)
                showSessionExpiredDialog = false
                navController.navigate(AuthRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    )
    
    if (showSplash) {
        SplashRouteFixed(
            onFinished = { showSplash = false },
            totalMs = 1500
        )
    } else {
        // Use NavHost that includes both auth and main app navigation
        NavHost(
            navController = navController,
            startDestination = if (UserAuthService.isLoggedIn(context)) "main" else AuthRoutes.LOGIN
        ) {
            // Auth navigation
            composable(AuthRoutes.LOGIN) {
                ru.wassertech.feature.auth.LoginScreen(
                    onLoginSuccess = {
                        // После успешного логина переходим на экран синхронизации
                        navController.navigate("sync") {
                            popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            // Экран синхронизации после логина
            composable("sync") {
                ru.wassertech.ui.sync.PostLoginSyncScreen(
                    onSyncComplete = {
                        // После успешной синхронизации переходим на основной экран
                        navController.navigate("main") {
                            popUpTo("sync") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoOffline = {
                        // Переходим в оффлайн режим
                        navController.navigate("main") {
                            popUpTo("sync") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            
            // Main app navigation
            composable("main") {
                AppNavHost(
                    onLogout = {
                        UserAuthService.logout(context)
                        navController.navigate(AuthRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
