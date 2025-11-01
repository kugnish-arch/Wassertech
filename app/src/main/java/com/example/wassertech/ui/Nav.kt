package com.example.wassertech.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.wassertech.ui.clients.ClientsRoute // ← добавили импорт маршрута
import com.example.wassertech.ui.hierarchy.ComponentsScreen
import com.example.wassertech.ui.hierarchy.SiteDetailScreen

import com.example.wassertech.ui.maintenance.MaintenanceHistoryScreen
import com.example.wassertech.ui.templates.TemplateEditorScreen
import com.example.wassertech.ui.templates.TemplatesScreen
import com.example.wassertech.ui.maintenance.MaintenanceScreen
import android.net.Uri


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavHostController) {
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route ?: "clients"
    val canNavigateBack = route != "clients" || navController.previousBackStackEntry != null

    var menuOpen by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        },
        title = {
            Image(
                painter = painterResource(id = R.drawable.logo_wassertech),
                contentDescription = "Wassertech"
            )
        },
        actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Клиенты") },
                    onClick = {
                        menuOpen = false
                        navController.navigate("clients") {
                            popUpTo("clients") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
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
                    text = { Text("История ТО") },
                    onClick = {
                        menuOpen = false
                        navController.navigate("maintenance_history") {
                            launchSingleTop = true
                        }
                    }
                )
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
    Scaffold(topBar = { AppTopBar(navController)}) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "clients",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("clients") {
                Column {
                    SectionHeader("Клиенты")
                    // Было: ClientsScreen(onOpenClient = { ... })
                    // Стало: маршрут-обёртка, сам подписывается на VM и передает данные в UI
                    ClientsRoute(
                        onClientClick = { clientId ->
                            navController.navigate("client/$clientId") {
                                launchSingleTop = true
                                restoreState = true
                            }                        }
                    )
                }
            }

            composable("templates") {
                Column {
                    SectionHeader("Шаблоны компонентов")
                    TemplatesScreen(onOpenTemplate = { id ->
                        navController.navigate("template_editor/$id")
                    })
                }
            }

            composable(
                "template_editor/{templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.StringType })
            ) { bse ->
                val id = bse.arguments?.getString("templateId") ?: return@composable
                TemplateEditorScreen(templateId = id)
            }

            composable(
                "client/{clientId}",
                arguments = listOf(navArgument("clientId") { type = NavType.StringType })
            ) { bse ->
                val clientId = bse.arguments?.getString("clientId") ?: return@composable
                Column {
                    SectionHeader("Клиент")
                    ClientDetailScreen(
                        clientId = clientId,
                        onOpenSite = { siteId -> navController.navigate("site/$siteId") },
                        onOpenInstallation = { installationId -> navController.navigate("installation/$installationId") }
                    )
                }
            }

            composable(
                "site/{siteId}",
                arguments = listOf(navArgument("siteId") { type = NavType.StringType })
            ) { bse ->
                val siteId = bse.arguments?.getString("siteId") ?: return@composable
                Column {
                    SectionHeader("Объект")
                    SiteDetailScreen(
                        siteId = siteId,
                        onOpenInstallation = { installationId -> navController.navigate("installation/$installationId") }
                    )
                }
            }

            composable(
                "installation/{installationId}",
                arguments = listOf(navArgument("installationId") { type = NavType.StringType })
            ) { bse ->
                val installationId = bse.arguments?.getString("installationId") ?: return@composable
                Column {
                    SectionHeader("Установка")
                    ComponentsScreen(
                        installationId = installationId,
                        onStartMaintenance = { /* unused single component */ },
                        onStartMaintenanceAll = { siteId, installationName ->
                            val encodedName = Uri.encode(installationName)
                            navController.navigate("maintenance_all/$siteId/$installationId/$encodedName")
                        }

                    )
                }
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
                    onNavigateToHistory = { id -> navController.navigate("maintenance_history/$id") }
                )
            }



            // --- История ТО: общий экран ---
            composable("maintenance_history") {
                Column {
                    SectionHeader("История ТО")
                    MaintenanceHistoryScreen(
                        installationId = null,
                        onBack = { navController.navigateUp() }
                    )
                }
            }

            // --- История ТО конкретной установки ---
            composable(
                "maintenance_history/{installationId}",
                arguments = listOf(navArgument("installationId") { type = NavType.StringType })
            ) { bse ->
                val installationId = bse.arguments?.getString("installationId")
                Column {
                    SectionHeader("История ТО")
                    MaintenanceHistoryScreen(
                        installationId = installationId,
                        onBack = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Surface(tonalElevation = 1.dp) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
