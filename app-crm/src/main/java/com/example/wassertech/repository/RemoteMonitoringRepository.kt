package ru.wassertech.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.wassertech.core.auth.DataStoreTokenStorage
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.api.RemoteMonitoringApi
import ru.wassertech.core.screens.remote.TemperaturePoint
import ru.wassertech.core.screens.remote.toTemperaturePoint
import ru.wassertech.core.network.ApiConfig.BASE_URL

/**
 * Репозиторий для работы с удалённым мониторингом датчиков
 */
class RemoteMonitoringRepository(private val context: Context) {
    
    private val tokenStorage = DataStoreTokenStorage(context)
    
    // Базовый URL для API мониторинга (без /public/, так как sensors API находится в /api/sensors/)
    //private val monitoringBaseUrl: String = "https://2024.wassertech.ru/api/public/"
    private val monitoringBaseUrl: String = BASE_URL;

    private val api: RemoteMonitoringApi by lazy {

        ApiClient.createService<RemoteMonitoringApi>(
            tokenStorage = tokenStorage,
            baseUrl = monitoringBaseUrl,
            enableLogging = true
        )
    }
    
    companion object {
        private const val TAG = "RemoteMonitoringRepository"
    }
    
    /**
     * Загружает точки температуры для устройства за указанный период
     * @param deviceId ID устройства
     * @param from Начальная дата и время в формате "YYYY-MM-DD+HH:MM:SS" (плюс вместо пробела)
     * @param to Конечная дата и время в формате "YYYY-MM-DD+HH:MM:SS" (плюс вместо пробела)
     * @return Список точек температуры, отсортированных по времени
     */
    suspend fun loadTemperaturePoints(
        deviceId: String,
        from: String,
        to: String
    ): List<TemperaturePoint> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Загрузка логов температуры: deviceId=$deviceId, from=$from, to=$to")
            
            val response = api.getTemperatureLogs(deviceId, from, to)
            
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    Log.d(TAG, "Получено ${dto.count} точек температуры")
                    val points = dto.data
                        .sortedBy { it.timestamp }
                        .map { it.toTemperaturePoint() }
                    return@withContext points
                } else {
                    Log.w(TAG, "Ответ пустой")
                    return@withContext emptyList()
                }
            } else {
                val errorMessage = "HTTP ${response.code()}: ${response.message()}"
                Log.e(TAG, errorMessage)
                throw HttpException(response)
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ошибка при загрузке логов температуры", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке логов температуры", e)
            throw e
        }
    }
}

