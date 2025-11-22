package ru.wassertech.core.ui.sync

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Overlay для блокирующей синхронизации (после логина).
 * Показывает полупрозрачный фон с карточкой прогресса посередине.
 */
@Composable
fun SyncOverlay(
    state: SyncUiState,
    modifier: Modifier = Modifier
) {
    if (!state.shouldShowOverlay()) {
        return
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Круговой индикатор прогресса
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Заголовок
                Text(
                    text = "Синхронизация с сервером",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                
                // Подзаголовок
                Text(
                    text = "Пожалуйста, подождите…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                // Линейный прогресс-бар (если есть прогресс)
                state.progress?.let { progress ->
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } ?: LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                // Текущий шаг синхронизации
                state.currentStep?.let { step ->
                    Text(
                        text = step.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Неблокирующий индикатор синхронизации (для фоновой синхронизации при запуске).
 * Показывает небольшой индикатор вверху экрана.
 */
@Composable
fun SyncIndicator(
    state: SyncUiState,
    modifier: Modifier = Modifier
) {
    if (!state.shouldShowIndicator()) {
        return
    }
    
    AnimatedVisibility(
        visible = state.shouldShowIndicator(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Синхронизация данных",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    state.currentStep?.let { step ->
                        Text(
                            text = step.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Диалог ошибки синхронизации.
 */
@Composable
fun SyncErrorDialog(
    error: SyncError,
    onRetry: () -> Unit,
    onGoOffline: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Ошибка синхронизации")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(error.getUserMessage())
                if (error.httpCode != null) {
                    Text(
                        text = "Код ошибки: ${error.httpCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Повторить")
            }
        },
        dismissButton = {
            TextButton(onClick = onGoOffline) {
                Text("Оффлайн режим")
            }
        }
    )
}

/**
 * Диалог долгой синхронизации (таймаут).
 */
@Composable
fun LongSyncDialog(
    onWaitMore: () -> Unit,
    onGoOffline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onWaitMore, // По умолчанию ждём ещё
        title = {
            Text("Синхронизация занимает больше обычного")
        },
        text = {
            Text(
                "Синхронизация данных с сервером занимает больше времени, чем обычно. " +
                "Это может быть связано с медленным интернет-соединением или большой загрузкой сервера."
            )
        },
        confirmButton = {
            TextButton(onClick = onWaitMore) {
                Text("Подождать ещё")
            }
        },
        dismissButton = {
            TextButton(onClick = onGoOffline) {
                Text("Оффлайн режим")
            }
        }
    )
}




