package ru.wassertech.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.wassertech.feature.auth.LoginScreen
import ru.wassertech.screen.HomeScreen
import ru.wassertech.client.ui.maintenance.MaintenanceSessionDetailScreen

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
            HomeScreen(navController = navController)
        }
        
        composable(
            route = AppRoutes.SESSION_DETAIL,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            MaintenanceSessionDetailScreen(
                sessionId = sessionId,
                onNavigateToEdit = { _, _, _, _ -> } // Пока не реализовано редактирование
            )
        }
    }
}

