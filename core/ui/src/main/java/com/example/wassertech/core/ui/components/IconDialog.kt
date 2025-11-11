package com.example.wassertech.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.wassertech.core.ui.R
import com.example.wassertech.core.ui.theme.DialogStyle

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

