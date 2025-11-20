package ru.wassertech.core.network.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ru.wassertech.core.network.dto.TemperatureLogResponseDto

/**
 * API для удалённого мониторинга датчиков
 */
interface RemoteMonitoringApi {
    
    /**
     * Получение логов температуры для устройства
     * @param deviceId ID устройства
     * @param from Начальная дата и время в формате "YYYY-MM-DD+HH:MM:SS" (плюс вместо пробела)
     * @param to Конечная дата и время в формате "YYYY-MM-DD+HH:MM:SS" (плюс вместо пробела)
     */
    @GET("sensors/temperature/get.php")
    suspend fun getTemperatureLogs(
        @Query("device_id") deviceId: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<TemperatureLogResponseDto>
}

