package ru.wassertech.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.wassertech.feature.reports.ReportsScreen
import ru.wassertech.core.ui.theme.NavigationBarStyle

/**
 * Главный экран с табами
 */
@Composable
fun HomeScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NavigationBarStyle.backgroundColor,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("Отчёты") },
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    colors = NavigationBarStyle.itemColors()
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Пусто") },
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    colors = NavigationBarStyle.itemColors()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> ReportsScreen()
                1 -> EmptyPlaceholderScreen()
            }
        }
    }
}

/**
 * Заглушка для пустой страницы
 */
@Composable
private fun EmptyPlaceholderScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Пустая страница",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Здесь будет контент",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

