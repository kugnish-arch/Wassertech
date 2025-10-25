package com.example.wassertech.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wassertech.ui.clients.ClientsScreen
import com.example.wassertech.ui.empty.EmptyScreen
import com.example.wassertech.ui.hierarchy.ComponentsScreen
import com.example.wassertech.ui.hierarchy.InstallationsScreen
import com.example.wassertech.ui.hierarchy.SitesScreen
import com.example.wassertech.ui.maintenance.MaintenanceScreen

sealed class Route(val route: String, val title: String) {
    data object Clients : Route("clients", "Клиенты")
    data object Sites : Route("sites/{clientId}", "Объекты")
    data object Installations : Route("installations/{siteId}", "Установки")
    data object Components : Route("components/{installationId}", "Компоненты")
    data object Maintenance : Route("maintenance/{componentId}", "Провести ТО")
    data object Empty : Route("empty", "Пусто")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String) {
    TopAppBar(title = { Text(title) })
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    Scaffold(topBar = {
        val title = when (navController.currentBackStackEntry?.destination?.route) {
            "clients" -> Route.Clients.title
            "empty" -> Route.Empty.title
            else -> "Wassertech"
        }
        AppTopBar(title = title)
    }) { inner ->
        NavHost(
            navController = navController,
            startDestination = Route.Clients.route,
            modifier = Modifier.padding(inner)
        ) {
            composable(Route.Clients.route) {
                ClientsScreen(onOpenClient = { clientId ->
                    navController.navigate("sites/$clientId")
                })
            }
            composable(
                route = Route.Sites.route,
                arguments = listOf(navArgument("clientId"){ type = NavType.StringType })
            ) { backStack ->
                val clientId = backStack.arguments?.getString("clientId")!!
                SitesScreen(
                    clientId = clientId,
                    onOpenSite = { siteId -> navController.navigate("installations/$siteId") }
                )
            }
            composable(
                route = Route.Installations.route,
                arguments = listOf(navArgument("siteId"){ type = NavType.StringType })
            ) { backStack ->
                val siteId = backStack.arguments?.getString("siteId")!!
                InstallationsScreen(
                    siteId = siteId,
                    onOpenInstallation = { installationId -> navController.navigate("components/$installationId") },
                    onOpenSessions = { /* TODO sessions list */ }
                )
            }
            composable(
                route = Route.Components.route,
                arguments = listOf(navArgument("installationId"){ type = NavType.StringType })
            ) { backStack ->
                val installationId = backStack.arguments?.getString("installationId")!!
                ComponentsScreen(
                    installationId = installationId,
                    onStartMaintenance = { componentId -> navController.navigate("maintenance/$componentId") }
                )
            }
            composable(
                route = Route.Maintenance.route,
                arguments = listOf(navArgument("componentId"){ type = NavType.StringType })
            ) { backStack ->
                val componentId = backStack.arguments?.getString("componentId")!!
                MaintenanceScreen(componentId = componentId, onDone = { navController.popBackStack() })
            }
            composable(Route.Empty.route) { EmptyScreen() }
        }
    }
}
