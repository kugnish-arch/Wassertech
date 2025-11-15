package ru.wassertech.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ru.wassertech.feature.auth.LoginScreen
import ru.wassertech.feature.auth.AuthRoutes
import ru.wassertech.screen.HomeScreen
import ru.wassertech.client.ui.maintenance.MaintenanceSessionDetailScreen
import ru.wassertech.client.ui.sites.SitesScreen
import ru.wassertech.client.ui.sites.SiteDetailScreen
import ru.wassertech.client.ui.components.ComponentsScreen

/**
 * Навигационный граф приложения
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = AuthRoutes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoutes.HOME) {
                        // Очищаем стек навигации, чтобы нельзя было вернуться на экран логина
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        
        composable(AppRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        
        composable(
            route = AppRoutes.SITES,
            arguments = listOf(
                navArgument("clientId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            SitesScreen(
                clientId = clientId,
                onOpenSite = { siteId ->
                    navController.navigate(AppRoutes.siteDetail(siteId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = AppRoutes.SITE_DETAIL,
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: return@composable
            SiteDetailScreen(
                siteId = siteId,
                onOpenInstallation = { installationId ->
                    navController.navigate(AppRoutes.installation(installationId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = AppRoutes.INSTALLATION,
            arguments = listOf(
                navArgument("installationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val installationId = backStackEntry.arguments?.getString("installationId") ?: return@composable
            ComponentsScreen(
                installationId = installationId,
                onOpenMaintenanceHistory = { instId ->
                    navController.navigate(AppRoutes.maintenanceHistory(instId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
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
        
        // TODO: Добавить маршрут для истории ТО (MAINTENANCE_HISTORY)
    }
}

