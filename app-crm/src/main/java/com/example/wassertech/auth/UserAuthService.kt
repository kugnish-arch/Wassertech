package com.example.wassertech.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.wassertech.data.entities.UserEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Локальная информация о пользователе для оффлайн доступа
 */
data class LocalUserInfo(
    val userId: String,
    val login: String,
    val role: String,
    val permissions: String?,
    val lastLoginAtEpoch: Long, // Время последнего входа на это устройство
    val lastRemoteLoginAtEpoch: Long? // Время последнего входа в удаленной БД
)

/**
 * Сервис для управления состоянием входа пользователя
 */
object UserAuthService {
    private const val PREFS_NAME = "user_auth_prefs"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_LOCAL_USERS = "local_users" // JSON массив с локальными пользователями
    private const val KEY_OFFLINE_MODE = "offline_mode" // Флаг оффлайн режима
    private const val OFFLINE_ACCESS_DAYS = 90L // Количество дней для оффлайн доступа
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Сохраняет информацию о входе пользователя (локально и обновляет список локальных пользователей)
     * @param isOfflineMode true если вход через оффлайн режим, false если обычный вход с онлайн проверкой
     */
    fun saveLogin(context: Context, user: UserEntity, isOfflineMode: Boolean = false) {
        val prefs = getSharedPreferences(context)
        val now = System.currentTimeMillis()
        
        // Сохраняем текущего пользователя
        prefs.edit()
            .putString(KEY_CURRENT_USER_ID, user.id)
            .putBoolean(KEY_OFFLINE_MODE, isOfflineMode)
            .apply()
        
        // Добавляем/обновляем пользователя в списке локальных пользователей
        val localUsers = getLocalUsers(context).toMutableList()
        val existingUserIndex = localUsers.indexOfFirst { it.userId == user.id }
        
        val localUserInfo = LocalUserInfo(
            userId = user.id,
            login = user.login,
            role = user.role,
            permissions = user.permissions,
            lastLoginAtEpoch = now, // Время входа на это устройство
            lastRemoteLoginAtEpoch = user.lastLoginAtEpoch // Время последнего входа в удаленной БД
        )
        
        if (existingUserIndex >= 0) {
            localUsers[existingUserIndex] = localUserInfo
        } else {
            localUsers.add(localUserInfo)
        }
        
        // Сохраняем обновленный список
        saveLocalUsers(context, localUsers)
    }
    
    /**
     * Выход пользователя
     */
    fun logout(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .remove(KEY_CURRENT_USER_ID)
            .apply()
    }
    
    /**
     * Проверяет, вошёл ли пользователь
     */
    fun isLoggedIn(context: Context): Boolean {
        return getCurrentUserId(context) != null
    }
    
    /**
     * Получает ID текущего пользователя
     */
    fun getCurrentUserId(context: Context): String? {
        val prefs = getSharedPreferences(context)
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }
    
    /**
     * Получает логин текущего пользователя
     */
    fun getCurrentUserLogin(context: Context): String? {
        val userId = getCurrentUserId(context) ?: return null
        return getLocalUserByUserId(context, userId)?.login
    }
    
    /**
     * Получает информацию о локальном пользователе по логину (регистронезависимый поиск)
     */
    fun getLocalUserByLogin(context: Context, login: String): LocalUserInfo? {
        val loginLower = login.lowercase()
        return getLocalUsers(context).find { it.login.lowercase() == loginLower }
    }
    
    /**
     * Получает информацию о локальном пользователе по ID
     */
    fun getLocalUserByUserId(context: Context, userId: String): LocalUserInfo? {
        return getLocalUsers(context).find { it.userId == userId }
    }
    
    /**
     * Получает список всех локальных пользователей
     */
    fun getLocalUsers(context: Context): List<LocalUserInfo> {
        val prefs = getSharedPreferences(context)
        val jsonString = prefs.getString(KEY_LOCAL_USERS, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val users = mutableListOf<LocalUserInfo>()
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                users.add(
                    LocalUserInfo(
                        userId = jsonObj.getString("userId"),
                        login = jsonObj.getString("login"),
                        role = jsonObj.optString("role", UserRole.USER.name),
                        permissions = jsonObj.optString("permissions", null),
                        lastLoginAtEpoch = jsonObj.getLong("lastLoginAtEpoch"),
                        lastRemoteLoginAtEpoch = if (jsonObj.has("lastRemoteLoginAtEpoch") && !jsonObj.isNull("lastRemoteLoginAtEpoch")) {
                            jsonObj.getLong("lastRemoteLoginAtEpoch")
                        } else null
                    )
                )
            }
            users
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Устанавливает текущего пользователя по ID (для оффлайн режима)
     * Обновляет время последнего входа для этого пользователя и устанавливает флаг оффлайн режима
     */
    fun setCurrentUser(context: Context, userId: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .putString(KEY_CURRENT_USER_ID, userId)
            .putBoolean(KEY_OFFLINE_MODE, true) // Устанавливаем флаг оффлайн режима
            .apply()
        
        // Обновляем время последнего входа для этого пользователя
        val localUsers = getLocalUsers(context).toMutableList()
        val index = localUsers.indexOfFirst { it.userId == userId }
        if (index >= 0) {
            val user = localUsers[index]
            val updatedUser = user.copy(lastLoginAtEpoch = System.currentTimeMillis())
            localUsers[index] = updatedUser
            saveLocalUsers(context, localUsers)
        }
    }
    
    /**
     * Проверяет, включен ли оффлайн режим
     */
    fun isOfflineMode(context: Context): Boolean {
        val prefs = getSharedPreferences(context)
        return prefs.getBoolean(KEY_OFFLINE_MODE, false)
    }
    
    /**
     * Сохраняет список локальных пользователей
     */
    private fun saveLocalUsers(context: Context, users: List<LocalUserInfo>) {
        val prefs = getSharedPreferences(context)
        try {
            val jsonArray = JSONArray()
            users.forEach { user ->
                val jsonObj = JSONObject()
                jsonObj.put("userId", user.userId)
                jsonObj.put("login", user.login)
                jsonObj.put("role", user.role)
                jsonObj.put("permissions", user.permissions)
                jsonObj.put("lastLoginAtEpoch", user.lastLoginAtEpoch)
                jsonObj.put("lastRemoteLoginAtEpoch", user.lastRemoteLoginAtEpoch)
                jsonArray.put(jsonObj)
            }
            prefs.edit()
                .putString(KEY_LOCAL_USERS, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            // В случае ошибки просто не сохраняем
        }
    }
    
    /**
     * Проверяет, можно ли использовать оффлайн доступ для пользователя
     * @return Pair<Boolean, String?> - (можно ли использовать, сообщение об ошибке или null)
     */
    fun canUseOfflineAccess(context: Context, login: String): Pair<Boolean, String?> {
        val localUser = getLocalUserByLogin(context, login) ?: return false to "Пользователь не найден в локальной записи устройства"
        
        val now = System.currentTimeMillis()
        val lastLogin = localUser.lastLoginAtEpoch
        val daysSinceLastLogin = (now - lastLogin) / MILLIS_PER_DAY
        
        if (daysSinceLastLogin > OFFLINE_ACCESS_DAYS) {
            return false to "Превышен срок оффлайн доступа (90 дней)"
        }
        
        return true to null
    }
    
    /**
     * Получает права доступа текущего пользователя
     */
    fun getCurrentUserPermissions(context: Context): UserPermissions? {
        val userId = getCurrentUserId(context) ?: return null
        val localUser = getLocalUserByUserId(context, userId) ?: return null
        return UserPermissions.fromJson(localUser.permissions)
    }
    
    /**
     * Получает стартовый экран для текущего пользователя
     */
    fun getCurrentUserStartScreen(context: Context): String {
        return getCurrentUserPermissions(context)?.startScreen ?: "clients"
    }
}

