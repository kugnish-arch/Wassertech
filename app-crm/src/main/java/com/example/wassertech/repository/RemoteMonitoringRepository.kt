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
import ru.wassertech.core.screens.remote.toTemperaturePoint

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
            
            // Запрашиваем последние 1000 точек (самые новые), чтобы гарантированно получить все данные
            val response = api.getTemperatureLogs(deviceId, from, to, limit = 1000)
            
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
    
    /**
     * Получает последнее значение температуры для устройства через API
     * @param deviceId ID устройства
     * @return Последнее значение температуры в градусах Цельсия или null, если данных нет
     */
    suspend fun getLatestTemperature(deviceId: String): Double? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Запрос последнего значения температуры для deviceId=$deviceId")
            
            // Запрашиваем последний час (датчик считывает температуру 1 раз в минуту, максимум 60 записей)
            val now = java.time.LocalDateTime.now()
            val hourAgo = now.minusHours(1)
            
            // Форматируем даты в формат "YYYY-MM-DD+HH:MM:SS"
            val from = hourAgo.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd+HH:mm:ss"))
            val to = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd+HH:mm:ss"))
            
            Log.d(TAG, "Запрос температуры: from=$from, to=$to, limit=100")
            val response = api.getTemperatureLogs(deviceId, from, to, limit = 100)
            
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null && dto.data.isNotEmpty()) {
                    // Преобразуем в TemperaturePoint для правильной сортировки по timestampMillis
                    val points = dto.data.map { it.toTemperaturePoint() }
                    // Сортируем по timestampMillis по убыванию (самые новые первыми) и берем самое новое значение
                    val sortedPoints = points.sortedByDescending { it.timestampMillis }
                    val latestPoint = sortedPoints.first()
                    val latestValue = latestPoint.valueCelsius.toDouble()
                    
                    Log.d(TAG, "Получено последнее значение температуры для deviceId=$deviceId: $latestValue (timestamp=${latestPoint.timestampMillis}, из ${dto.data.size} записей)")
                    Log.d(TAG, "Все значения (первые 5): ${sortedPoints.take(5).map { "${it.valueCelsius} (${it.timestampMillis})" }}")
                    return@withContext latestValue
                } else {
                    Log.d(TAG, "Нет данных температуры для deviceId=$deviceId")
                    return@withContext null
                }
            } else {
                Log.w(TAG, "HTTP ${response.code()}: ${response.message()} при запросе температуры для deviceId=$deviceId")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении последнего значения температуры для deviceId=$deviceId", e)
            return@withContext null
        }
    }
}

