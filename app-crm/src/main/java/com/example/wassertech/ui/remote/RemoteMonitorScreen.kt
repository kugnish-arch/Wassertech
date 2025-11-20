package ru.wassertech.ui.remote

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.wassertech.core.screens.remote.RemoteMonitorSharedScreen
import ru.wassertech.viewmodel.RemoteMonitorViewModel

/**
 * Thin-wrapper экран для удалённого мониторинга температуры
 */
@Composable
fun RemoteMonitorScreen(
    deviceId: String = "esp-test-01",
    onBackClick: () -> Unit,
    viewModel: RemoteMonitorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Запускаем мониторинг при заходе на экран
    LaunchedEffect(deviceId) {
        viewModel.startMonitoring(deviceId)
    }
    
    RemoteMonitorSharedScreen(
        uiState = uiState,
        onBackClick = onBackClick
    )
}

