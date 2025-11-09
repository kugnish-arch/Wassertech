package com.example.wassertech.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Конфигурация для Floating Action Button
 */
data class FABTemplate(
    val icon: ImageVector = Icons.Filled.Add,
    val containerColor: Color = Color(0xFFD32F2F), // Красный по умолчанию
    val contentColor: Color = Color.White,
    val onClick: () -> Unit,
    val options: List<FABOption> = emptyList() // Опции для меню (если нужно несколько действий)
)

/**
 * Опция в меню FAB
 */
data class FABOption(
    val label: String,
    val icon: ImageVector = Icons.Filled.Add,
    val onClick: () -> Unit
)

/**
 * Универсальный Floating Action Button
 * Если есть опции (options), при клике показывается меню с выбором
 * Если опций нет, выполняется onClick напрямую
 */
@Composable
fun AppFloatingActionButton(
    template: FABTemplate,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    if (template.options.isEmpty()) {
        // Простой FAB без меню
        FloatingActionButton(
            onClick = template.onClick,
            containerColor = template.containerColor,
            contentColor = template.contentColor,
            shape = CircleShape,
            modifier = modifier
        ) {
            Icon(
                imageVector = template.icon,
                contentDescription = null
            )
        }
    } else {
        // FAB с меню опций
        Box(modifier = modifier) {
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = template.containerColor,
                contentColor = template.contentColor,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = template.icon,
                    contentDescription = null
                )
            }
            
            // Меню с опциями
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 180.dp)
            ) {
                template.options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(option.label)
                            }
                        },
                        onClick = {
                            expanded = false
                            option.onClick()
                        }
                    )
                }
            }
        }
    }
}

