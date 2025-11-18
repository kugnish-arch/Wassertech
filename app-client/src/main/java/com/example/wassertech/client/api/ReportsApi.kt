package ru.wassertech.client.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ru.wassertech.core.network.dto.ReportDto

/**
 * API для работы с отчётами в app-client.
 * Используется для получения списка отчётов клиента.
 */
interface ReportsApi {
    
    /**
     * Получение списка отчётов.
     * 
     * Для роли CLIENT:
     * - Возвращаются только отчёты клиента, к которому привязан пользователь
     * - Параметр clientId игнорируется (используется users.client_id из токена)
     * - Возвращаются только неархивные отчёты
     * 
     * @param sinceUpdatedAtEpoch Unix timestamp в мс. Если указан, возвращаются только отчёты с updatedAtEpoch > sinceUpdatedAtEpoch
     * 
     * @return Response со списком отчётов (200 OK)
     */
    @GET("reports/list")
    suspend fun getReports(
        @Query("sinceUpdatedAtEpoch") sinceUpdatedAtEpoch: Long? = null
    ): Response<List<ReportDto>>
}


