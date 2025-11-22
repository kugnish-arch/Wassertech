package ru.wassertech.data.types

/**
 * Статус синхронизации записи с сервером.
 * Используется для отслеживания состояния записей в оффлайн-очереди.
 */
enum class SyncStatus(val value: Int) {
    /** Запись синхронизирована с сервером */
    SYNCED(0),
    
    /** Запись в очереди на отправку */
    QUEUED(1),
    
    /** Обнаружен конфликт при синхронизации */
    CONFLICT(2);
    
    companion object {
        fun fromInt(value: Int): SyncStatus {
            return values().find { it.value == value } ?: SYNCED
        }
    }
}





