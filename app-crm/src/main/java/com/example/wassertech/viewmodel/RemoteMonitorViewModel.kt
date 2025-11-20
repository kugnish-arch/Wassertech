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
        private const val POLLING_INTERVAL_MS = 10_000L // 10 секунд
        private const val DEFAULT_DEVICE_ID = "esp-test-01"
        private const val TIME_RANGE_HOURS = 10 // Запрашиваем данные за последние 10 часов
    }
    
    private val repository = RemoteMonitoringRepository(application)
    
    private val _state = MutableStateFlow(
        RemoteMonitorUiState(
            deviceId = DEFAULT_DEVICE_ID,
            isLoading = false,
            points = emptyList(),
            errorMessage = null
        )
    )
    val uiState: StateFlow<RemoteMonitorUiState> = _state.asStateFlow()
    
    private var monitoringJob: kotlinx.coroutines.Job? = null
    
    /**
     * Начинает мониторинг температуры для указанного устройства
     */
    fun startMonitoring(deviceId: String) {
        // Останавливаем предыдущий мониторинг, если он был
        stopMonitoring()
        
        _state.update { it.copy(deviceId = deviceId, errorMessage = null) }
        
        monitoringJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    _state.update { it.copy(isLoading = true, errorMessage = null) }
                    
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
                    
                    val points = repository.loadTemperaturePoints(deviceId, fromStr, toStr)
                    
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            points = points,
                            errorMessage = null
                        ) 
                    }
                    
                    Log.d(TAG, "Загружено ${points.size} точек температуры")
                    
                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Ошибка загрузки данных"
                    Log.e(TAG, "Ошибка при загрузке данных", e)
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = errorMessage
                        ) 
                    }
                }
                
                delay(POLLING_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Останавливает мониторинг
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}

