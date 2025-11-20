package ru.wassertech.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для элемента лога температуры
 */
data class TemperatureLogItemDto(
    val timestamp: String,
    val value: Double
)

/**
 * DTO для ответа API логов температуры
 */
data class TemperatureLogResponseDto(
    @SerializedName("device_id") val deviceId: String,
    val count: Int,
    val data: List<TemperatureLogItemDto>
)

