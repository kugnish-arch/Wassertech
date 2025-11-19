package ru.wassertech.core.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Диалог истечения сессии.
 * Показывается при получении HTTP 401 от сервера.
 * 
 * @param visible Видимость диалога
 * @param onDismissRequest Коллбек при закрытии диалога (для кнопки "Остаться офлайн")
 * @param onNavigateToLogin Коллбек для перехода к экрану авторизации
 */
@Composable
fun SessionExpiredDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text("Сессия истекла")
            },
            text = {
                Text(
                    "Соединение с сервером требует повторного входа. " +
                    "Вы можете перейти к авторизации или продолжить работу в офлайн режиме."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onNavigateToLogin,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Перейти к авторизации")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text("Остаться офлайн")
                }
            },
            modifier = modifier
        )
    }
}

