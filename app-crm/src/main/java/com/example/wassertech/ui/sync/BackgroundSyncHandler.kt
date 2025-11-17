package ru.wassertech.ui.sync

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.core.ui.sync.*
import ru.wassertech.sync.SyncHelper

/**
 * Обработчик фоновой синхронизации при запуске приложения.
 * Показывает неблокирующий индикатор синхронизации.
 */
@Composable
fun BackgroundSyncHandler(
    context: Context,
    modifier: Modifier = Modifier
) {
    val viewModel: SyncViewModel = viewModel()
    val syncState by viewModel.syncState.collectAsState()
    
    // Запускаем фоновую синхронизацию при первом появлении
    LaunchedEffect(Unit) {
        if (!syncState.isRunning) {
            val syncFunction = SyncHelper.createSyncFunction(context)
            viewModel.startBackgroundSync(syncFunction)
        }
    }
    
    // Показываем неблокирующий индикатор
    SyncIndicator(state = syncState, modifier = modifier)
}

