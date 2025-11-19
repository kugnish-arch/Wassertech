package ru.wassertech.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import ru.wassertech.core.network.TokenStorage
import ru.wassertech.core.network.interceptor.AuthInterceptor
import ru.wassertech.core.network.interceptor.ErrorInterceptor
import ru.wassertech.core.network.interceptor.SessionExpiredCallback

/**
 * API клиент для сетевых запросов
 */
object ApiClient {
    
    @PublishedApi
    internal var baseUrl: String = "https://api.example.com/"
    
    /**
     * Глобальный callback для обработки истечения сессии.
     * Устанавливается при инициализации приложения.
     */
    private var globalSessionExpiredCallback: SessionExpiredCallback? = null
    
    /**
     * Инициализация клиента с базовым URL
     */
    fun initialize(baseUrl: String) {
        this.baseUrl = baseUrl
    }
    
    /**
     * Устанавливает глобальный callback для обработки истечения сессии.
     * Вызывается при инициализации приложения (например, в MainActivity).
     */
    fun setSessionExpiredCallback(callback: SessionExpiredCallback?) {
        globalSessionExpiredCallback = callback
    }
    
    /**
     * Создает OkHttpClient с interceptors
     */
    fun createOkHttpClient(
        tokenStorage: TokenStorage? = null,
        enableLogging: Boolean = true,
        sessionExpiredCallback: SessionExpiredCallback? = null
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Увеличено до 60 секунд
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // Включаем retry при ошибках соединения
        
        // Auth interceptor (добавляем первым, чтобы токен был в заголовках)
        tokenStorage?.let {
            builder.addInterceptor(AuthInterceptor(it))
        }
        
        // Logging interceptor (добавляем перед ErrorInterceptor для лучшего логирования)
        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        // Error interceptor (добавляем последним)
        // Используем переданный callback или глобальный
        val callback = sessionExpiredCallback ?: globalSessionExpiredCallback
        builder.addInterceptor(ErrorInterceptor(callback))
        
        return builder.build()
    }
    
    /**
     * Создает Retrofit instance
     */
    fun createRetrofit(
        okHttpClient: OkHttpClient,
        baseUrl: String = this.baseUrl
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Создает API service
     */
    inline fun <reified T> createService(
        tokenStorage: TokenStorage? = null,
        baseUrl: String = this.baseUrl,
        enableLogging: Boolean = true,
        sessionExpiredCallback: SessionExpiredCallback? = null
    ): T {
        val okHttpClient = createOkHttpClient(tokenStorage, enableLogging, sessionExpiredCallback)
        val retrofit = createRetrofit(okHttpClient, baseUrl)
        return retrofit.create(T::class.java)
    }
}

