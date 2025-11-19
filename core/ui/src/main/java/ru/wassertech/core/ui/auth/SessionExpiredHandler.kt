package ru.wassertech.core.ui.auth

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.wassertech.core.network.interceptor.SessionExpiredCallback

/**
 * Централизованный обработчик событий истечения сессии.
 * Используется для уведомления UI о необходимости показать диалог истечения сессии.
 */
object SessionExpiredHandler : SessionExpiredCallback {
    private const val TAG = "SessionExpiredHandler"
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(replay = 0)
    
    /**
     * Flow событий истечения сессии.
     * Подписывайтесь на этот Flow в UI для показа диалога.
     */
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()
    
    /**
     * Отправляет событие истечения сессии.
     * Вызывается из ErrorInterceptor при получении HTTP 401.
     * Реализует интерфейс SessionExpiredCallback для использования в ErrorInterceptor.
     */
    override fun onSessionExpired() {
        Log.d(TAG, "Сессия истекла, отправляем событие")
        scope.launch {
            _sessionExpiredEvent.emit(Unit)
        }
    }
    
    /**
     * @deprecated Используйте onSessionExpired()
     */
    @Deprecated("Используйте onSessionExpired()")
    fun notifySessionExpired() {
        onSessionExpired()
    }
}

