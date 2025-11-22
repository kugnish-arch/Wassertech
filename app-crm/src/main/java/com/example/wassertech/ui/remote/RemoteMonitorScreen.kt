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
    deviceId: String? = null, // Если null, используется режим с двумя устройствами
    onBackClick: () -> Unit,
    viewModel: RemoteMonitorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Запускаем мониторинг при заходе на экран
    LaunchedEffect(deviceId) {
        if (deviceId != null) {
            // Режим с одним устройством
            viewModel.startMonitoring(deviceId, null)
        } else {
            // Режим с двумя устройствами (по умолчанию)
            viewModel.startMonitoring()
        }
    }
    
    RemoteMonitorSharedScreen(
        uiState = uiState,
        onBackClick = onBackClick
    )
}

