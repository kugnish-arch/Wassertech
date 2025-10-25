package com.example.wassertech.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wassertech.R
import com.example.wassertech.ui.clients.ClientDetailScreen
import com.example.wassertech.ui.clients.ClientsScreen
import com.example.wassertech.ui.empty.EmptyScreen
import com.example.wassertech.ui.hierarchy.ComponentsScreen
import com.example.wassertech.ui.hierarchy.InstallationsScreen
import com.example.wassertech.ui.hierarchy.SitesScreen
import com.example.wassertech.ui.maintenance.MaintenanceAllScreen
import com.example.wassertech.ui.maintenance.MaintenanceScreen
import kotlinx.coroutines.launch

sealed class Route(val route: String, val title: String) {
    data object Clients : Route("clients", "Клиенты")
    data object ClientDetail : Route("client/{clientId}", "Заказчик")
    data object Sites : Route("sites/{clientId}", "Объекты")
    data object Installations : Route("installations/{siteId}", "Установки")
    data object Components : Route("components/{installationId}", "Компоненты")
    data object Maintenance : Route("maintenance/{componentId}", "Провести ТО (компонент)")
    data object MaintenanceAll : Route("maintenance_all/{installationId}", "Провести ТО")
    data object Empty : Route("empty", "Пусто")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    canNavigateBack: Boolean,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                }
            } else {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Filled.Menu, contentDescription = "Меню")
                }
            }
        },
        title = {
            Row(modifier = Modifier.padding(end = 4.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.logo_wassertech),
                    contentDescription = null,
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 40.dp)
                )
            }
        }
    )
}

@Composable
private fun SectionSubtitle(title: String) {
    Surface(tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""
    val canBack = navController.previousBackStackEntry != null
    val title = when {
        currentRoute.startsWith("client/") -> Route.ClientDetail.title
        currentRoute.startsWith("sites/") -> Route.Sites.title
        currentRoute.startsWith("installations/") -> Route.Installations.title
        currentRoute.startsWith("components/") -> Route.Components.title
        currentRoute.startsWith("maintenance_all/") -> Route.MaintenanceAll.title
        currentRoute.startsWith("maintenance/") -> Route.Maintenance.title
        currentRoute == "clients" -> Route.Clients.title
        currentRoute == "empty" -> Route.Empty.title
        else -> ""
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Навигация", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(
                    label = { Text("Клиенты") },
                    selected = currentRoute == Route.Clients.route,
                    onClick = {
                        navController.navigate(Route.Clients.route) {
                            popUpTo(Route.Clients.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Пустой экран") },
                    selected = currentRoute == Route.Empty.route,
                    onClick = {
                        navController.navigate(Route.Empty.route) {
                            popUpTo(Route.Clients.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    canNavigateBack = canBack,
                    onBack = { navController.popBackStack() },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        ) { inner ->
            Column(Modifier.padding(inner)) {
                if (title.isNotEmpty()) {
                    SectionSubtitle(title = title)
                }
                NavHost(
                    navController = navController,
                    startDestination = Route.Clients.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Route.Clients.route) {
                        ClientsScreen(onOpenClient = { clientId ->
                            navController.navigate("client/" + clientId)
                        })
                    }
                    composable(
                        route = Route.ClientDetail.route,
                        arguments = listOf(navArgument("clientId"){ type = NavType.StringType })
                    ) { backStack ->
                        val clientId = backStack.arguments?.getString("clientId")!!
                        ClientDetailScreen(
                            clientId = clientId,
                            onOpenComponents = { installationId ->
                                navController.navigate("components/$installationId")
                            }
                        )
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
                            onOpenSessions = { }
                        )
                    }
                    composable(
                        route = Route.Components.route,
                        arguments = listOf(navArgument("installationId"){ type = NavType.StringType })
                    ) { backStack ->
                        val installationId = backStack.arguments?.getString("installationId")!!
                        ComponentsScreen(
                            installationId = installationId,
                            onStartMaintenance = { componentId -> navController.navigate("maintenance/$componentId") },
                            onStartMaintenanceAll = { id -> navController.navigate("maintenance_all/$id") }
                        )
                    }
                    composable(
                        route = Route.Maintenance.route,
                        arguments = listOf(navArgument("componentId"){ type = NavType.StringType })
                    ) { backStack ->
                        val componentId = backStack.arguments?.getString("componentId")!!
                        MaintenanceScreen(componentId = componentId, onDone = { navController.popBackStack() })
                    }
                    composable(
                        route = Route.MaintenanceAll.route,
                        arguments = listOf(navArgument("installationId"){ type = NavType.StringType })
                    ) { backStack ->
                        val instId = backStack.arguments?.getString("installationId")!!
                        MaintenanceAllScreen(installationId = instId, onDone = { navController.popBackStack() })
                    }
                    composable(Route.Empty.route) { EmptyScreen() }
                }
            }
        }
    }
}
