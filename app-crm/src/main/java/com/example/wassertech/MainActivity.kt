package ru.wassertech.crm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import ru.wassertech.auth.UserAuthService
import ru.wassertech.ui.AppNavHost
import ru.wassertech.core.ui.splash.SplashRouteFixed
import ru.wassertech.core.ui.theme.WassertechTheme
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
    val context = LocalContext.current
    val navController = rememberNavController()
    
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
                        navController.navigate("main") {
                            popUpTo(AuthRoutes.LOGIN) { inclusive = true }
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
