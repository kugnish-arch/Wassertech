package ru.wassertech.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.wassertech.feature.auth.LoginScreen
import ru.wassertech.screen.HomeScreen

/**
 * Навигационный граф приложения
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = AppRoutes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        // Очищаем стек навигации, чтобы нельзя было вернуться на экран логина
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(AppRoutes.HOME) {
            HomeScreen()
        }
    }
}

