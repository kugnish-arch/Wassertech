package com.example.wassertech.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.wassertech.core.auth.TokenStorage
import com.example.wassertech.core.network.interceptor.AuthInterceptor
import com.example.wassertech.core.network.interceptor.ErrorInterceptor

/**
 * API клиент для сетевых запросов
 */
object ApiClient {
    
    @PublishedApi
    internal var baseUrl: String = "https://api.example.com/"
    
    /**
     * Инициализация клиента с базовым URL
     */
    fun initialize(baseUrl: String) {
        this.baseUrl = baseUrl
    }
    
    /**
     * Создает OkHttpClient с interceptors
     */
    fun createOkHttpClient(
        tokenStorage: TokenStorage? = null,
        enableLogging: Boolean = true
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        // Auth interceptor
        tokenStorage?.let {
            builder.addInterceptor(AuthInterceptor(it))
        }
        
        // Error interceptor
        builder.addInterceptor(ErrorInterceptor())
        
        // Logging interceptor
        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
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
        enableLogging: Boolean = true
    ): T {
        val okHttpClient = createOkHttpClient(tokenStorage, enableLogging)
        val retrofit = createRetrofit(okHttpClient, baseUrl)
        return retrofit.create(T::class.java)
    }
}

