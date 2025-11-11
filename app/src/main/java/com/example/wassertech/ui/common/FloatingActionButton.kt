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
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

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
    modifier: Modifier = Modifier.padding(bottom = 16.dp) // Отступ снизу для позиционирования внизу экрана
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
            // Выпрыгивающие кнопки (сверху вниз) - позиционируются выше красного FABа
            template.options.reversed().forEachIndexed { index, option ->
                // Интервал между кнопками: 10dp
                val buttonSpacing = 10.dp
                val fabSize = 56.dp
                
                // Анимационные значения для каждой кнопки
                // Используем Float для offsetY и конвертируем в dp при использовании
                val offsetYFloat = remember { Animatable(0f) }
                val alpha = remember { Animatable(0f) }
                val scale = remember { Animatable(0.3f) }

                LaunchedEffect(expanded) {
                    if (expanded) {
                        // Анимация появления с задержкой для каждой кнопки
                        delay(index * 50L)
                        // Запускаем все анимации параллельно
                        coroutineScope {
                            val targetOffset = -(fabSize.value + buttonSpacing.value) * (index + 1)
                            launch {
                                offsetYFloat.animateTo(
                                    targetValue = targetOffset,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                )
                            }
                            launch {
                                alpha.animateTo(1f, tween(300))
                            }
                            launch {
                                scale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                            }
                        }
                    } else {
                        // Анимация исчезновения
                        coroutineScope {
                            launch {
                                alpha.animateTo(0f, tween(200))
                            }
                            launch {
                                scale.animateTo(0.3f, tween(200))
                            }
                            launch {
                                offsetYFloat.animateTo(0f, tween(200))
                            }
                        }
                    }
                }

                // Кнопка с подписью слева (черным текстом) и круглой кнопкой справа
                // Позиционируем абсолютно относительно основного FAB
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = offsetYFloat.value.dp)
                        .alpha(alpha.value)
                        .scale(scale.value)
                ) {
                    Row(
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
                        
                        // Круглая кнопка справа (выровнена по горизонтали с красным FABом)
                        // Используем Surface с минимальной тенью вместо FloatingActionButton
                        Surface(
                            onClick = {
                                expanded = false
                                option.onClick()
                            },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = template.optionsColor,
                            tonalElevation = 1.dp, // Очень минимальная тень
                            shadowElevation = 1.dp // Очень минимальная тень
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
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
                }
            }

            // Основная FAB (всегда "+") - позиционируется внизу
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = template.containerColor,
                contentColor = template.contentColor,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (expanded) "Закрыть" else "Открыть меню"
                )
            }
        }
    }
}

