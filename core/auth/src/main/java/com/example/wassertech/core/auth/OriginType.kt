package ru.wassertech.core.auth

/**
 * Тип происхождения сущности (откуда она была создана).
 * Соответствует серверному полю origin в таблицах БД.
 */
enum class OriginType(val serverValue: String) {
    /**
     * Сущность создана инженером в CRM (официальные данные под сервисный контракт).
     * В app-client такие сущности доступны только для просмотра (read-only).
     */
    CRM("CRM"),
    
    /**
     * Сущность создана самим клиентом в app-client (его внутренние записи).
     * В app-client такие сущности можно редактировать и удалять.
     */
    CLIENT("CLIENT");
    
    companion object {
        /**
         * Преобразует строковое значение в OriginType.
         * По умолчанию возвращает CRM (для совместимости со старыми данными).
         */
        fun fromString(value: String?): OriginType {
            return try {
                if (value == null) CRM else {
                    when (value.uppercase()) {
                        "CRM" -> CRM
                        "CLIENT" -> CLIENT
                        else -> {
                            android.util.Log.w("OriginType", "Неизвестное значение origin: $value, используем CRM")
                            CRM
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OriginType", "Ошибка парсинга origin: $value", e)
                CRM // По умолчанию считаем CRM
            }
        }
    }
}


