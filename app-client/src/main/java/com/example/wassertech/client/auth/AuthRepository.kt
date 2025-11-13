package ru.wassertech.client.auth

import android.content.Context
import android.util.Log
import retrofit2.HttpException
import ru.wassertech.client.api.ApiConfig
import ru.wassertech.client.api.WassertechApi
import ru.wassertech.client.api.dto.LoginRequest
import ru.wassertech.core.network.ApiClient

/**
 * Репозиторий для работы с авторизацией через API
 */
class AuthRepository(private val context: Context) {
    
    private val tokenStorage = DataStoreTokenStorage(context)
    private val api: WassertechApi by lazy {
        ApiClient.createService<WassertechApi>(
            tokenStorage = tokenStorage,
            baseUrl = ApiConfig.getBaseUrl(),
            enableLogging = true
        )
    }
    
    companion object {
        private const val TAG = "AuthRepository"
    }
    
    /**
     * Выполнить вход в систему
     */
    suspend fun login(login: String, password: String): LoginResult {
        return try {
            val request = LoginRequest(login = login, password = password)
            val response = api.login(request)
            
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    // Сохраняем токен
                    tokenStorage.saveAccessTokenAsync(loginResponse.token)
                    
                    Log.d(TAG, "Успешный вход. Токен сохранен. Exp: ${loginResponse.exp}")
                    LoginResult.Success(
                        token = loginResponse.token,
                        exp = loginResponse.exp
                    )
                } else {
                    Log.e(TAG, "Пустой ответ от сервера")
                    LoginResult.Error("Пустой ответ от сервера")
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Неверный логин или пароль"
                    403 -> "Доступ запрещен"
                    else -> "Ошибка авторизации: ${response.code()}"
                }
                Log.e(TAG, "Ошибка авторизации: ${response.code()}")
                LoginResult.Error(errorMessage)
            }
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP ошибка при входе", e)
            LoginResult.Error("Ошибка сети: ${e.message}")
        } catch (e: ru.wassertech.core.network.interceptor.NetworkException) {
            Log.e(TAG, "Сетевая ошибка при входе", e)
            val errorMessage = when {
                e.message?.contains("exhausted all routes") == true -> {
                    "Не удалось подключиться к серверу. Проверьте интернет-соединение."
                }
                e.message?.contains("timeout") == true -> {
                    "Превышено время ожидания ответа от сервера."
                }
                e.message?.contains("SSL") == true || e.message?.contains("certificate") == true -> {
                    "Ошибка SSL сертификата. Обратитесь к администратору."
                }
                else -> "Ошибка сети: ${e.message}"
            }
            LoginResult.Error(errorMessage)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO ошибка при входе", e)
            val errorMessage = when {
                e.message?.contains("exhausted all routes") == true -> {
                    "Не удалось подключиться к серверу. Проверьте интернет-соединение и доступность сервера."
                }
                e.message?.contains("timeout") == true -> {
                    "Превышено время ожидания ответа от сервера."
                }
                else -> "Ошибка подключения: ${e.message}"
            }
            LoginResult.Error(errorMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при входе", e)
            LoginResult.Error("Ошибка при входе: ${e.message ?: "Неизвестная ошибка"}")
        }
    }
    
    /**
     * Получить текущий токен
     */
    suspend fun getToken(): String? {
        return tokenStorage.getAccessToken()
    }
    
    /**
     * Проверить, авторизован ли пользователь
     */
    suspend fun isAuthenticated(): Boolean {
        return tokenStorage.getAccessToken() != null
    }
    
    /**
     * Выход из системы
     */
    suspend fun logout() {
        tokenStorage.clearTokensAsync()
        Log.d(TAG, "Пользователь вышел из системы")
    }
}

/**
 * Результат авторизации
 */
sealed class LoginResult {
    data class Success(
        val token: String,
        val exp: Long
    ) : LoginResult()
    
    data class Error(
        val message: String
    ) : LoginResult()
}

