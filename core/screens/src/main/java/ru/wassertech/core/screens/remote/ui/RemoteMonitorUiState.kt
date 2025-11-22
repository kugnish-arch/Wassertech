package ru.wassertech.core.screens.remote.ui

import ru.wassertech.core.screens.remote.TemperaturePoint

/**
 * UI State для экрана удалённого мониторинга
 */
data class RemoteMonitorUiState(
    val deviceId1: String = "esp-test-01",
    val deviceId2: String = "esp-test-02",
    val isLoading: Boolean = false,
    val points1: List<TemperaturePoint> = emptyList(),
    val points2: List<TemperaturePoint> = emptyList(),
    val errorMessage: String? = null
) {
    // Для обратной совместимости
    val deviceId: String get() = deviceId1
    val points: List<TemperaturePoint> get() = points1
}

