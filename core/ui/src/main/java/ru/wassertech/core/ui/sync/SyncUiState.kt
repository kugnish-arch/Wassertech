package ru.wassertech.core.ui.sync

/**
 * Шаги синхронизации для отображения прогресса пользователю.
 * Порядок соответствует реальному порядку выполнения в SyncEngine.
 */
enum class SyncStep(val displayName: String) {
    PUSH_CLIENTS("Отправка клиентов"),
    PUSH_SITES("Отправка объектов"),
    PUSH_INSTALLATIONS("Отправка установок"),
    PUSH_COMPONENTS("Отправка компонентов"),
    PUSH_SESSIONS("Отправка сессий ТО"),
    PUSH_VALUES("Отправка значений ТО"),
    PUSH_TEMPLATES("Отправка шаблонов"),
    PUSH_ICON_PACKS("Отправка икон-паков"),
    
    PULL_CLIENTS("Загрузка клиентов"),
    PULL_SITES("Загрузка объектов"),
    PULL_INSTALLATIONS("Загрузка установок"),
    PULL_COMPONENTS("Загрузка компонентов"),
    PULL_SESSIONS("Загрузка сессий ТО"),
    PULL_VALUES("Загрузка значений ТО"),
    PULL_TEMPLATES("Загрузка шаблонов"),
    PULL_ICON_PACKS("Загрузка икон-паков"),
    PULL_ICONS("Загрузка иконок"),
    PULL_DELETED("Обработка удалений"),
    
    COMPLETED("Синхронизация завершена")
}

/**
 * Тип ошибки синхронизации.
 */
sealed class SyncErrorType {
    object Network : SyncErrorType() // Ошибка сети (нет интернета, timeout)
    object Server : SyncErrorType() // Ошибка сервера (HTTP 5xx)
    object Auth : SyncErrorType() // Ошибка авторизации (HTTP 401, 403)
    object Parse : SyncErrorType() // Ошибка парсинга ответа
    object Unknown : SyncErrorType() // Неизвестная ошибка
}

/**
 * Информация об ошибке синхронизации.
 */
data class SyncError(
    val type: SyncErrorType,
    val message: String,
    val httpCode: Int? = null,
    val exception: Throwable? = null
) {
    /**
     * Форматированное сообщение для пользователя.
     */
    fun getUserMessage(): String {
        return when (type) {
            is SyncErrorType.Network -> "Ошибка подключения к серверу. Проверьте интернет-соединение."
            is SyncErrorType.Server -> "Ошибка сервера${if (httpCode != null) " (HTTP $httpCode)" else ""}. Попробуйте позже."
            is SyncErrorType.Auth -> "Ошибка авторизации${if (httpCode != null) " (HTTP $httpCode)" else ""}. Необходимо войти заново."
            is SyncErrorType.Parse -> "Ошибка обработки данных с сервера."
            is SyncErrorType.Unknown -> message.ifBlank { "Неизвестная ошибка синхронизации" }
        }
    }
}

/**
 * Состояние UI синхронизации.
 */
data class SyncUiState(
    /**
     * Запущена ли синхронизация.
     */
    val isRunning: Boolean = false,
    
    /**
     * Текущий шаг синхронизации (null, если синхронизация не запущена).
     */
    val currentStep: SyncStep? = null,
    
    /**
     * Прогресс синхронизации от 0.0 до 1.0 (null, если indeterminate).
     */
    val progress: Float? = null,
    
    /**
     * Ошибка синхронизации (null, если ошибок нет).
     */
    val error: SyncError? = null,
    
    /**
     * Блокирующая ли синхронизация (true = показывать overlay, false = показывать только индикатор).
     */
    val isBlocking: Boolean = true,
    
    /**
     * Показывать ли диалог долгой синхронизации.
     */
    val showLongSyncDialog: Boolean = false,
    
    /**
     * Время начала синхронизации (для отслеживания таймаута).
     */
    val startTimeMs: Long? = null
) {
    /**
     * Проверяет, нужно ли показывать overlay.
     */
    fun shouldShowOverlay(): Boolean = isRunning && isBlocking
    
    /**
     * Проверяет, нужно ли показывать неблокирующий индикатор.
     */
    fun shouldShowIndicator(): Boolean = isRunning && !isBlocking
    
    /**
     * Проверяет, есть ли ошибка.
     */
    fun hasError(): Boolean = error != null
}

