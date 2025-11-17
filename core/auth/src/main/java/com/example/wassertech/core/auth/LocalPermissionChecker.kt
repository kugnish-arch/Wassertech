package ru.wassertech.core.auth

import android.util.Log

/**
 * Локальная утилита для проверки прав доступа на основе ролей и владения данными.
 * Повторяет логику серверного AccessControlHelper для работы в оффлайн-режиме.
 * 
 * Правила:
 * - ADMIN и ENGINEER могут редактировать/удалять все сущности
 * - CLIENT может редактировать/удалять только сущности с origin = CLIENT и принадлежащие его клиенту
 * - CLIENT может просматривать все сущности своего клиента (включая origin = CRM)
 */
object LocalPermissionChecker {
    
    private const val TAG = "LocalPermissionChecker"
    
    /**
     * Проверяет, может ли пользователь просматривать объект.
     */
    fun canViewSite(user: UserSession, siteClientId: String): Boolean {
        return when {
            user.isAdmin() || user.isEngineer() -> true
            user.isClient() -> siteClientId == user.clientId
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${user.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь редактировать объект.
     */
    fun canEditSite(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return when {
            user.isAdmin() || user.isEngineer() -> true
            user.isClient() -> siteClientId == user.clientId && origin == OriginType.CLIENT
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${user.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь удалять объект.
     */
    fun canDeleteSite(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditSite(user, siteClientId, origin)
    }
    
    /**
     * Проверяет, может ли пользователь просматривать установку.
     * Для проверки нужен clientId объекта, к которому относится установка.
     */
    fun canViewInstallation(user: UserSession, siteClientId: String): Boolean {
        return canViewSite(user, siteClientId)
    }
    
    /**
     * Проверяет, может ли пользователь редактировать установку.
     */
    fun canEditInstallation(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditSite(user, siteClientId, origin)
    }
    
    /**
     * Проверяет, может ли пользователь удалять установку.
     */
    fun canDeleteInstallation(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditInstallation(user, siteClientId, origin)
    }
    
    /**
     * Проверяет, может ли пользователь просматривать компонент.
     * Для проверки нужен clientId объекта, к которому относится компонент (через установку).
     */
    fun canViewComponent(user: UserSession, siteClientId: String): Boolean {
        return canViewSite(user, siteClientId)
    }
    
    /**
     * Проверяет, может ли пользователь редактировать компонент.
     */
    fun canEditComponent(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditSite(user, siteClientId, origin)
    }
    
    /**
     * Проверяет, может ли пользователь удалять компонент.
     */
    fun canDeleteComponent(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditComponent(user, siteClientId, origin)
    }
    
    /**
     * Проверяет, может ли пользователь просматривать шаблон компонента.
     * Шаблоны глобальные, доступны всем ролям для просмотра.
     */
    fun canViewComponentTemplate(user: UserSession): Boolean {
        return true // Шаблоны доступны всем для просмотра
    }
    
    /**
     * Проверяет, может ли пользователь редактировать шаблон компонента.
     */
    fun canEditComponentTemplate(user: UserSession, origin: OriginType): Boolean {
        return when {
            user.isAdmin() || user.isEngineer() -> true
            user.isClient() -> origin == OriginType.CLIENT
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${user.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь удалять шаблон компонента.
     */
    fun canDeleteComponentTemplate(user: UserSession, origin: OriginType): Boolean {
        return when {
            user.isAdmin() -> true // Только ADMIN может удалять шаблоны
            user.isEngineer() -> origin == OriginType.CRM // ENGINEER может удалять только CRM-шаблоны
            user.isClient() -> origin == OriginType.CLIENT // CLIENT может удалять только свои шаблоны
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${user.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь просматривать сессию ТО.
     */
    fun canViewMaintenanceSession(user: UserSession, siteClientId: String): Boolean {
        return canViewSite(user, siteClientId)
    }
    
    /**
     * Проверяет, может ли пользователь редактировать сессию ТО.
     * CLIENT не может редактировать сессии ТО (только просмотр).
     */
    fun canEditMaintenanceSession(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return when {
            user.isAdmin() || user.isEngineer() -> true
            user.isClient() -> false // CLIENT не может редактировать сессии ТО
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${user.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь создавать сессию ТО.
     * CLIENT не может создавать сессии ТО.
     */
    fun canCreateMaintenanceSession(user: UserSession): Boolean {
        return when {
            user.isAdmin() || user.isEngineer() -> true
            user.isClient() -> false // CLIENT не может создавать сессии ТО
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${user.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь удалять сессию ТО.
     * Только ADMIN может удалять сессии ТО.
     */
    fun canDeleteMaintenanceSession(user: UserSession): Boolean {
        return user.isAdmin()
    }
    
    /**
     * Проверяет, может ли пользователь генерировать PDF-отчёты.
     * CLIENT не может генерировать PDF (может только просматривать существующие).
     */
    fun canGeneratePdf(user: UserSession): Boolean {
        return when {
            user.isAdmin() || user.isEngineer() -> true
            user.isClient() -> false // CLIENT не может генерировать PDF
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${user.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь просматривать PDF-отчёты.
     * Все роли могут просматривать PDF (если файл доступен).
     */
    fun canViewPdf(user: UserSession): Boolean {
        return true // Все роли могут просматривать PDF
    }
    
    /**
     * Проверяет, может ли пользователь создавать сущности.
     * Все роли могут создавать сущности (с соответствующими ограничениями по clientId).
     */
    fun canCreateEntity(user: UserSession): Boolean {
        return true // Все роли могут создавать сущности
    }
    
    /**
     * Проверяет, может ли пользователь менять иконку объекта.
     * Использует ту же логику, что и canEditSite - если пользователь может редактировать объект,
     * он может менять его иконку.
     */
    fun canChangeIconForSite(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditSite(user, siteClientId, origin)
    }
    
    /**
     * Проверяет, может ли пользователь менять иконку установки.
     * Использует ту же логику, что и canEditInstallation.
     */
    fun canChangeIconForInstallation(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditInstallation(user, siteClientId, origin)
    }
    
    /**
     * Проверяет, может ли пользователь менять иконку компонента.
     * Использует ту же логику, что и canEditComponent.
     */
    fun canChangeIconForComponent(user: UserSession, siteClientId: String, origin: OriginType): Boolean {
        return canEditComponent(user, siteClientId, origin)
    }
}

