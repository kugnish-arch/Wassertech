package ru.wassertech.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.wassertech.repository.RemoteMonitoringRepository
import ru.wassertech.core.screens.remote.ui.RemoteMonitorUiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel для экрана удалённого мониторинга температуры
 */
class RemoteMonitorViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "RemoteMonitorViewModel"
        private const val POLLING_INTERVAL_DEVICE1_MS = 10_000L // 10 секунд для esp-test-01
        private const val POLLING_INTERVAL_DEVICE2_MS = 60_000L // 1 минута для esp-test-02
        private const val DEVICE_ID_1 = "esp-test-01"
        private const val DEVICE_ID_2 = "esp-test-02"
        private const val TIME_RANGE_HOURS = 10 // Запрашиваем данные за последние 10 часов
    }

    private val repository = RemoteMonitoringRepository(application)

    private val _state = MutableStateFlow(
        RemoteMonitorUiState(
            deviceId1 = DEVICE_ID_1,
            deviceId2 = DEVICE_ID_2,
            isLoading = false,
            points1 = emptyList(),
            points2 = emptyList(),
            errorMessage = null
        )
    )
    val uiState: StateFlow<RemoteMonitorUiState> = _state.asStateFlow()

    private var monitoringJob1: kotlinx.coroutines.Job? = null
    private var monitoringJob2: kotlinx.coroutines.Job? = null

    /**
     * Начинает мониторинг температуры для обоих устройств
     */
    fun startMonitoring() {
        android.util.Log.e(TAG, "=== startMonitoring() ВЫЗВАН - запускаем мониторинг обоих устройств ===")
        stopMonitoring()

        // Запускаем мониторинг первого устройства
        monitoringJob1 = viewModelScope.launch(Dispatchers.IO) {
            android.util.Log.e(TAG, "=== Job1 ЗАПУЩЕН для $DEVICE_ID_1 ===")
            while (isActive) {
                try {
                    _state.update { it.copy(isLoading = true, errorMessage = null) }

                    val points = loadTemperaturePoints(DEVICE_ID_1)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            points1 = points,
                            errorMessage = null
                        )
                    }

                    Log.d(TAG, "Загружено ${points.size} точек температуры для $DEVICE_ID_1")

                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Ошибка загрузки данных для $DEVICE_ID_1"
                    Log.e(TAG, "Ошибка при загрузке данных для $DEVICE_ID_1", e)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMessage
                        )
                    }
                }

                delay(POLLING_INTERVAL_DEVICE1_MS)
            }
        }

        // Запускаем мониторинг второго устройства
        monitoringJob2 = viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.e(TAG, "=== Job2 ЗАПУЩЕН для $DEVICE_ID_2 ===")
                while (isActive) {
                    try {
                        val points = loadTemperaturePoints(DEVICE_ID_2)

                        _state.update {
                            it.copy(
                                points2 = points
                            )
                        }

                        Log.d(TAG, "Загружено ${points.size} точек температуры для $DEVICE_ID_2")

                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при загрузке данных для $DEVICE_ID_2", e)
                        // Не обновляем errorMessage для второго устройства, чтобы не перекрывать ошибки первого
                    }

                    delay(POLLING_INTERVAL_DEVICE2_MS)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "=== Job2 УПАЛ С ОШИБКОЙ: ${e.message} ===", e)
            }
        }
    }

    /**
     * Загружает точки температуры для указанного устройства
     */
    private suspend fun loadTemperaturePoints(deviceId: String): List<ru.wassertech.core.screens.remote.TemperaturePoint> {
        // Вычисляем временной диапазон (последние 10 часов)
        val now = Calendar.getInstance()
        val from = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -TIME_RANGE_HOURS)
        }

        // Формат даты: YYYY-MM-DD+HH:MM:SS (плюс вместо пробела для URL)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd+HH:mm:ss", Locale.getDefault())
        val fromStr = dateFormat.format(from.time)
        val toStr = dateFormat.format(now.time)

        Log.d(TAG, "Загрузка данных: deviceId=$deviceId, from=$fromStr, to=$toStr")

        return repository.loadTemperaturePoints(deviceId, fromStr, toStr)
    }

    /**
     * Начинает мониторинг температуры для указанного устройства (для обратной совместимости)
     */
    fun startMonitoring(deviceId: String) {
        startMonitoring()
    }

    /**
     * Останавливает мониторинг
     */
    fun stopMonitoring() {
        monitoringJob1?.cancel()
        monitoringJob1 = null
        monitoringJob2?.cancel()
        monitoringJob2 = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}

