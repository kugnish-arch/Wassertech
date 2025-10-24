package com.example.wassertech.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.wassertech.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateTo: (AppSection) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_wassertech),
                        contentDescription = "Wassertech",
                        modifier = Modifier
                            .height(40.dp)      // <-- увеличь, если хочешь крупнее
                            .aspectRatio(3f)    // <-- примерно 3:1, чтобы логотип был вытянут
                    )

                }
                },
                actions = {
                    // Тема
                    IconButton(onClick = onToggleTheme) {
                        val icon = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode
                        Icon(icon, contentDescription = "Переключить тему")
                    }
                    // Меню
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Клиенты") },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateTo(AppSection.Clients)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Пустой экран") },
                                onClick = { menuExpanded = false } // уже здесь
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Пустой экран ✨", style = MaterialTheme.typography.titleMedium)
        }
    }
}
