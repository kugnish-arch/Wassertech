package ru.wassertech.auth

import android.content.Context
import ru.wassertech.core.auth.AuthRepositoryImpl
import ru.wassertech.core.auth.AuthTokenData
import ru.wassertech.core.auth.CurrentUserData
import ru.wassertech.data.entities.UserEntity

/**
 * Репозиторий для работы с авторизацией через REST API
 * 
 * Обёртка над AuthRepositoryImpl из core/auth для обратной совместимости
 * Используйте ru.wassertech.core.auth.createAuthRepository() напрямую
 */
@Deprecated("Используйте ru.wassertech.core.auth.createAuthRepository()", ReplaceWith("ru.wassertech.core.auth.createAuthRepository(context)"))
class AuthRepository(private val context: Context) {
    
    private val impl = AuthRepositoryImpl(context)
    
    /**
     * Выполнить вход в систему
     */
    suspend fun login(login: String, password: String): Result<AuthTokenData> {
        return impl.login(login, password)
    }
    
    /**
     * Загрузить информацию о текущем пользователе
     */
    suspend fun loadCurrentUser(): Result<CurrentUserData> {
        return impl.loadCurrentUser()
    }
    
    /**
     * Получить текущий токен
     */
    suspend fun getToken(): String? {
        return impl.getToken()
    }
    
    /**
     * Проверить, авторизован ли пользователь
     */
    suspend fun isAuthenticated(): Boolean {
        return impl.isAuthenticated()
    }
    
    /**
     * Выход из системы
     */
    suspend fun logout() {
        impl.logout()
    }
}

/**
 * Данные токена авторизации
 * @deprecated Используйте ru.wassertech.core.auth.AuthTokenData
 */
@Deprecated("Используйте ru.wassertech.core.auth.AuthTokenData", ReplaceWith("ru.wassertech.core.auth.AuthTokenData"))
typealias AuthTokenData = ru.wassertech.core.auth.AuthTokenData

/**
 * Данные текущего пользователя
 * @deprecated Используйте ru.wassertech.core.auth.CurrentUserData
 */
@Deprecated("Используйте ru.wassertech.core.auth.CurrentUserData", ReplaceWith("ru.wassertech.core.auth.CurrentUserData"))
typealias CurrentUserData = ru.wassertech.core.auth.CurrentUserData

/**
 * Extension функция для преобразования CurrentUserData в UserEntity
 */
fun ru.wassertech.core.auth.CurrentUserData.toUserEntity(): UserEntity {
    return UserEntity(
        id = id,
        login = login,
        password = "", // Пароль не возвращается из API
        name = name,
        email = email,
        phone = phone,
        role = role,
        permissions = permissions,
        lastLoginAtEpoch = lastLoginAtEpoch,
        createdAtEpoch = createdAtEpoch,
        updatedAtEpoch = updatedAtEpoch
    )
}

