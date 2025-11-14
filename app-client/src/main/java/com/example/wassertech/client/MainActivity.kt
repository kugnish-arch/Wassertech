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
import ru.wassertech.core.auth.UserAuthService
import ru.wassertech.core.ui.theme.WassertechTheme
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
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    
                    // Проверяем состояние входа при старте
                    LaunchedEffect(Unit) {
                        startDestination = if (UserAuthService.isLoggedIn(context)) {
                            AppRoutes.HOME
                        } else {
                            AuthRoutes.LOGIN
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

