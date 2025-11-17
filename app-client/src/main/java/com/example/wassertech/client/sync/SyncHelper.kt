package ru.wassertech.client.sync

import android.content.Context
import ru.wassertech.core.ui.sync.SyncFunction
import ru.wassertech.core.ui.sync.SyncStep
import ru.wassertech.core.ui.sync.SyncResult as UiSyncResult

/**
 * Вспомогательные функции для интеграции SyncEngine с системой автоматической синхронизации.
 */
object SyncHelper {
    
    /**
     * Создаёт SyncFunction из SyncEngine для использования в SyncOrchestrator.
     * Выполняет syncPush и syncPull отдельно, обновляя прогресс между шагами.
     */
    fun createSyncFunction(context: Context): SyncFunction = suspend@{ onProgress ->
        val syncEngine = SyncEngine(context)
        
        // Push фаза
        onProgress?.invoke(SyncStep.PUSH_CLIENTS)
        val pushResult = syncEngine.syncPush()
        
        if (!pushResult.success) {
            return@suspend UiSyncResult(
                success = false,
                message = pushResult.message
            )
        }
        
        // Pull фаза
        onProgress?.invoke(SyncStep.PULL_CLIENTS)
        val pullResult = syncEngine.syncPull()
        
        UiSyncResult(
            success = pushResult.success && pullResult.success,
            message = "Push: ${pushResult.message}; Pull: ${pullResult.message}"
        )
    }
    
    /**
     * Создаёт SyncFunction для полной синхронизации (syncFull).
     * Используется для ручной синхронизации в настройках.
     */
    fun createFullSyncFunction(context: Context): SyncFunction = suspend@{ onProgress ->
        val syncEngine = SyncEngine(context)
        
        onProgress?.invoke(SyncStep.PUSH_CLIENTS)
        val result = syncEngine.syncFull()
        
        UiSyncResult(
            success = result.success,
            message = result.message
        )
    }
}

