package ru.wassertech.client.auth

/**
 * Тип происхождения сущности (откуда она была создана).
 */
enum class OriginType {
    /**
     * Сущность создана инженером в CRM (официальные данные под сервисный контракт).
     * В app-client такие сущности доступны только для просмотра (read-only).
     */
    CRM,
    
    /**
     * Сущность создана самим клиентом в app-client (его внутренние записи).
     * В app-client такие сущности можно редактировать и удалять.
     */
    CLIENT;
    
    companion object {
        /**
         * Преобразует строковое значение в OriginType.
         * По умолчанию возвращает CRM (для совместимости со старыми данными).
         */
        fun fromString(value: String?): OriginType {
            return try {
                if (value == null) CRM else valueOf(value.uppercase())
            } catch (e: Exception) {
                CRM // По умолчанию считаем CRM
            }
        }
        
        /**
         * Преобразует числовое значение (0=CRM, 1=CLIENT) в OriginType.
         */
        fun fromInt(value: Int): OriginType {
            return when (value) {
                0 -> CRM
                1 -> CLIENT
                else -> CRM // По умолчанию
            }
        }
        
        /**
         * Преобразует OriginType в числовое значение (0=CRM, 1=CLIENT).
         */
        fun OriginType.toInt(): Int {
            return when (this) {
                CRM -> 0
                CLIENT -> 1
            }
        }
    }
}

