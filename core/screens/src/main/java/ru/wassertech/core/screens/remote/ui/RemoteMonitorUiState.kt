package ru.wassertech.core.screens.remote.ui

import ru.wassertech.core.screens.remote.TemperaturePoint

/**
 * UI State для экрана удалённого мониторинга
 */
data class RemoteMonitorUiState(
    val deviceId: String,
    val isLoading: Boolean = false,
    val points: List<TemperaturePoint> = emptyList(),
    val errorMessage: String? = null
)

