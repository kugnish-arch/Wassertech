package ru.wassertech.client.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Цвета для Bottom Navigation Bar
private val BottomBarBackground = Color.White.copy(alpha = 0.95f)
private val BottomBarActiveIcon = Color(0xFFE53935)
private val BottomBarInactiveIcon = Color(0xFF5A5A5A)
private val BottomBarActiveIndicator = Color(0xFFE53935)

@Composable
fun NavigationBottomBar(
    currentRoute: String?,
    onNavigateToHome: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(90.dp),
        containerColor = BottomBarBackground,
        tonalElevation = 3.dp
    ) {
        // Объекты (Главная)
        val isHomeSelected = currentRoute == "home" || currentRoute?.startsWith("site/") == true || 
                            currentRoute?.startsWith("sites/") == true || 
                            currentRoute?.startsWith("installation/") == true
        NavigationBarItem(
            icon = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (isHomeSelected) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(BottomBarActiveIndicator)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Объекты",
                            tint = if (isHomeSelected) BottomBarActiveIcon else BottomBarInactiveIcon
                        )
                    }
                }
            },
            label = {},
            selected = isHomeSelected,
            onClick = onNavigateToHome,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomBarActiveIcon,
                unselectedIconColor = BottomBarInactiveIcon,
                indicatorColor = Color.Transparent
            )
        )
        
        // Редактор шаблонов
        val isTemplatesSelected = currentRoute?.startsWith("templates") == true || 
                                  currentRoute?.startsWith("template_editor/") == true
        NavigationBarItem(
            icon = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (isTemplatesSelected) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(BottomBarActiveIndicator)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Редактор шаблонов",
                            tint = if (isTemplatesSelected) BottomBarActiveIcon else BottomBarInactiveIcon
                        )
                    }
                }
            },
            label = {},
            selected = isTemplatesSelected,
            onClick = onNavigateToTemplates,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomBarActiveIcon,
                unselectedIconColor = BottomBarInactiveIcon,
                indicatorColor = Color.Transparent
            )
        )
        
        // Настройки
        val isSettingsSelected = currentRoute == "home_settings"
        NavigationBarItem(
            icon = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (isSettingsSelected) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(BottomBarActiveIndicator)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Настройки",
                            tint = if (isSettingsSelected) BottomBarActiveIcon else BottomBarInactiveIcon
                        )
                    }
                }
            },
            label = {},
            selected = isSettingsSelected,
            onClick = onNavigateToSettings,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomBarActiveIcon,
                unselectedIconColor = BottomBarInactiveIcon,
                indicatorColor = Color.Transparent
            )
        )
    }
}

