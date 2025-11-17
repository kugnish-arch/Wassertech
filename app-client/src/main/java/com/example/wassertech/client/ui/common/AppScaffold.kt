package ru.wassertech.client.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import ru.wassertech.navigation.AppRoutes
import ru.wassertech.feature.auth.AuthRoutes

// Data class для состояния редактирования
data class EditingState(
    val isEditing: Boolean,
    val onToggle: () -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit
)

// CompositionLocal для передачи состояния редактирования в экраны
val LocalEditingState = compositionLocalOf<EditingState?> { null }

@Composable
fun AppScaffold(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    
    // Определяем, показывать ли переключатель редактирования
    val showEditToggle = currentRoute?.let { route ->
        route == AppRoutes.HOME ||
        route.startsWith("sites/") ||
        route.startsWith("site/") ||
        route.startsWith("installation/") ||
        route.startsWith("templates") ||
        route.startsWith("maintenance_history/")
    } ?: false
    
    // Состояние редактирования для разных экранов
    var sitesEditing by remember { mutableStateOf(false) }
    var siteDetailEditing by remember { mutableStateOf(false) }
    var installationEditing by remember { mutableStateOf(false) }
    var templatesEditing by remember { mutableStateOf(false) }
    var maintenanceHistoryEditing by remember { mutableStateOf(false) }
    
    // Определяем текущее состояние редактирования и функции управления
    val editingState = remember(
        currentRoute,
        sitesEditing,
        siteDetailEditing,
        installationEditing,
        templatesEditing,
        maintenanceHistoryEditing
    ) {
        when {
            currentRoute == AppRoutes.HOME || currentRoute?.startsWith("sites/") == true -> EditingState(
                isEditing = sitesEditing,
                onToggle = { sitesEditing = !sitesEditing },
                onSave = { sitesEditing = false },
                onCancel = { sitesEditing = false }
            )
            currentRoute?.startsWith("site/") == true -> EditingState(
                isEditing = siteDetailEditing,
                onToggle = { siteDetailEditing = !siteDetailEditing },
                onSave = { siteDetailEditing = false },
                onCancel = { siteDetailEditing = false }
            )
            currentRoute?.startsWith("installation/") == true -> EditingState(
                isEditing = installationEditing,
                onToggle = { installationEditing = !installationEditing },
                onSave = { installationEditing = false },
                onCancel = { installationEditing = false }
            )
            currentRoute?.startsWith("templates") == true -> EditingState(
                isEditing = templatesEditing,
                onToggle = { templatesEditing = !templatesEditing },
                onSave = { templatesEditing = false },
                onCancel = { templatesEditing = false }
            )
            currentRoute?.startsWith("maintenance_history/") == true -> EditingState(
                isEditing = maintenanceHistoryEditing,
                onToggle = { maintenanceHistoryEditing = !maintenanceHistoryEditing },
                onSave = { maintenanceHistoryEditing = false },
                onCancel = { maintenanceHistoryEditing = false }
            )
            else -> EditingState(
                isEditing = false,
                onToggle = { },
                onSave = { },
                onCancel = { }
            )
        }
    }
    
    val currentEditing = editingState.isEditing
    val toggleEditing = editingState.onToggle
    val saveEditing = editingState.onSave
    val cancelEditing = editingState.onCancel
    
    Scaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                isEditing = currentEditing,
                onToggleEdit = if (showEditToggle) toggleEditing else null,
                onSave = if (showEditToggle && currentEditing) saveEditing else null,
                onCancel = if (showEditToggle && currentEditing) cancelEditing else null,
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
        // Предоставляем состояние редактирования через CompositionLocal
        CompositionLocalProvider(
            LocalEditingState provides editingState
        ) {
            content(paddingValues)
        }
    }
}

