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
import ru.wassertech.client.ui.templates.TemplatesScreen
import ru.wassertech.client.ui.common.AppScaffold
import ru.wassertech.client.ui.maintenance.MaintenanceHistoryScreen
import ru.wassertech.client.ui.icons.ClientIconPacksScreen
import ru.wassertech.client.ui.icons.ClientIconPackDetailScreen

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
                    // После успешного логина переходим на экран синхронизации
                    navController.navigate(AppRoutes.SYNC) {
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Экран синхронизации после логина
        composable(AppRoutes.SYNC) {
            ru.wassertech.client.ui.sync.PostLoginSyncScreen(
                onSyncComplete = {
                    // После успешной синхронизации переходим на основной экран
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.SYNC) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoOffline = {
                    // Переходим в оффлайн режим
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.SYNC) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        composable(AppRoutes.HOME) {
            AppScaffold(navController = navController) { paddingValues ->
                HomeScreen(navController = navController, paddingValues = paddingValues, initialTab = 0)
            }
        }
        
        composable(AppRoutes.HOME_SETTINGS) {
            AppScaffold(navController = navController) { paddingValues ->
                HomeScreen(navController = navController, paddingValues = paddingValues, initialTab = 1)
            }
        }
        
        composable(
            route = AppRoutes.SITES,
            arguments = listOf(
                navArgument("clientId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: return@composable
            AppScaffold(navController = navController) { paddingValues ->
                SitesScreen(
                    clientId = clientId,
                    onOpenSite = { siteId ->
                        navController.navigate(AppRoutes.siteDetail(siteId))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    paddingValues = paddingValues
                )
            }
        }
        
        composable(
            route = AppRoutes.SITE_DETAIL,
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: return@composable
            AppScaffold(navController = navController) { paddingValues ->
                SiteDetailScreen(
                    siteId = siteId,
                    onOpenInstallation = { installationId ->
                        navController.navigate(AppRoutes.installation(installationId))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    paddingValues = paddingValues
                )
            }
        }
        
        composable(
            route = AppRoutes.INSTALLATION,
            arguments = listOf(
                navArgument("installationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val installationId = backStackEntry.arguments?.getString("installationId") ?: return@composable
            AppScaffold(navController = navController) { paddingValues ->
                ComponentsScreen(
                    installationId = installationId,
                    onOpenMaintenanceHistory = { instId ->
                        navController.navigate(AppRoutes.maintenanceHistory(instId))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    paddingValues = paddingValues
                )
            }
        }
        
        composable(
            route = AppRoutes.SESSION_DETAIL,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            AppScaffold(navController = navController) { paddingValues ->
                MaintenanceSessionDetailScreen(
                    sessionId = sessionId,
                    onNavigateToEdit = { _, _, _, _ -> }, // Пока не реализовано редактирование
                    paddingValues = paddingValues
                )
            }
        }
        
        composable(AppRoutes.TEMPLATES) {
            AppScaffold(navController = navController) { paddingValues ->
                TemplatesScreen(
                    onOpenTemplate = { templateId ->
                        navController.navigate(AppRoutes.templateEditor(templateId))
                    },
                    paddingValues = paddingValues
                )
            }
        }
        
        composable(
            route = AppRoutes.TEMPLATE_EDITOR,
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable
            AppScaffold(navController = navController) { paddingValues ->
                // TODO: Добавить TemplateEditorScreen для app-client
                // Пока просто возвращаемся назад
                navController.popBackStack()
            }
        }
        
        composable(
            route = AppRoutes.MAINTENANCE_HISTORY,
            arguments = listOf(
                navArgument("installationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val installationId = backStackEntry.arguments?.getString("installationId") ?: return@composable
            AppScaffold(navController = navController) { paddingValues ->
                MaintenanceHistoryScreen(
                    installationId = installationId,
                    onOpenSession = { sessionId ->
                        navController.navigate(AppRoutes.sessionDetail(sessionId))
                    },
                    paddingValues = paddingValues
                )
            }
        }
        
        // Экран списка икон-паков для клиента
        composable(AppRoutes.CLIENT_ICON_PACKS) {
            AppScaffold(navController = navController) { paddingValues ->
                ClientIconPacksScreen(
                    onPackClick = { packId ->
                        navController.navigate(AppRoutes.clientIconPackDetail(packId)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Экран детального просмотра икон-пака для клиента
        composable(
            route = AppRoutes.CLIENT_ICON_PACK_DETAIL,
            arguments = listOf(
                navArgument("packId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId") ?: return@composable
            AppScaffold(navController = navController) { paddingValues ->
                ClientIconPackDetailScreen(
                    packId = packId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

