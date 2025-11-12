package ru.wassertech.auth

import org.json.JSONObject

/**
 * Роли пользователей в системе
 */
enum class UserRole(val displayName: String) {
    ADMIN("Администратор"),
    USER("Пользователь"),
    VIEWER("Наблюдатель");
    
    companion object {
        fun fromString(value: String?): UserRole {
            return try {
                if (value == null) USER else valueOf(value)
            } catch (e: Exception) {
                USER // По умолчанию
            }
        }
    }
}

/**
 * Права доступа пользователя к страницам приложения
 */
data class UserPermissions(
    val canAccessClients: Boolean = true,
    val canAccessTemplates: Boolean = true,
    val canAccessReports: Boolean = true,
    val canAccessMaintenance: Boolean = true,
    val canAccessSettings: Boolean = true,
    val startScreen: String = "clients" // Экран, на который попадает пользователь после входа
) {
    companion object {
        /**
         * Создает JSON строку из объекта прав
         */
        fun toJson(permissions: UserPermissions): String {
            return try {
                val json = JSONObject()
                json.put("canAccessClients", permissions.canAccessClients)
                json.put("canAccessTemplates", permissions.canAccessTemplates)
                json.put("canAccessReports", permissions.canAccessReports)
                json.put("canAccessMaintenance", permissions.canAccessMaintenance)
                json.put("canAccessSettings", permissions.canAccessSettings)
                json.put("startScreen", permissions.startScreen)
                json.toString()
            } catch (e: Exception) {
                "{}" // В случае ошибки возвращаем пустой JSON
            }
        }
        
        /**
         * Создает объект прав из JSON строки
         */
        fun fromJson(json: String?): UserPermissions {
            if (json == null || json.isBlank()) {
                return UserPermissions() // Права по умолчанию
            }
            
            return try {
                val jsonObj = JSONObject(json)
                UserPermissions(
                    canAccessClients = jsonObj.optBoolean("canAccessClients", true),
                    canAccessTemplates = jsonObj.optBoolean("canAccessTemplates", true),
                    canAccessReports = jsonObj.optBoolean("canAccessReports", true),
                    canAccessMaintenance = jsonObj.optBoolean("canAccessMaintenance", true),
                    canAccessSettings = jsonObj.optBoolean("canAccessSettings", true),
                    startScreen = jsonObj.optString("startScreen", "clients")
                )
            } catch (e: Exception) {
                UserPermissions() // В случае ошибки - права по умолчанию
            }
        }
    }
}

