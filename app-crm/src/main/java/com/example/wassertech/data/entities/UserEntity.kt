package com.example.wassertech.data.entities

import com.example.wassertech.auth.UserRole
import com.example.wassertech.auth.UserPermissions

/**
 * Entity для пользователей системы.
 * Хранится только в удаленной MySQL БД, не в локальной Room БД.
 */
data class UserEntity(
    val id: String,
    val login: String, // Уникальное поле
    val password: String, // Хранится в открытом виде (для простоты, в будущем можно добавить хеширование)
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String = UserRole.USER.name, // Роль пользователя (ADMIN, USER, VIEWER)
    val permissions: String? = null, // JSON строка с правами доступа (UserPermissions)
    val lastLoginAtEpoch: Long? = null, // Время последнего входа в приложение
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
) {
    /**
     * Получает объект прав доступа из JSON строки
     */
    fun getPermissions(): UserPermissions {
        return UserPermissions.fromJson(permissions)
    }
    
    /**
     * Получает роль пользователя
     */
    fun getUserRole(): UserRole {
        return UserRole.fromString(role)
    }
}

