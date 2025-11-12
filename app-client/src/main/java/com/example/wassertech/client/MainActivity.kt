package ru.wassertech.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import ru.wassertech.core.auth.UserAuthService
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
                    
                    // Определяем стартовый экран в зависимости от статуса авторизации
                    val startDestination = if (UserAuthService.isLoggedIn(this)) {
                        AppRoutes.HOME
                    } else {
                        AppRoutes.LOGIN
                    }
                    
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}

