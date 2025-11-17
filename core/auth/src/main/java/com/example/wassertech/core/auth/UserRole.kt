package ru.wassertech.core.auth

import org.json.JSONObject

/**
 * Роли пользователей в системе
 */
/**
 * Роли пользователей в системе.
 * Соответствуют серверным ролям: ADMIN, ENGINEER, CLIENT.
 * USER и VIEWER оставлены для обратной совместимости, но на сервере используются только ADMIN, ENGINEER, CLIENT.
 */
enum class UserRole(val displayName: String, val serverValue: String) {
    ADMIN("Администратор", "ADMIN"),
    ENGINEER("Инженер", "ENGINEER"),
    USER("Пользователь", "ENGINEER"), // Для обратной совместимости, маппится на ENGINEER
    VIEWER("Наблюдатель", "ENGINEER"), // Для обратной совместимости, маппится на ENGINEER
    CLIENT("Клиент", "CLIENT");
    
    companion object {
        /**
         * Преобразует строковое значение роли в enum.
         * Поддерживает как серверные значения (ADMIN, ENGINEER, CLIENT), так и старые (USER, VIEWER).
         * По умолчанию возвращает ENGINEER (безопаснее, чем ADMIN).
         */
        fun fromString(value: String?): UserRole {
            return try {
                if (value == null) ENGINEER else {
                    when (value.uppercase()) {
                        "ADMIN" -> ADMIN
                        "ENGINEER" -> ENGINEER
                        "CLIENT" -> CLIENT
                        "USER" -> USER // Старое значение, маппится на ENGINEER на сервере
                        "VIEWER" -> VIEWER // Старое значение, маппится на ENGINEER на сервере
                        else -> {
                            // Логируем неожиданное значение, но возвращаем ENGINEER
                            android.util.Log.w("UserRole", "Неизвестная роль: $value, используем ENGINEER")
                            ENGINEER
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRole", "Ошибка парсинга роли: $value", e)
                ENGINEER // По умолчанию ENGINEER (безопаснее, чем ADMIN)
            }
        }
        
        /**
         * Возвращает серверное значение роли (для отправки на сервер).
         * USER и VIEWER преобразуются в ENGINEER.
         */
        fun UserRole.toServerValue(): String {
            return when (this) {
                ADMIN -> "ADMIN"
                ENGINEER -> "ENGINEER"
                USER -> "ENGINEER" // Маппинг на ENGINEER
                VIEWER -> "ENGINEER" // Маппинг на ENGINEER
                CLIENT -> "CLIENT"
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

