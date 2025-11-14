package ru.wassertech.core.auth

import android.content.Context
import android.util.Log
import retrofit2.HttpException
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.ApiConfig
import ru.wassertech.core.network.api.WassertechApi
import ru.wassertech.core.network.dto.LoginRequest

/**
 * Репозиторий для работы с авторизацией через REST API
 */
class AuthRepositoryImpl(private val context: Context) {
    
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
    suspend fun login(login: String, password: String): Result<AuthTokenData> {
        Log.d(TAG, "=== Начало авторизации через REST API ===")
        Log.d(TAG, "AuthRepository.login() вызван с login='$login'")
        return try {
            // Убираем пробелы только в начале и конце логина, но не в пароле
            val trimmedLogin = login.trim()
            val request = LoginRequest(login = trimmedLogin, password = password)
            
            Log.d(TAG, "=== Отправка REST запроса ===")
            Log.d(TAG, "Base URL: ${ApiConfig.getBaseUrl()}")
            Log.d(TAG, "Endpoint: auth/login")
            Log.d(TAG, "Full URL: ${ApiConfig.getBaseUrl()}auth/login")
            Log.d(TAG, "Login: '$trimmedLogin' (длина=${trimmedLogin.length}, bytes=${trimmedLogin.toByteArray().size})")
            Log.d(TAG, "Password длина: ${password.length}, bytes=${password.toByteArray().size}")
            Log.d(TAG, "Request: LoginRequest(login='$trimmedLogin', password=***)")
            
            Log.d(TAG, "Вызов api.login() через Retrofit (REST API, НЕ JDBC)")
            val response = api.login(request)
            
            Log.d(TAG, "Ответ получен: code=${response.code()}, isSuccessful=${response.isSuccessful}")
            Log.d(TAG, "Это REST API ответ, НЕ JDBC ответ")
            
            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    // Сохраняем токен
                    tokenStorage.saveAccessTokenAsync(loginResponse.token)
                    
                    Log.d(TAG, "Успешный вход. Токен сохранен. Exp: ${loginResponse.exp}")
                    Result.success(
                        AuthTokenData(
                            token = loginResponse.token,
                            exp = loginResponse.exp
                        )
                    )
                } else {
                    Log.e(TAG, "Пустой ответ от сервера")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                // Читаем тело ошибки
                val errorBody = try {
                    response.errorBody()?.string()
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка чтения тела ошибки", e)
                    null
                }
                Log.e(TAG, "Ошибка авторизации: code=${response.code()}, body=$errorBody")
                
                val errorMessage = when (response.code()) {
                    401 -> {
                        // Пытаемся извлечь сообщение из тела ошибки
                        errorBody?.takeIf { it.isNotBlank() } ?: "Неверный логин или пароль"
                    }
                    403 -> "Доступ запрещен"
                    else -> "Ошибка авторизации: ${response.code()}" + 
                            if (errorBody != null) " ($errorBody)" else ""
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            // HttpException выбрасывается только для успешных кодов, но с ошибкой парсинга
            // Для неуспешных кодов Retrofit не выбрасывает HttpException
            Log.e(TAG, "HTTP ошибка при входе: code=${e.code()}, message=${e.message()}", e)
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (ex: Exception) {
                null
            }
            Log.e(TAG, "Тело ошибки: $errorBody")
            Result.failure(Exception("Ошибка сети: ${e.message()}"))
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
            Result.failure(Exception(errorMessage))
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
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при входе", e)
            Result.failure(Exception("Ошибка при входе: ${e.message ?: "Неизвестная ошибка"}"))
        }
    }
    
    /**
     * Загрузить информацию о текущем пользователе
     */
    suspend fun loadCurrentUser(): Result<CurrentUserData> {
        return try {
            val response = api.getCurrentUser()
            
            if (response.isSuccessful) {
                val userMeResponse = response.body()
                if (userMeResponse != null) {
                    Log.d(TAG, "Информация о пользователе загружена: ${userMeResponse.login}")
                    Result.success(
                        CurrentUserData(
                            id = userMeResponse.id,
                            login = userMeResponse.login,
                            name = userMeResponse.name,
                            email = userMeResponse.email,
                            phone = userMeResponse.phone,
                            role = userMeResponse.role,
                            permissions = userMeResponse.permissions,
                            lastLoginAtEpoch = userMeResponse.lastLoginAtEpoch,
                            createdAtEpoch = userMeResponse.createdAtEpoch,
                            updatedAtEpoch = userMeResponse.updatedAtEpoch
                        )
                    )
                } else {
                    Log.e(TAG, "Пустой ответ от сервера при загрузке пользователя")
                    Result.failure(Exception("Пустой ответ от сервера"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "Токен недействителен или истек"
                    403 -> "Доступ запрещен"
                    else -> "Ошибка загрузки пользователя: ${response.code()}"
                }
                Log.e(TAG, "Ошибка загрузки пользователя: ${response.code()}")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ошибка при загрузке пользователя", e)
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        } catch (e: ru.wassertech.core.network.interceptor.NetworkException) {
            Log.e(TAG, "Сетевая ошибка при загрузке пользователя", e)
            Result.failure(Exception("Ошибка сети: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке пользователя", e)
            Result.failure(Exception("Ошибка при загрузке пользователя: ${e.message ?: "Неизвестная ошибка"}"))
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
 * Данные токена авторизации
 */
data class AuthTokenData(
    val token: String,
    val exp: Long
)

/**
 * Данные текущего пользователя
 */
data class CurrentUserData(
    val id: String,
    val login: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    val role: String,
    val permissions: String?,
    val lastLoginAtEpoch: Long?,
    val createdAtEpoch: Long,
    val updatedAtEpoch: Long
)

/**
 * Создать экземпляр AuthRepository для использования в UI
 */
fun createAuthRepository(context: Context): AuthRepositoryImpl {
    return AuthRepositoryImpl(context)
}

