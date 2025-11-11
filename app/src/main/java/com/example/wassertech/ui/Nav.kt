
package com.example.wassertech.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import com.example.wassertech.ui.theme.EditButtonStyle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import androidx.compose.foundation.clickable
import com.example.wassertech.R
import com.example.wassertech.auth.UserAuthService
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

// Вспомогательные функции для анимаций переходов
private fun slideInFromRight() = slideInHorizontally(
    initialOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(500)
) + fadeIn(animationSpec = tween(500))

private fun slideOutToLeft() = slideOutHorizontally(
    targetOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(500)
) + fadeOut(animationSpec = tween(500))

private fun slideInFromLeft() = slideInHorizontally(
    initialOffsetX = { fullWidth -> -fullWidth },
    animationSpec = tween(500)
) + fadeIn(animationSpec = tween(500))

private fun slideOutToRight() = slideOutHorizontally(
    targetOffsetX = { fullWidth -> fullWidth },
    animationSpec = tween(500)
) + fadeOut(animationSpec = tween(500))

// Легкий fade transition для некоторых экранов
private fun fadeInTransition() = fadeIn(animationSpec = tween(500))
private fun fadeOutTransition() = fadeOut(animationSpec = tween(500))

// Data class для состояния редактирования
private data class EditingState(
    val isEditing: Boolean,
    val onToggle: () -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    navController: NavHostController,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onOfflineIconClick: ((Offset) -> Unit)? = null
) {
    val context = LocalContext.current
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route ?: "clients"
    val canNavigateBack = route != "clients" || navController.previousBackStackEntry != null
    val isOfflineMode = UserAuthService.isOfflineMode(context)

    var menuOpen by remember { mutableStateOf(false) }

    // Получаем название страницы из route
    val pageTitle = remember(route) {
        when {
            route == "login" -> "Вход"
            route.startsWith("clients") -> "Клиенты"
            route.startsWith("templates") -> "Шаблоны компонентов"
            route.startsWith("template_editor") -> "Редактор шаблона"
            route.startsWith("client/") -> "Клиент"
            route.startsWith("site/") -> "Объект"
            route.startsWith("installation/") -> "Установка"
            route.startsWith("maintenance_all") -> "Техническое обслуживание"
            route.startsWith("maintenance_edit") -> "Редактирование ТО"
            route.startsWith("maintenance_history") -> "История обслуживания"
            route.startsWith("maintenance_session") -> "Детали обслуживания"
            route.startsWith("reports") -> "Отчёты обслуживания"
            route.startsWith("settings") -> "Настройки"
            route.startsWith("about") -> "О программе"
            else -> "Wassertech CRM"
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background, // Фон совпадает с фоном приложения
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier
            .shadow(elevation = 2.dp) // Нежная тень для отделения от контента
            .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(0.dp)), // Тонкая рамка
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
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.background(com.example.wassertech.ui.theme.DropdownMenuBackground) // Практически белый фон для выпадающих меню
                        ) {
                            DropdownMenuItem(
                                text = { Text("Шаблоны") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate("templates") {
                                        launchSingleTop = true
                                    }
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate("settings") {
                                        launchSingleTop = true
                                    }
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            DropdownMenuItem(
                                text = { Text("О программе") },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate("about") {
                                        launchSingleTop = true
                                    }
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (onLogout != null) {
                                DropdownMenuItem(
                                    text = { Text("Выйти") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.ExitToApp,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onLogout()
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка оффлайн режима (если включен)
                if (isOfflineMode) {
                    var iconPosition by remember { mutableStateOf<Offset?>(null) }
                    
                    IconButton(
                        onClick = {
                            iconPosition?.let { position ->
                                onOfflineIconClick?.invoke(position)
                            }
                        },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val position = coordinates.localToWindow(Offset.Zero)
                            iconPosition = position
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.cloud_offline_outline),
                            contentDescription = "Оффлайн режим",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Переключатель режима редактирования (если доступен)
                if (onToggleEdit != null || onSave != null || onCancel != null) {
                    if (isEditing) {
                        // В режиме редактирования показываем две кнопки: Отмена и Сохранить
                        // Кнопка "Отмена" (красный кружок с крестом)
                        if (onCancel != null) {
                            IconButton(onClick = onCancel) {
                                Icon(
                                    imageVector = EditButtonStyle.CancelIcon,
                                    contentDescription = "Отмена",
                                    tint = EditButtonStyle.CancelIconColor
                                )
                            }
                        }
                        // Кнопка "Сохранить" (зеленый кружок с галочкой)
                        if (onSave != null) {
                            IconButton(onClick = onSave) {
                                Icon(
                                    imageVector = EditButtonStyle.SaveIcon,
                                    contentDescription = "Сохранить",
                                    tint = EditButtonStyle.SaveIconColor
                                )
                            }
                        }
                    } else {
                        // Вне режима редактирования показываем кнопку "Редактировать"
                        if (onToggleEdit != null) {
                            IconButton(onClick = onToggleEdit) {
                                Icon(
                                    imageVector = EditButtonStyle.EditIcon,
                                    contentDescription = "Редактировать"
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

/**
 * Компонент тултипа оффлайн режима с жёлтым фоном, иконкой и крестиком для закрытия
 * Позиционируется относительно anchorPosition (координаты иконки)
 */
@Composable
private fun OfflineModeTooltip(
    anchorPosition: Offset?,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    var containerPosition by remember { mutableStateOf<Offset?>(null) }
    
    // Используем Box с fillMaxSize для верхнего слоя
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
            .zIndex(1000f) // Верхний слой
            .onGloballyPositioned { coordinates ->
                // Получаем позицию контейнера в window координатах
                containerPosition = coordinates.localToWindow(Offset.Zero)
            },
        contentAlignment = Alignment.TopStart
    ) {
        // Полупрозрачный фон
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxSize()
        ) {}

        // Карточка с подсказкой - позиционируется относительно иконки
        if (anchorPosition != null && containerPosition != null) {
            // Вычисляем относительное смещение от контейнера до иконки
            val relativeX = with(density) { 
                (anchorPosition.x - containerPosition!!.x).toDp()
            }
            val relativeY = with(density) { 
                (anchorPosition.y - containerPosition!!.y).toDp()
            }
            
            Card(
                modifier = Modifier
                    .offset(
                        x = relativeX + 24.dp - 265.dp, // Сдвигаем еще левее (дополнительно -40dp от предыдущего значения)
                        y = relativeY + 72.dp // Опускаем на 40dp ниже (было 32dp, стало 72dp)
                    )
                    .widthIn(max = 300.dp)
                    .clickable(enabled = false) {},
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEB3B).copy(alpha = 0.85f) // Жёлтый фон с прозрачностью 85%
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Заголовок с иконкой оффлайн режима и крестиком
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.cloud_offline_outline),
                                contentDescription = null,
                                tint = Color(0xFF1A1A1A), // Тёмный цвет для иконки
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Оффлайн режим",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1A1A1A) // Тёмный текст на жёлтом фоне
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = Color(0xFF1A1A1A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Текст подсказки
                    Text(
                        text = "Вы используете оффлайн режим приложения. Функционал работы с удалённым сервером будет недоступен до повторного входа в приложение.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, onLogout: (() -> Unit)? = null) {
    AppScaffold(navController, onLogout)
}

@Composable
fun AppNavHost(onLogout: (() -> Unit)? = null) {
    val navController = rememberNavController()
    AppScaffold(navController, onLogout)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(navController: NavHostController, onLogout: (() -> Unit)? = null) {
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    
    // Определяем, показывать ли переключатель редактирования
    val showEditToggle = currentRoute?.let { route ->
        route.startsWith("clients") || 
        route.startsWith("templates") ||
        route.startsWith("client/") ||
        route.startsWith("site/") ||
        route.startsWith("installation/") ||
        route.startsWith("reports") ||
        route.startsWith("maintenance_history")
    } ?: false
    
    // Состояние редактирования для разных экранов
    var clientsEditing by remember { mutableStateOf(false) }
    var clientDetailEditing by remember { mutableStateOf(false) }
    var siteEditing by remember { mutableStateOf(false) }
    var installationEditing by remember { mutableStateOf(false) }
    var templatesEditing by remember { mutableStateOf(false) }
    var reportsEditing by remember { mutableStateOf(false) }
    var maintenanceHistoryEditing by remember { mutableStateOf(false) }
    
    // Определяем текущее состояние редактирования и функции управления
    // Добавляем зависимости на все состояния редактирования для реактивного обновления
    val editingState = remember(
        currentRoute,
        clientsEditing,
        clientDetailEditing,
        siteEditing,
        installationEditing,
        templatesEditing,
        reportsEditing,
        maintenanceHistoryEditing
    ) {
        when {
            currentRoute == "clients" -> EditingState(
                isEditing = clientsEditing,
                onToggle = { clientsEditing = !clientsEditing },
                onSave = { clientsEditing = false },
                onCancel = { clientsEditing = false }
            )
            currentRoute?.startsWith("client/") == true -> EditingState(
                isEditing = clientDetailEditing,
                onToggle = { clientDetailEditing = !clientDetailEditing },
                onSave = { clientDetailEditing = false },
                onCancel = { clientDetailEditing = false }
            )
            currentRoute?.startsWith("site/") == true -> EditingState(
                isEditing = siteEditing,
                onToggle = { siteEditing = !siteEditing },
                onSave = { siteEditing = false },
                onCancel = { siteEditing = false }
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
            currentRoute?.startsWith("reports") == true -> EditingState(
                isEditing = reportsEditing,
                onToggle = { reportsEditing = !reportsEditing },
                onSave = { reportsEditing = false },
                onCancel = { reportsEditing = false }
            )
            currentRoute?.startsWith("maintenance_history") == true -> EditingState(
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
    
    // Состояние для тултипа оффлайн режима
    var showOfflineTooltip by remember { mutableStateOf(false) }
    var offlineIconPosition by remember { mutableStateOf<Offset?>(null) }
    
    Scaffold(
        topBar = { 
            AppTopBar(
                navController = navController,
                isEditing = currentEditing,
                onToggleEdit = if (showEditToggle) toggleEditing else null,
                onSave = if (showEditToggle && currentEditing) saveEditing else null,
                onCancel = if (showEditToggle && currentEditing) cancelEditing else null,
                onLogout = onLogout,
                onOfflineIconClick = { position ->
                    offlineIconPosition = position
                    showOfflineTooltip = true
                }
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
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "clients", // По умолчанию все пользователи начинают с "clients" (см. UserPermissions.startScreen)
                modifier = Modifier.padding(innerPadding)
            ) {
            composable(
                route = "clients",
                enterTransition = { fadeInTransition() },
                exitTransition = { slideOutToLeft() },
                popEnterTransition = { slideInFromLeft() },
                popExitTransition = { fadeOutTransition() }
            ) {
                ClientsRoute(
                    isEditing = clientsEditing,
                    onToggleEdit = { clientsEditing = !clientsEditing },
                    onCancel = { clientsEditing = false },
                    onClientClick = { clientId ->
                        navController.navigate("client/$clientId") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(
                route = "templates",
                enterTransition = { fadeInTransition() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { fadeOutTransition() }
            ) {
                TemplatesScreen(
                    isEditing = templatesEditing,
                    onToggleEdit = { templatesEditing = !templatesEditing },
                    onOpenTemplate = { id ->
                        navController.navigate("template_editor/$id")
                    }
                )
            }

            composable(
                route = "template_editor/{templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.StringType }),
                enterTransition = { slideInFromRight() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { slideOutToRight() }
            ) { bse ->
                val id = bse.arguments?.getString("templateId") ?: return@composable
                TemplateEditorScreen(
                    templateId = id,
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = "client/{clientId}",
                arguments = listOf(navArgument("clientId") { type = NavType.StringType }),
                enterTransition = { slideInFromRight() },
                exitTransition = { slideOutToLeft() },
                popEnterTransition = { slideInFromLeft() },
                popExitTransition = { slideOutToRight() }
            ) { bse ->
                val clientId = bse.arguments?.getString("clientId") ?: return@composable
                ClientDetailScreen(
                    clientId = clientId,
                    isEditing = clientDetailEditing,
                    onToggleEdit = { clientDetailEditing = !clientDetailEditing },
                    onOpenSite = { siteId -> navController.navigate("site/$siteId") },
                    onOpenInstallation = { installationId -> navController.navigate("installation/$installationId") }
                )
            }

            composable(
                route = "site/{siteId}",
                arguments = listOf(navArgument("siteId") { type = NavType.StringType }),
                enterTransition = { slideInFromRight() },
                exitTransition = { slideOutToLeft() },
                popEnterTransition = { slideInFromLeft() },
                popExitTransition = { slideOutToRight() }
            ) { bse ->
                val siteId = bse.arguments?.getString("siteId") ?: return@composable
                SiteDetailScreen(
                    siteId = siteId,
                    isEditing = siteEditing,
                    onToggleEdit = { siteEditing = !siteEditing },
                    onOpenInstallation = { installationId -> navController.navigate("installation/$installationId") }
                )
            }

            composable(
                route = "installation/{installationId}",
                arguments = listOf(navArgument("installationId") { type = NavType.StringType }),
                enterTransition = { slideInFromRight() },
                exitTransition = { slideOutToLeft() },
                popEnterTransition = { slideInFromLeft() },
                popExitTransition = { slideOutToRight() }
            ) { bse ->
                val installationId = bse.arguments?.getString("installationId") ?: return@composable
                ComponentsScreen(
                    installationId = installationId,
                    isEditing = installationEditing,
                    onToggleEdit = { installationEditing = !installationEditing },
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
                ),
                enterTransition = { slideInFromRight() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { slideOutToRight() }
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
                ),
                enterTransition = { slideInFromRight() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { slideOutToRight() }
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
            composable(
                route = "maintenance_history",
                enterTransition = { fadeInTransition() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { fadeOutTransition() }
            ) {
                MaintenanceHistoryScreen(
                    installationId = null,
                    isEditing = maintenanceHistoryEditing,
                    onToggleEdit = { maintenanceHistoryEditing = !maintenanceHistoryEditing },
                    onBack = { navController.navigateUp() },
                    onOpenSession = { sid -> navController.navigate("maintenance_session/$sid") },
                    onOpenReports = { navController.navigate("reports") }
                )
            }

            // История ТО по установке
            composable(
                route = "maintenance_history/{installationId}",
                arguments = listOf(navArgument("installationId") { type = NavType.StringType }),
                enterTransition = { slideInFromRight() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { slideOutToRight() }
            ) { bse ->
                val installationId = bse.arguments?.getString("installationId")
                MaintenanceHistoryScreen(
                    installationId = installationId,
                    isEditing = maintenanceHistoryEditing,
                    onToggleEdit = { maintenanceHistoryEditing = !maintenanceHistoryEditing },
                    onBack = { navController.navigateUp() },
                    onOpenSession = { sid -> navController.navigate("maintenance_session/$sid") },
                    onOpenReports = { navController.navigate("reports") }
                )
            }
            
            // Экран отчётов ТО
            composable(
                route = "reports",
                enterTransition = { fadeInTransition() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { fadeOutTransition() }
            ) {
                ReportsScreen(
                    isEditing = reportsEditing,
                    onToggleEdit = { reportsEditing = !reportsEditing }
                )
            }

            // Экран деталей ТО
            composable(
                route = "maintenance_session/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
                enterTransition = { slideInFromRight() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { slideOutToRight() }
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
            composable(
                route = "settings",
                enterTransition = { fadeInTransition() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { fadeOutTransition() }
            ) {
                SettingsScreen()
            }
            
            // Экран "О программе"
            composable(
                route = "about",
                enterTransition = { fadeInTransition() },
                exitTransition = { fadeOutTransition() },
                popEnterTransition = { fadeInTransition() },
                popExitTransition = { fadeOutTransition() }
            ) {
                AboutScreen()
            }
            }
            
            // Тултип оффлайн режима (отображается поверх всего контента)
            if (showOfflineTooltip && offlineIconPosition != null) {
                OfflineModeTooltip(
                    anchorPosition = offlineIconPosition,
                    onDismiss = { showOfflineTooltip = false }
                )
            }
        }
    }
}


