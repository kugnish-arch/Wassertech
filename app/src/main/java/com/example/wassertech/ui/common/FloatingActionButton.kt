package com.example.wassertech.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Конфигурация для Floating Action Button
 */
data class FABTemplate(
    val icon: ImageVector = Icons.Filled.Add,
    val containerColor: Color = Color(0xFFD32F2F), // Красный по умолчанию
    val contentColor: Color = Color.White,
    val onClick: () -> Unit,
    val options: List<FABOption> = emptyList(), // Опции для меню (если нужно несколько действий)
    val optionsColor: Color = Color(0xFF1E1E1E) // Цвет выпрыгивающих кнопок (черный по умолчанию)
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
        // FAB с выпрыгивающими кнопками
        Box(modifier = modifier) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Выпрыгивающие кнопки (сверху вниз)
                template.options.reversed().forEachIndexed { index, option ->
                    // Увеличенный интервал между кнопками: 80dp для лучшего визуального разделения
                    val buttonHeightWithSpacing = 80f
                    val offsetY = remember(expanded) {
                        Animatable(if (expanded) 0f else buttonHeightWithSpacing * (index + 1))
                    }
                    val alpha = remember(expanded) {
                        Animatable(if (expanded) 1f else 0f)
                    }
                    val scale = remember(expanded) {
                        Animatable(if (expanded) 1f else 0f)
                    }

                    LaunchedEffect(expanded) {
                        if (expanded) {
                            // Анимация появления с задержкой для каждой кнопки
                            delay(index * 50L)
                            offsetY.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                            alpha.animateTo(1f, tween(300))
                            scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                        } else {
                            // Анимация исчезновения
                            alpha.animateTo(0f, tween(200))
                            scale.animateTo(0f, tween(200))
                            offsetY.animateTo(buttonHeightWithSpacing * (index + 1), tween(200))
                        }
                    }

                    // Кнопка с подписью слева (черным текстом) и круглой кнопкой справа
                    Row(
                        modifier = Modifier
                            .offset(y = (-offsetY.value).dp)
                            .alpha(alpha.value)
                            .scale(scale.value)
                            .padding(end = 8.dp), // Небольшой отступ от края
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Подпись слева (черным цветом)
                        Text(
                            text = option.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        
                        // Круглая кнопка справа
                        FloatingActionButton(
                            onClick = {
                                expanded = false
                                option.onClick()
                            },
                            containerColor = template.optionsColor,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Основная FAB (всегда "+")
                FloatingActionButton(
                    onClick = { expanded = !expanded },
                    containerColor = template.containerColor,
                    contentColor = template.contentColor,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = if (expanded) "Закрыть" else "Открыть меню"
                    )
                }
            }
        }
    }
}

