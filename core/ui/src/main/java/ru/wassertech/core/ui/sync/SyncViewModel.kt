package ru.wassertech.core.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel для управления состоянием синхронизации.
 * Инкапсулирует SyncOrchestrator и предоставляет удобный API для UI.
 */
class SyncViewModel : ViewModel() {
    
    private val orchestrator = SyncOrchestrator()
    
    /**
     * Состояние синхронизации для UI.
     */
    val syncState: StateFlow<SyncUiState> = orchestrator.syncState
    
    /**
     * Запускает блокирующую синхронизацию (после логина).
     */
    fun startBlockingSync(syncFunction: SyncFunction) {
        viewModelScope.launch {
            orchestrator.startSync(syncFunction, isBlocking = true)
            
            // Запускаем проверку таймаута
            startTimeoutCheck()
        }
    }
    
    /**
     * Запускает неблокирующую синхронизацию (при запуске приложения).
     */
    fun startBackgroundSync(syncFunction: SyncFunction) {
        viewModelScope.launch {
            orchestrator.startSync(syncFunction, isBlocking = false)
        }
    }
    
    /**
     * Повторяет синхронизацию после ошибки.
     */
    fun retrySync(syncFunction: SyncFunction) {
        orchestrator.clearError()
        viewModelScope.launch {
            orchestrator.startSync(syncFunction, isBlocking = syncState.value.isBlocking)
            startTimeoutCheck()
        }
    }
    
    /**
     * Переходит в оффлайн режим (отменяет синхронизацию и скрывает ошибки).
     */
    fun goOffline() {
        orchestrator.cancelSync()
        orchestrator.clearError()
        orchestrator.setShowLongSyncDialog(false)
    }
    
    /**
     * Обрабатывает выбор "Подождать ещё" в диалоге долгой синхронизации.
     */
    fun waitMore() {
        orchestrator.setShowLongSyncDialog(false)
        // Продолжаем синхронизацию
    }
    
    /**
     * Закрывает диалог долгой синхронизации.
     */
    fun dismissLongSyncDialog() {
        orchestrator.setShowLongSyncDialog(false)
    }
    
    /**
     * Запускает проверку таймаута синхронизации.
     */
    private fun startTimeoutCheck() {
        viewModelScope.launch {
            while (syncState.value.isRunning) {
                delay(SyncOrchestrator.TIMEOUT_CHECK_INTERVAL_MS)
                orchestrator.checkTimeout()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        orchestrator.cancelSync()
    }
}

