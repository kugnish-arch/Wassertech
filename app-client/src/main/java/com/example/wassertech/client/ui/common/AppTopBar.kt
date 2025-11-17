package ru.wassertech.client.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import ru.wassertech.client.auth.AuthRepository
import ru.wassertech.navigation.AppRoutes
import ru.wassertech.feature.auth.AuthRoutes
import ru.wassertech.core.ui.theme.EditButtonStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    navController: NavController,
    isEditing: Boolean = false,
    onToggleEdit: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val backEntry by navController.currentBackStackEntryAsState()
    val route = backEntry?.destination?.route ?: AppRoutes.HOME
    val canNavigateBack = navController.previousBackStackEntry != null

    var menuOpen by remember { mutableStateOf(false) }

    // Получаем название страницы из route
    val pageTitle = remember(route) {
        when {
            route == AuthRoutes.LOGIN -> "Вход"
            route == AppRoutes.HOME -> "Объекты"
            route.startsWith("sites/") -> "Объекты"
            route.startsWith("site/") -> "Объект"
            route.startsWith("installation/") -> "Установка"
            route.startsWith("session_detail/") -> "Детали обслуживания"
            route.startsWith("maintenance_history/") -> "История обслуживания"
            route.startsWith("templates") -> "Шаблоны компонентов"
            route.startsWith("template_editor/") -> "Редактор шаблона"
            route == AppRoutes.CLIENT_ICON_PACKS || route.startsWith("client_icon_packs") -> "Икон паки"
            else -> "Wassertech Client"
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier
            .shadow(elevation = 2.dp)
            .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(0.dp)),
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
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate(AppRoutes.HOME_SETTINGS) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                            if (onLogout != null) {
                                DropdownMenuItem(
                                    text = { Text("Выйти") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Logout,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuOpen = false
                                        onLogout()
                                    }
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

