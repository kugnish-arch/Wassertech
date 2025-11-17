package ru.wassertech.client.auth

import android.content.Context
import android.util.Log
import retrofit2.HttpException
import ru.wassertech.core.network.ApiClient
import ru.wassertech.core.network.ApiConfig
import ru.wassertech.core.network.api.WassertechApi
import ru.wassertech.core.network.dto.LoginRequest
import ru.wassertech.core.network.dto.LoginResponse

/**
 * Репозиторий для работы с авторизацией через API
 */
class AuthRepository(private val context: Context) {
    
    private val tokenStorage = ru.wassertech.core.auth.DataStoreTokenStorage(context)
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
                    // Детальное логирование ответа от сервера
                    Log.d(TAG, "=== ОТВЕТ ОТ СЕРВЕРА ПРИ ЛОГИНЕ ===")
                    Log.d(TAG, "Токен: ${loginResponse.token.take(20)}... (длина: ${loginResponse.token.length})")
                    Log.d(TAG, "Exp: ${loginResponse.exp}")
                    
                    loginResponse.user?.let { userDto ->
                        Log.d(TAG, "Данные пользователя:")
                        Log.d(TAG, "  - id: ${userDto.id}")
                        Log.d(TAG, "  - login: ${userDto.login}")
                        Log.d(TAG, "  - role: ${userDto.role}")
                        Log.d(TAG, "  - clientId: ${userDto.clientId}")
                        Log.d(TAG, "  - name: ${userDto.name}")
                        Log.d(TAG, "  - email: ${userDto.email}")
                    } ?: run {
                        Log.w(TAG, "⚠️ В ответе отсутствует объект user!")
                    }
                    
                    // Сохраняем токен
                    tokenStorage.saveAccessTokenAsync(loginResponse.token)
                    
                    // Проверяем, что токен сохранился
                    val savedToken = tokenStorage.getAccessToken()
                    if (savedToken != null) {
                        Log.d(TAG, "Токен успешно сохранен и проверен: ${savedToken.take(20)}...")
                    } else {
                        Log.e(TAG, "ОШИБКА: Токен не сохранился после saveAccessTokenAsync!")
                    }
                    
                    // Если в ответе есть данные пользователя, создаём сессию
                    loginResponse.user?.let { userDto ->
                        val session = ru.wassertech.core.auth.UserSessionImpl(
                            userId = userDto.id,
                            login = userDto.login,
                            role = ru.wassertech.core.auth.UserRole.fromString(userDto.role),
                            clientId = userDto.clientId,
                            name = userDto.name,
                            email = userDto.email
                        )
                        ru.wassertech.core.auth.SessionManager.getInstance(context).setCurrentSession(session)
                        Log.d(TAG, "=== СОЗДАНА СЕССИЯ ===")
                        Log.d(TAG, "  - userId: ${session.userId}")
                        Log.d(TAG, "  - role: ${session.role} (name: ${session.role.name})")
                        Log.d(TAG, "  - clientId: ${session.clientId}")
                        Log.d(TAG, "  - login: ${session.login}")
                        Log.d(TAG, "  - name: ${session.name}")
                        Log.d(TAG, "  - email: ${session.email}")
                    } ?: run {
                        Log.w(TAG, "⚠️ Не удалось создать сессию: userDto отсутствует в ответе")
                    }
                    
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
        ru.wassertech.core.auth.SessionManager.getInstance(context).clearSession()
        Log.d(TAG, "Пользователь вышел из системы, сессия очищена")
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

