package ru.wassertech.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import ru.wassertech.core.network.dto.ReportDto

/**
 * API для работы с отчётами (загрузка PDF).
 * Используется в app-crm для загрузки сгенерированных отчётов на сервер.
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
}

