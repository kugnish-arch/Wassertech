
package com.example.wassertech.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.wassertech.ui.clients.ClientDetailScreen
import com.example.wassertech.ui.clients.ClientsRoute
import com.example.wassertech.ui.hierarchy.ComponentsScreen
import com.example.wassertech.ui.hierarchy.SiteDetailScreen
import com.example.wassertech.ui.maintenance.MaintenanceHistoryScreen
import com.example.wassertech.ui.maintenance.MaintenanceScreen
import com.example.wassertech.ui.maintenance.MaintenanceSessionDetailScreen
import com.example.wassertech.ui.reports.ReportsScreen
import com.example.wassertech.ui.about.AboutScreen
import com.example.wassertech.ui.settings.SettingsScreen
import com.example.wassertech.ui.templates.TemplateEditorScreen
import com.example.wassertech.ui.templates.TemplatesScreen
import android.net.Uri
import com.example.wassertech.ui.common.NavigationBottomBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    navController: NavHostController,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null
) {
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route ?: "clients"
    val canNavigateBack = route != "clients" || navController.previousBackStackEntry != null

    var menuOpen by remember { mutableStateOf(false) }

    // Получаем название страницы из route
    val pageTitle = remember(route) {
        when {
            route.startsWith("clients") -> "Клиенты"
            route.startsWith("templates") -> "Шаблоны компонентов"
            route.startsWith("template_editor") -> "Редактор шаблона"
            route.startsWith("client/") -> "Клиент"
            route.startsWith("site/") -> "Объект"
            route.startsWith("installation/") -> "Установка"
            route.startsWith("maintenance_all") -> "Техническое обслуживание"
            route.startsWith("maintenance_edit") -> "Редактирование ТО"
            route.startsWith("maintenance_history") -> "История ТО"
            route.startsWith("maintenance_session") -> "Детали ТО"
            route.startsWith("reports") -> "Отчёты ТО"
            route.startsWith("settings") -> "Настройки"
            route.startsWith("about") -> "О программе"
            else -> "Wassertech CRM"
        }
    }

    TopAppBar(
        navigationIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canNavigateBack) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                } else {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Меню")
                        }
                        // Бургер-меню привязано к иконке
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Шаблоны") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate("templates") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("О программе") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate("about") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        title = {
            Text(
                text = pageTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        actions = {
            // Переключатель режима редактирования (если доступен)
            if (onToggleEdit != null) {
                IconButton(onClick = onToggleEdit) {
                    Icon(
                        imageVector = if (isEditing) Icons.Filled.Edit else Icons.Filled.Visibility,
                        contentDescription = if (isEditing) "Редактирование" else "Просмотр"
                    )
                }
            }
        }
    )
}

@Composable
fun AppNavHost(navController: NavHostController) {
    AppScaffold(navController)
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    AppScaffold(navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(navController: NavHostController) {
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    
    // Определяем, показывать ли переключатель редактирования
    val showEditToggle = currentRoute?.let { route ->
        route.startsWith("clients") || 
        route.startsWith("templates") ||
        route.startsWith("client/") ||
        route.startsWith("site/") ||
        route.startsWith("installation/")
    } ?: false
    
    // Состояние редактирования для разных экранов
    var clientsEditing by remember { mutableStateOf(false) }
    var otherEditing by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = { 
            AppTopBar(
                navController = navController,
                isEditing = if (currentRoute == "clients") clientsEditing else otherEditing,
                onToggleEdit = if (showEditToggle) {
                    if (currentRoute == "clients") {
                        { clientsEditing = !clientsEditing }
                    } else {
                        { otherEditing = !otherEditing }
                    }
                } else null
            )
        },
        bottomBar = {
            NavigationBottomBar(
                currentRoute = currentRoute,
                onNavigateToClients = {
                    navController.navigate("clients") {
                        popUpTo("clients") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToMaintenanceHistory = {
                    navController.navigate("maintenance_history") {
                        launchSingleTop = true
                    }
                },
                onNavigateToReports = {
                    navController.navigate("reports") {
                        launchSingleTop = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "clients",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("clients") {
                ClientsRoute(
                    isEditing = clientsEditing,
                    onToggleEdit = { clientsEditing = !clientsEditing },
                    onClientClick = { clientId ->
                        navController.navigate("client/$clientId") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("templates") {
                TemplatesScreen(onOpenTemplate = { id ->
                    navController.navigate("template_editor/$id")
                })
            }

            composable(
                "template_editor/{templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.StringType })
            ) { bse ->
                val id = bse.arguments?.getString("templateId") ?: return@composable
                TemplateEditorScreen(
                    templateId = id,
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                "client/{clientId}",
                arguments = listOf(navArgument("clientId") { type = NavType.StringType })
            ) { bse ->
                val clientId = bse.arguments?.getString("clientId") ?: return@composable
                ClientDetailScreen(
                    clientId = clientId,
                    onOpenSite = { siteId -> navController.navigate("site/$siteId") },
                    onOpenInstallation = { installationId -> navController.navigate("installation/$installationId") }
                )
            }

            composable(
                "site/{siteId}",
                arguments = listOf(navArgument("siteId") { type = NavType.StringType })
            ) { bse ->
                val siteId = bse.arguments?.getString("siteId") ?: return@composable
                SiteDetailScreen(
                    siteId = siteId,
                    onOpenInstallation = { installationId -> navController.navigate("installation/$installationId") }
                )
            }

            composable(
                "installation/{installationId}",
                arguments = listOf(navArgument("installationId") { type = NavType.StringType })
            ) { bse ->
                val installationId = bse.arguments?.getString("installationId") ?: return@composable
                ComponentsScreen(
                    installationId = installationId,
                    onStartMaintenance = { /* ... */ },
                    onStartMaintenanceAll = { siteId, installationName ->
                        navController.navigate(
                            "maintenance_all/$siteId/$installationId/${Uri.encode(installationName)}"
                        )
                    },
                    onOpenMaintenanceHistoryForInstallation = { id ->
                        navController.navigate("maintenance_history/$id")
                    }
                )
            }



            composable(
                route = "maintenance_all/{siteId}/{installationId}/{installationName}",
                arguments = listOf(
                    navArgument("siteId") { type = NavType.StringType },
                    navArgument("installationId") { type = NavType.StringType },
                    navArgument("installationName") { type = NavType.StringType }
                )
            ) { bse ->
                val siteId = bse.arguments?.getString("siteId")!!
                val installationId = bse.arguments?.getString("installationId")!!
                val installationName = bse.arguments?.getString("installationName")!!

                MaintenanceScreen(
                    siteId = siteId,
                    installationId = installationId,
                    installationName = installationName,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { id -> navController.navigate("maintenance_history/$id") },
                    sessionId = null
                )
            }

            // Редактирование существующей сессии ТО
            composable(
                route = "maintenance_edit/{sessionId}/{siteId}/{installationId}/{installationName}",
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("siteId") { type = NavType.StringType },
                    navArgument("installationId") { type = NavType.StringType },
                    navArgument("installationName") { type = NavType.StringType }
                )
            ) { bse ->
                val sessionId = bse.arguments?.getString("sessionId")!!
                val siteId = bse.arguments?.getString("siteId")!!
                val installationId = bse.arguments?.getString("installationId")!!
                val installationName = bse.arguments?.getString("installationName")!!

                MaintenanceScreen(
                    siteId = siteId,
                    installationId = installationId,
                    installationName = installationName,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { id -> navController.navigate("maintenance_history/$id") },
                    sessionId = sessionId
                )
            }

            // История ТО (общая)
            composable("maintenance_history") {
                MaintenanceHistoryScreen(
                    installationId = null,
                    onBack = { navController.navigateUp() },
                    onOpenSession = { sid -> navController.navigate("maintenance_session/$sid") },
                    onOpenReports = { navController.navigate("reports") }
                )
            }

            // История ТО по установке
            composable(
                "maintenance_history/{installationId}",
                arguments = listOf(navArgument("installationId") { type = NavType.StringType })
            ) { bse ->
                val installationId = bse.arguments?.getString("installationId")
                MaintenanceHistoryScreen(
                    installationId = installationId,
                    onBack = { navController.navigateUp() },
                    onOpenSession = { sid -> navController.navigate("maintenance_session/$sid") },
                    onOpenReports = { navController.navigate("reports") }
                )
            }
            
            // Экран отчётов ТО
            composable("reports") {
                ReportsScreen()
            }

            // Экран деталей ТО
            composable(
                "maintenance_session/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { bse ->
                val sessionId = bse.arguments?.getString("sessionId") ?: return@composable
                MaintenanceSessionDetailScreen(
                    sessionId = sessionId,
                    onNavigateToEdit = { sid, siteId, installationId, installationName ->
                        navController.navigate(
                            "maintenance_edit/$sid/$siteId/$installationId/${Uri.encode(installationName)}"
                        )
                    }
                )
            }
            
            // Экран настроек
            composable("settings") {
                SettingsScreen()
            }
            
            // Экран "О программе"
            composable("about") {
                AboutScreen()
            }
        }
    }
}


