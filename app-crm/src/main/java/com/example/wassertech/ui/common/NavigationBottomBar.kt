package com.example.wassertech.ui.common

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
private val BottomBarBackground = Color.White.copy(alpha = 0.95f) // Полупрозрачный белый
private val BottomBarActiveIcon = Color(0xFFE53935) // Красный для активной иконки
private val BottomBarInactiveIcon = Color(0xFF5A5A5A) // Серый для неактивных иконок
private val BottomBarActiveIndicator = Color(0xFFE53935) // Красная линия для активной вкладки

@Composable
fun NavigationBottomBar(
    currentRoute: String?,
    onNavigateToClients: () -> Unit,
    onNavigateToMaintenanceHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(90.dp),
        containerColor = BottomBarBackground,
        tonalElevation = 3.dp
    ) {
        // Клиенты
        val isClientsSelected = currentRoute?.startsWith("clients") == true
        NavigationBarItem(
            icon = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Красная линия сверху для активной вкладки
                    if (isClientsSelected) {
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
                            Icons.Filled.Person,
                            contentDescription = "Клиенты",
                            tint = if (isClientsSelected) BottomBarActiveIcon else BottomBarInactiveIcon
                        )
                    }
                }
            },
            label = {},
            selected = isClientsSelected,
            onClick = onNavigateToClients,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomBarActiveIcon,
                unselectedIconColor = BottomBarInactiveIcon,
                indicatorColor = Color.Transparent // Убираем стандартный индикатор
            )
        )
        
        // История ТО
        val isMaintenanceSelected = currentRoute?.startsWith("maintenance_history") == true
        NavigationBarItem(
            icon = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Красная линия сверху для активной вкладки
                    if (isMaintenanceSelected) {
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
                            Icons.Filled.Schedule,
                            contentDescription = "Обслуживание",
                            tint = if (isMaintenanceSelected) BottomBarActiveIcon else BottomBarInactiveIcon
                        )
                    }
                }
            },
            label = {},
            selected = isMaintenanceSelected,
            onClick = onNavigateToMaintenanceHistory,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomBarActiveIcon,
                unselectedIconColor = BottomBarInactiveIcon,
                indicatorColor = Color.Transparent
            )
        )
        
        // Отчёты
        val isReportsSelected = currentRoute?.startsWith("reports") == true
        NavigationBarItem(
            icon = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Красная линия сверху для активной вкладки
                    if (isReportsSelected) {
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
                            Icons.Filled.Description,
                            contentDescription = "Отчёты",
                            tint = if (isReportsSelected) BottomBarActiveIcon else BottomBarInactiveIcon
                        )
                    }
                }
            },
            label = {},
            selected = isReportsSelected,
            onClick = onNavigateToReports,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BottomBarActiveIcon,
                unselectedIconColor = BottomBarInactiveIcon,
                indicatorColor = Color.Transparent
            )
        )
    }
}

