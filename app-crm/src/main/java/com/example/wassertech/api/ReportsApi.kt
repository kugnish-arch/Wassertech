package ru.wassertech.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import ru.wassertech.core.network.dto.ReportDto

/**
 * API для работы с отчётами.
 * Используется в app-crm для загрузки и получения PDF-отчётов.
 */
interface ReportsApi {
    
    /**
     * Загрузка PDF-отчёта на сервер.
     * 
     * @param sessionId UUID сессии ТО (обязательно)
     * @param fileName Имя файла для отображения (опционально)
     * @param file PDF-файл
     * 
     * @return Response с данными созданного отчёта (201 Created)
     */
    @Multipart
    @POST("reports/upload")
    suspend fun uploadReport(
        @Part("sessionId") sessionId: RequestBody,
        @Part("fileName") fileName: RequestBody?,
        @Part file: MultipartBody.Part
    ): Response<ReportDto>
    
    /**
     * Получение списка отчётов.
     * 
     * Для ADMIN/ENGINEER возвращаются все отчёты с учётом опционального фильтра clientId.
     * Для остальных ролей - только отчёты, к которым есть доступ через user_membership.
     * 
     * @param sessionId Фильтрация по сессии ТО (опционально)
     * @param installationId Фильтрация по установке (опционально)
     * @param limit Пагинация - максимальное количество записей
     * @param offset Пагинация - смещение
     * 
     * @return Response со списком отчётов (200 OK)
     */
    @GET("reports")
    suspend fun getReports(
        @Query("sessionId") sessionId: String? = null,
        @Query("installationId") installationId: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<List<ReportDto>>
}


