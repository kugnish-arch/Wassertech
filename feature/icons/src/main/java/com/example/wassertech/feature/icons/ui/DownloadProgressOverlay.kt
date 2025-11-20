package ru.wassertech.feature.icons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Данные для прогресса загрузки.
 */
data class DownloadProgressData(
    val currentPack: Int,
    val totalPacks: Int,
    val currentIcon: Int,
    val totalIcons: Int
)

/**
 * Оверлей с прогрессом загрузки икон-паков.
 */
@Composable
fun DownloadProgressOverlay(
    progress: DownloadProgressData,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Загрузка икон-паков",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Text(
                    text = "Пак ${progress.currentPack} из ${progress.totalPacks}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LinearProgressIndicator(
                    progress = { 
                        if (progress.totalIcons > 0) {
                            progress.currentIcon.toFloat() / progress.totalIcons
                        } else {
                            0f
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Иконок: ${progress.currentIcon}/${progress.totalIcons}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                TextButton(onClick = onCancel) {
                    Text("Отмена")
                }
            }
        }
    }
}



