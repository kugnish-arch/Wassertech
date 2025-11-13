package ru.wassertech.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import ru.wassertech.client.auth.AuthRepository
import ru.wassertech.client.data.OfflineModeManager
import ru.wassertech.core.ui.theme.WassertechTheme
import ru.wassertech.navigation.AppNavigation
import ru.wassertech.navigation.AppRoutes

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
                    val authRepository = remember { AuthRepository(this@MainActivity) }
                    val offlineModeManager = remember { OfflineModeManager.getInstance(this@MainActivity) }
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    val scope = rememberCoroutineScope()
                    
                    // Проверяем наличие токена и оффлайн режим при старте
                    LaunchedEffect(Unit) {
                        val isOfflineMode = offlineModeManager.isOfflineMode()
                        val isAuthenticated = authRepository.isAuthenticated()
                        
                        startDestination = when {
                            isOfflineMode -> {
                                // Оффлайн режим: пропускаем логин
                                AppRoutes.HOME
                            }
                            isAuthenticated -> {
                                // Онлайн режим с токеном
                                AppRoutes.HOME
                            }
                            else -> {
                                // Нужна авторизация
                                AppRoutes.LOGIN
                            }
                        }
                    }
                    
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

