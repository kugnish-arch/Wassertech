package ru.wassertech.client.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import ru.wassertech.navigation.AppRoutes
import ru.wassertech.feature.auth.AuthRoutes

@Composable
fun AppScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    
    Scaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                onLogout = {
                    navController.navigate(AuthRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        },
        bottomBar = {
            // Показываем нижнее меню только на экранах, отличных от LOGIN
            if (currentRoute != AuthRoutes.LOGIN) {
                NavigationBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToHome = {
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(AppRoutes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTemplates = {
                        navController.navigate(AppRoutes.TEMPLATES) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        // Переключаемся на вкладку настроек в HomeScreen
                        navController.navigate(AppRoutes.HOME_SETTINGS) {
                            popUpTo(AppRoutes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

