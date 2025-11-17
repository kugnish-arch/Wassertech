package ru.wassertech.core.network.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import ru.wassertech.core.network.dto.SyncPullResponse
import ru.wassertech.core.network.dto.SyncPushRequest
import ru.wassertech.core.network.dto.SyncPushResponse

/**
 * API для синхронизации данных с сервером
 */
interface SyncApi {
    
    /**
     * Отправка локальных изменений на сервер
     */
    @POST("sync/push")
    suspend fun syncPush(@Body request: SyncPushRequest): Response<SyncPushResponse>
    
    /**
     * Получение изменений с сервера
     * @param since Unix timestamp последней синхронизации (в секундах)
     * @param entities Список сущностей для получения (например, ["icon_packs", "icons"])
     * @param clientId Опциональный ID клиента для фильтрации данных (используется в app-client для роли CLIENT)
     */
    @GET("sync/pull")
    suspend fun syncPull(
        @Query("since") since: Long,
        @Query("entities[]") entities: List<String>? = null,
        @Query("client_id") clientId: String? = null
    ): Response<SyncPullResponse>
}

