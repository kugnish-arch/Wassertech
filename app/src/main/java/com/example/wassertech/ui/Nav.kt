package com.example.wassertech.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.wassertech.ui.hierarchy.ComponentsScreen
import com.example.wassertech.ui.hierarchy.SiteDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavHostController) {
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route ?: "clients"
    val canNavigateBack = route != "clients" || navController.previousBackStackEntry != null

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        },
        title = {
            Image(painter = painterResource(id = R.drawable.logo_wassertech), contentDescription = "Wassertech")
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
    Scaffold(topBar = { AppTopBar(navController) }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "clients",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("clients") {
                Column {
                    SectionHeader("Клиенты")
                    ClientsScreen(onOpenClient = { clientId -> navController.navigate("client/" + clientId) })
                }
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
                        onOpenSite = { siteId -> navController.navigate("site/" + siteId) },
                        onOpenInstallation = { installationId -> navController.navigate("installation/" + installationId) }
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
                        onOpenInstallation = { installationId -> navController.navigate("installation/" + installationId) }
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
                        onStartMaintenance = { /* TODO */ },
                        onStartMaintenanceAll = { /* TODO */ }
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
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
