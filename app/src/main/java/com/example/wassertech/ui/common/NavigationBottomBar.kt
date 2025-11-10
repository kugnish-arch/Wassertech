package com.example.wassertech.ui.common

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NavigationBottomBar(
    currentRoute: String?,
    onNavigateToClients: () -> Unit,
    onNavigateToMaintenanceHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.height(93.dp) // Фиксированная высота 93dp (увеличено на 5px с 88dp)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Клиенты") },
            label = {},
            selected = currentRoute?.startsWith("clients") == true,
            onClick = onNavigateToClients
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Schedule, contentDescription = "Обслуживание") },
            label = {},
            selected = currentRoute?.startsWith("maintenance_history") == true,
            onClick = onNavigateToMaintenanceHistory
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Description, contentDescription = "Отчёты") },
            label = {},
            selected = currentRoute?.startsWith("reports") == true,
            onClick = onNavigateToReports
        )
    }
}

