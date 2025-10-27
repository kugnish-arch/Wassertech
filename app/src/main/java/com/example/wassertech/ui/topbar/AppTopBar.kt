package com.example.wassertech.ui.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String = "Wassertech CRM",
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    onOpenClients: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenMaintenanceHistory: ((String) -> Unit)? = null,
    currentInstallationId: String? = null
) {
    var menuOpen by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (showBack && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        },
        actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Клиенты") },
                    onClick = {
                        menuOpen = false
                        onOpenClients()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Шаблоны") },
                    onClick = {
                        menuOpen = false
                        onOpenTemplates()
                    }
                )
                if (onOpenMaintenanceHistory != null && currentInstallationId != null) {
                    DropdownMenuItem(
                        text = { Text("История ТО") },
                        onClick = {
                            menuOpen = false
                            onOpenMaintenanceHistory(currentInstallationId)
                        }
                    )
                }
            }
        }
    )
}