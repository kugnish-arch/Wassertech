package com.example.wassertech.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wassertech.core.ui.theme.DialogStyle
import com.example.wassertech.core.ui.theme.SaveIconColor

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
            shape = DialogStyle.shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = DialogStyle.elevation)
        ) {
            Column(
                modifier = Modifier.padding(DialogStyle.padding),
                verticalArrangement = Arrangement.spacedBy(DialogStyle.contentSpacing)
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

/**
 * Диалог с иконкой и текстом
 * 
 * @param title Заголовок диалога
 * @param message Текст сообщения
 * @param iconResId ID ресурса иконки (drawable)
 * @param onDismissRequest Обработчик закрытия диалога
 * @param confirmText Текст кнопки подтверждения
 * @param onConfirm Обработчик подтверждения
 * @param showDismissButton Показывать ли кнопку отмены (по умолчанию false)
 */
@Composable
fun IconDialog(
    title: String,
    message: String,
    iconResId: Int,
    onDismissRequest: () -> Unit,
    confirmText: String = "ОК",
    onConfirm: () -> Unit,
    showDismissButton: Boolean = false
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
            shape = DialogStyle.shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = DialogStyle.elevation)
        ) {
            Column(
                modifier = Modifier.padding(DialogStyle.padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DialogStyle.contentSpacing)
            ) {
                // Иконка
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                
                // Заголовок
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                // Сообщение
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (showDismissButton) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showDismissButton) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Отмена")
                        }
                    }
                    
                    Button(
                        onClick = onConfirm
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

