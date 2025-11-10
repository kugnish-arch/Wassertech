package com.example.wassertech.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wassertech.ui.theme.SaveIconColor

/**
 * Стили для диалогов добавления/редактирования элементов
 * Используются во всех диалогах добавления (Клиент, Группа, Объект, Установка, Компонент и т.д.)
 */
object DialogStyles {
    val shape = RoundedCornerShape(12.dp) // Радиус скругления 12dp (было 16dp) - более инженерный стиль
    val elevation = 6.dp // Elevation 6 - легкая тень для визуального отделения от фона
}

/**
 * Общий компонент для диалогов добавления/редактирования элементов
 * 
 * @param title Заголовок диалога
 * @param text Содержимое диалога (композ-функция)
 * @param onDismissRequest Обработчик закрытия диалога
 * @param confirmText Текст кнопки подтверждения (по умолчанию "Добавить")
 * @param dismissText Текст кнопки отмены (по умолчанию "Отмена")
 * @param onConfirm Обработчик подтверждения
 * @param onDismiss Обработчик отмены (по умолчанию просто закрывает диалог)
 * @param confirmEnabled Включена ли кнопка подтверждения (по умолчанию true)
 */
@Composable
fun CommonAddDialog(
    title: String,
    text: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    confirmText: String = "Добавить",
    dismissText: String = "Отмена",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = onDismissRequest,
    confirmEnabled: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = DialogStyles.shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = DialogStyles.elevation)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                
                // Содержимое
                text()
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка "Отмена" с красным крестом в кружке
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD32F2F), // Красный цвет
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = dismissText,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.width(8.dp))
                    
                    // Кнопка "Добавить" с зеленым плюсом в кружке
                    IconButton(
                        onClick = onConfirm,
                        enabled = confirmEnabled,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (confirmEnabled) SaveIconColor else MaterialTheme.colorScheme.surfaceVariant, // Зеленый цвет или серый если отключено
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = confirmText,
                                    tint = if (confirmEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

