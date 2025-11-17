package ru.wassertech.client.ui.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.wassertech.core.ui.sync.*
import ru.wassertech.client.sync.SyncHelper

/**
 * Экран синхронизации после логина.
 * Показывает блокирующий overlay с прогрессом синхронизации.
 * После успешной синхронизации вызывает onSyncComplete.
 */
@Composable
fun PostLoginSyncScreen(
    onSyncComplete: () -> Unit,
    onGoOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SyncViewModel = viewModel()
    val syncState by viewModel.syncState.collectAsState()
    
    // Запускаем синхронизацию при первом появлении экрана
    LaunchedEffect(Unit) {
        val syncFunction = SyncHelper.createSyncFunction(context)
        viewModel.startBlockingSync(syncFunction)
    }
    
    // Переходим на основной экран после успешной синхронизации
    LaunchedEffect(syncState.isRunning, syncState.error) {
        if (!syncState.isRunning && syncState.error == null && syncState.currentStep == SyncStep.COMPLETED) {
            onSyncComplete()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Показываем overlay синхронизации
        SyncOverlay(state = syncState)
        
        // Диалог ошибки синхронизации
        syncState.error?.let { error ->
            SyncErrorDialog(
                error = error,
                onRetry = {
                    val syncFunction = SyncHelper.createSyncFunction(context)
                    viewModel.retrySync(syncFunction)
                },
                onGoOffline = {
                    viewModel.goOffline()
                    onGoOffline()
                },
                onDismiss = {
                    viewModel.goOffline()
                    onGoOffline()
                }
            )
        }
        
        // Диалог долгой синхронизации
        if (syncState.showLongSyncDialog) {
            LongSyncDialog(
                onWaitMore = {
                    viewModel.waitMore()
                },
                onGoOffline = {
                    viewModel.goOffline()
                    onGoOffline()
                }
            )
        }
    }
}

