package ru.wassertech.crm.ui.common

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
import ru.wassertech.core.ui.theme.NavigationBarStyle

@Composable
fun NavigationBottomBar(
    currentRoute: String?,
    onNavigateToClients: () -> Unit,
    onNavigateToMaintenanceHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(NavigationBarStyle.height),
        containerColor = NavigationBarStyle.backgroundColor,
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
                                .width(NavigationBarStyle.indicatorWidth)
                                .height(NavigationBarStyle.indicatorHeight)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(NavigationBarStyle.activeIndicatorColor)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(NavigationBarStyle.indicatorHeight))
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
                            tint = if (isClientsSelected) NavigationBarStyle.activeIconColor else NavigationBarStyle.inactiveIconColor
                        )
                    }
                }
            },
            label = {},
            selected = isClientsSelected,
            onClick = onNavigateToClients,
            colors = NavigationBarStyle.itemColors()
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
                                .width(NavigationBarStyle.indicatorWidth)
                                .height(NavigationBarStyle.indicatorHeight)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(NavigationBarStyle.activeIndicatorColor)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(NavigationBarStyle.indicatorHeight))
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
                            tint = if (isMaintenanceSelected) NavigationBarStyle.activeIconColor else NavigationBarStyle.inactiveIconColor
                        )
                    }
                }
            },
            label = {},
            selected = isMaintenanceSelected,
            onClick = onNavigateToMaintenanceHistory,
            colors = NavigationBarStyle.itemColors()
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
                                .width(NavigationBarStyle.indicatorWidth)
                                .height(NavigationBarStyle.indicatorHeight)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(NavigationBarStyle.activeIndicatorColor)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(NavigationBarStyle.indicatorHeight))
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
                            tint = if (isReportsSelected) NavigationBarStyle.activeIconColor else NavigationBarStyle.inactiveIconColor
                        )
                    }
                }
            },
            label = {},
            selected = isReportsSelected,
            onClick = onNavigateToReports,
            colors = NavigationBarStyle.itemColors()
        )
    }
}

