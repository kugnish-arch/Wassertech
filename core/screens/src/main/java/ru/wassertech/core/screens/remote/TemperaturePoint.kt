package ru.wassertech.core.screens.remote

import java.text.SimpleDateFormat
import java.util.*

/**
 * Доменная модель точки измерения температуры
 */
data class TemperaturePoint(
    val timestampMillis: Long,
    val valueCelsius: Float
)

/**
 * Маппер из DTO в доменную модель
 */
fun ru.wassertech.core.network.dto.TemperatureLogItemDto.toTemperaturePoint(): TemperaturePoint {
    // Парсим timestamp в формате "YYYY-MM-DD HH:MM:SS"
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val date = format.parse(timestamp) ?: Date()
    val timestampMillis = date.time
    
    return TemperaturePoint(
        timestampMillis = timestampMillis,
        valueCelsius = value.toFloat()
    )
}

