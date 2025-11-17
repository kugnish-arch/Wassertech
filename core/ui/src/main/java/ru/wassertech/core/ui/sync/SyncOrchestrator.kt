package ru.wassertech.core.ui.sync

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import ru.wassertech.core.network.interceptor.NetworkException

/**
 * Функция для выполнения синхронизации.
 * Принимает callback для обновления прогресса.
 */
typealias SyncFunction = suspend (onProgress: ((SyncStep) -> Unit)?) -> SyncResult

/**
 * Оркестратор синхронизации с поддержкой прогресса и обработки ошибок.
 * Инкапсулирует работу с SyncEngine и предоставляет Flow<SyncUiState> для UI.
 * 
 * Использование:
 * ```
 * val orchestrator = SyncOrchestrator()
 * orchestrator.syncState.collect { state ->
 *     // Обновляем UI на основе state
 * }
 * orchestrator.startSync(isBlocking = true) { onProgress ->
 *     val syncEngine = SyncEngine(context)
 *     syncEngine.syncFull() // или syncPush() + syncPull()
 * }
 * ```
 */
class SyncOrchestrator {
    companion object {
        private const val TAG = "SyncOrchestrator"
        
        /**
         * Таймаут синхронизации в миллисекундах (25 секунд).
         * Если синхронизация длится дольше, показывается диалог.
         */
        const val SYNC_TIMEOUT_MS = 25_000L
        
        /**
         * Интервал проверки таймаута в миллисекундах (500 мс).
         */
        const val TIMEOUT_CHECK_INTERVAL_MS = 500L
        
        /**
         * Создаёт SyncFunction из SyncEngine с поддержкой прогресса.
         * Выполняет syncPush и syncPull отдельно, обновляя прогресс между шагами.
         */
        fun <T> createSyncFunction(
            syncEngine: T,
            syncPushMethod: suspend T.() -> SyncResult,
            syncPullMethod: suspend T.() -> SyncResult
        ): SyncFunction = suspend@{ onProgress ->
            // Push фаза
            onProgress?.invoke(SyncStep.PUSH_CLIENTS)
            val pushResult = syncPushMethod(syncEngine)
            
            if (!pushResult.success) {
                return@suspend SyncResult(
                    success = false,
                    message = pushResult.message
                )
            }
            
            // Pull фаза
            onProgress?.invoke(SyncStep.PULL_CLIENTS)
            val pullResult = syncPullMethod(syncEngine)
            
            SyncResult(
                success = pushResult.success && pullResult.success,
                message = "Push: ${pushResult.message}; Pull: ${pullResult.message}"
            )
        }
    }
    
    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()
    
    /**
     * Запускает синхронизацию.
     * 
     * @param syncFunction функция для выполнения синхронизации (обычно syncEngine.syncFull())
     * @param isBlocking true для блокирующей синхронизации (после логина), false для фоновой (при запуске)
     */
    suspend fun startSync(
        syncFunction: SyncFunction,
        isBlocking: Boolean = true
    ) {
        if (_syncState.value.isRunning) {
            Log.w(TAG, "Синхронизация уже запущена, пропускаем")
            return
        }
        
        _syncState.value = SyncUiState(
            isRunning = true,
            isBlocking = isBlocking,
            startTimeMs = System.currentTimeMillis()
        )
        
        try {
            // Выполняем синхронизацию с отслеживанием прогресса
            val result = syncFunction { step ->
                updateStep(step, null)
            }
            
            if (result.success) {
                _syncState.value = SyncUiState(
                    isRunning = false,
                    currentStep = SyncStep.COMPLETED,
                    progress = 1.0f
                )
            } else {
                // Ошибка уже обработана в syncFunction или обработаем здесь
                handleSyncError(result.message, null)
            }
        } catch (e: NetworkException) {
            handleNetworkException(e)
        } catch (e: Exception) {
            handleException(e)
        }
    }
    
    
    /**
     * Обновляет текущий шаг синхронизации.
     */
    private fun updateStep(step: SyncStep, onProgress: ((SyncStep) -> Unit)?) {
        _syncState.value = _syncState.value.copy(
            currentStep = step,
            progress = calculateProgress(step)
        )
        onProgress?.invoke(step)
    }
    
    /**
     * Вычисляет прогресс на основе текущего шага.
     * Упрощённая версия: считаем, что push и pull примерно равны по времени.
     */
    private fun calculateProgress(step: SyncStep): Float? {
        return when (step) {
            SyncStep.PUSH_CLIENTS -> 0.1f
            SyncStep.PUSH_SITES -> 0.15f
            SyncStep.PUSH_INSTALLATIONS -> 0.2f
            SyncStep.PUSH_COMPONENTS -> 0.25f
            SyncStep.PUSH_SESSIONS -> 0.3f
            SyncStep.PUSH_VALUES -> 0.35f
            SyncStep.PUSH_TEMPLATES -> 0.4f
            SyncStep.PUSH_ICON_PACKS -> 0.45f
            
            SyncStep.PULL_CLIENTS -> 0.5f
            SyncStep.PULL_SITES -> 0.55f
            SyncStep.PULL_INSTALLATIONS -> 0.6f
            SyncStep.PULL_COMPONENTS -> 0.65f
            SyncStep.PULL_SESSIONS -> 0.7f
            SyncStep.PULL_VALUES -> 0.75f
            SyncStep.PULL_TEMPLATES -> 0.8f
            SyncStep.PULL_ICON_PACKS -> 0.85f
            SyncStep.PULL_ICONS -> 0.9f
            SyncStep.PULL_DELETED -> 0.95f
            
            SyncStep.COMPLETED -> 1.0f
        }
    }
    
    /**
     * Обрабатывает ошибку синхронизации из сообщения.
     */
    private fun handleSyncError(message: String, httpCode: Int?) {
        val errorType = when {
            message.contains("401", ignoreCase = true) || message.contains("токен", ignoreCase = true) -> SyncErrorType.Auth
            message.contains("403", ignoreCase = true) || message.contains("доступ запрещен", ignoreCase = true) -> SyncErrorType.Auth
            message.contains("сеть", ignoreCase = true) || message.contains("network", ignoreCase = true) -> SyncErrorType.Network
            httpCode != null && httpCode >= 500 -> SyncErrorType.Server
            else -> SyncErrorType.Unknown
        }
        
        _syncState.value = _syncState.value.copy(
            isRunning = false,
            error = SyncError(
                type = errorType,
                message = message,
                httpCode = httpCode
            )
        )
    }
    
    /**
     * Обрабатывает сетевую ошибку.
     */
    private fun handleNetworkException(e: NetworkException) {
        _syncState.value = _syncState.value.copy(
            isRunning = false,
            error = SyncError(
                type = SyncErrorType.Network,
                message = e.message ?: "Ошибка сети",
                exception = e
            )
        )
    }
    
    /**
     * Обрабатывает общее исключение.
     */
    private fun handleException(e: Exception) {
        _syncState.value = _syncState.value.copy(
            isRunning = false,
            error = SyncError(
                type = SyncErrorType.Unknown,
                message = e.message ?: "Неизвестная ошибка",
                exception = e
            )
        )
    }
    
    /**
     * Отменяет синхронизацию.
     */
    fun cancelSync() {
        if (_syncState.value.isRunning) {
            _syncState.value = _syncState.value.copy(isRunning = false)
        }
    }
    
    /**
     * Сбрасывает состояние ошибки.
     */
    fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }
    
    /**
     * Устанавливает флаг показа диалога долгой синхронизации.
     */
    fun setShowLongSyncDialog(show: Boolean) {
        _syncState.value = _syncState.value.copy(showLongSyncDialog = show)
    }
    
    /**
     * Проверяет, превышен ли таймаут синхронизации.
     */
    fun checkTimeout(): Boolean {
        val state = _syncState.value
        val startTime = state.startTimeMs ?: return false
        
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > SYNC_TIMEOUT_MS && state.isRunning && !state.showLongSyncDialog) {
            setShowLongSyncDialog(true)
            return true
        }
        return false
    }
}

/**
 * Упрощённая модель результата синхронизации для оркестратора.
 * Используется для совместимости с разными версиями SyncEngine.
 */
data class SyncResult(
    val success: Boolean,
    val message: String
)

