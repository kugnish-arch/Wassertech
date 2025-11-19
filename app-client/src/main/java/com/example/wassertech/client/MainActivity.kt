package ru.wassertech.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import ru.wassertech.core.auth.UserAuthService
import ru.wassertech.core.auth.SessionManager
import ru.wassertech.core.ui.auth.SessionExpiredHandler
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.ui.theme.WassertechTheme
import ru.wassertech.core.ui.dialogs.SessionExpiredDialog
import ru.wassertech.navigation.AppNavigation
import ru.wassertech.navigation.AppRoutes
import ru.wassertech.feature.auth.AuthRoutes

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WassertechTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var showSessionExpiredDialog by remember { mutableStateOf(false) }
                    
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
                    
                    // Проверяем состояние входа при старте
                    LaunchedEffect(Unit) {
                        startDestination = if (UserAuthService.isLoggedIn(context)) {
                            AppRoutes.HOME
                        } else {
                            AuthRoutes.LOGIN
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
                    
                    // Показываем навигацию только после проверки
                    startDestination?.let { destination ->
                        AppNavigation(
                            navController = navController,
                            startDestination = destination
                        )
                    }
                }
            }
        }
    }
}

