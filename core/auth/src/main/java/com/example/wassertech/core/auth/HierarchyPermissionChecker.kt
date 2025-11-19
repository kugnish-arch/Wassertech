package ru.wassertech.core.auth

import android.util.Log

/**
 * Простое представление записи user_membership для использования в HierarchyPermissionChecker.
 * Не зависит от Room и может использоваться в core:auth.
 * 
 * ViewModel'и должны преобразовывать UserMembershipEntity в UserMembershipInfo перед передачей в checker.
 */
data class UserMembershipInfo(
    val userId: String,
    val scope: String, // "CLIENT", "SITE", "INSTALLATION"
    val targetId: String,
    val isArchived: Boolean = false
)

/**
 * Расширенный checker для проверки прав доступа в иерархии с учётом user_membership и created_by_user_id.
 * 
 * Правила для CLIENT:
 * - Видимость сущностей определяется через user_membership (если есть записи) или через clientId
 * - Редактирование разрешено только для сущностей с created_by_user_id == currentUserId и origin = CLIENT
 * - Создание новых сущностей разрешено только если пользователь является создателем родительской сущности
 * 
 * Правила для ADMIN/ENGINEER:
 * - Полный доступ ко всем сущностям (независимо от membership и created_by_user_id)
 */
object HierarchyPermissionChecker {
    
    private const val TAG = "HierarchyPermissionChecker"
    
    // ========== Вспомогательные функции для работы с membership ==========
    
    /**
     * Проверяет, есть ли у пользователя доступ к клиенту через CLIENT-membership.
     */
    private fun List<UserMembershipInfo>.hasClientAccessToClient(
        userId: String,
        clientId: String
    ): Boolean = any {
        it.userId == userId &&
        it.scope == "CLIENT" &&
        it.targetId == clientId &&
        !it.isArchived
    }
    
    /**
     * Проверяет, есть ли у пользователя доступ к объекту через SITE-membership.
     */
    private fun List<UserMembershipInfo>.hasSiteAccess(
        userId: String,
        siteId: String
    ): Boolean = any {
        it.userId == userId &&
        it.scope == "SITE" &&
        it.targetId == siteId &&
        !it.isArchived
    }
    
    /**
     * Проверяет, есть ли у пользователя доступ к установке через INSTALLATION-membership.
     */
    private fun List<UserMembershipInfo>.hasInstallationAccess(
        userId: String,
        installationId: String
    ): Boolean = any {
        it.userId == userId &&
        it.scope == "INSTALLATION" &&
        it.targetId == installationId &&
        !it.isArchived
    }
    
    // ========== Методы проверки прав для Site (объект) ==========
    
    /**
     * Проверяет, может ли пользователь просматривать объект (Site).
     * 
     * Для CLIENT: проверяет user_membership (CLIENT или SITE scope) или clientId.
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canViewSite(
        siteId: String,
        siteClientId: String,
        currentUser: UserSession,
        memberships: List<UserMembershipInfo> = emptyList()
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> {
                // Проверяем через user_membership
                val hasClientMembership = memberships.hasClientAccessToClient(currentUser.userId, siteClientId)
                val hasSiteMembership = memberships.hasSiteAccess(currentUser.userId, siteId)
                
                // Или через clientId (fallback для обратной совместимости)
                val hasClientIdAccess = siteClientId == currentUser.clientId
                
                hasClientMembership || hasSiteMembership || hasClientIdAccess
            }
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь редактировать объект (Site).
     * 
     * Для CLIENT: только если created_by_user_id == currentUserId и origin = CLIENT.
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canEditSite(
        siteCreatedByUserId: String?,
        siteOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> {
                val isCreator = siteCreatedByUserId == currentUser.userId
                val isClientOrigin = OriginType.fromString(siteOrigin) == OriginType.CLIENT
                isCreator && isClientOrigin
            }
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь удалять объект (Site).
     * Логика совпадает с canEditSite.
     */
    fun canDeleteSite(
        siteCreatedByUserId: String?,
        siteOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return canEditSite(siteCreatedByUserId, siteOrigin, currentUser)
    }
    
    /**
     * Проверяет, может ли пользователь менять иконку объекта.
     * Логика совпадает с canEditSite.
     */
    fun canChangeIconForSite(
        siteCreatedByUserId: String?,
        siteOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return canEditSite(siteCreatedByUserId, siteOrigin, currentUser)
    }
    
    /**
     * Проверяет, может ли пользователь создавать установки в объекте.
     * 
     * Для CLIENT: только если site.created_by_user_id == currentUserId.
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canCreateInstallationUnderSite(
        siteCreatedByUserId: String?,
        currentUser: UserSession
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> siteCreatedByUserId == currentUser.userId
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
    
    // ========== Методы проверки прав для Installation (установка) ==========
    
    /**
     * Проверяет, может ли пользователь просматривать установку (Installation).
     * 
     * Для CLIENT: проверяет user_membership (CLIENT, SITE или INSTALLATION scope) или clientId через site.
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canViewInstallation(
        installationId: String,
        installationSiteId: String,
        siteClientId: String?,
        currentUser: UserSession,
        memberships: List<UserMembershipInfo> = emptyList()
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> {
                // Проверяем через user_membership
                val hasClientMembership = siteClientId?.let { 
                    memberships.hasClientAccessToClient(currentUser.userId, it) 
                } ?: false
                val hasSiteMembership = memberships.hasSiteAccess(currentUser.userId, installationSiteId)
                val hasInstallationMembership = memberships.hasInstallationAccess(currentUser.userId, installationId)
                
                // Или через clientId (fallback для обратной совместимости)
                val hasClientIdAccess = siteClientId == currentUser.clientId
                
                hasClientMembership || hasSiteMembership || hasInstallationMembership || hasClientIdAccess
            }
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь редактировать установку (Installation).
     * 
     * Для CLIENT: только если created_by_user_id == currentUserId и origin = CLIENT.
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canEditInstallation(
        installationCreatedByUserId: String?,
        installationOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> {
                val isCreator = installationCreatedByUserId == currentUser.userId
                val isClientOrigin = OriginType.fromString(installationOrigin) == OriginType.CLIENT
                isCreator && isClientOrigin
            }
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь удалять установку (Installation).
     * Логика совпадает с canEditInstallation.
     */
    fun canDeleteInstallation(
        installationCreatedByUserId: String?,
        installationOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return canEditInstallation(installationCreatedByUserId, installationOrigin, currentUser)
    }
    
    /**
     * Проверяет, может ли пользователь менять иконку установки.
     * Логика совпадает с canEditInstallation.
     */
    fun canChangeIconForInstallation(
        installationCreatedByUserId: String?,
        installationOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return canEditInstallation(installationCreatedByUserId, installationOrigin, currentUser)
    }
    
    // ========== Методы проверки прав для Component (компонент) ==========
    
    /**
     * Проверяет, может ли пользователь просматривать компонент (Component).
     * 
     * Для CLIENT: проверяет доступ через установку (canViewInstallation).
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canViewComponent(
        componentInstallationId: String,
        installationSiteId: String?,
        siteClientId: String?,
        currentUser: UserSession,
        memberships: List<UserMembershipInfo> = emptyList()
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> {
                // Компоненты доступны через установку
                canViewInstallation(
                    installationId = componentInstallationId,
                    installationSiteId = installationSiteId ?: "",
                    siteClientId = siteClientId,
                    currentUser = currentUser,
                    memberships = memberships
                )
            }
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь редактировать компонент (Component).
     * 
     * Для CLIENT: только если created_by_user_id == currentUserId и origin = CLIENT.
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canEditComponent(
        componentCreatedByUserId: String?,
        componentOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> {
                val isCreator = componentCreatedByUserId == currentUser.userId
                val isClientOrigin = OriginType.fromString(componentOrigin) == OriginType.CLIENT
                isCreator && isClientOrigin
            }
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
    
    /**
     * Проверяет, может ли пользователь удалять компонент (Component).
     * Логика совпадает с canEditComponent.
     */
    fun canDeleteComponent(
        componentCreatedByUserId: String?,
        componentOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return canEditComponent(componentCreatedByUserId, componentOrigin, currentUser)
    }
    
    /**
     * Проверяет, может ли пользователь менять иконку компонента.
     * Логика совпадает с canEditComponent.
     */
    fun canChangeIconForComponent(
        componentCreatedByUserId: String?,
        componentOrigin: String?,
        currentUser: UserSession
    ): Boolean {
        return canEditComponent(componentCreatedByUserId, componentOrigin, currentUser)
    }
    
    /**
     * Проверяет, может ли пользователь создавать компоненты в установке.
     * 
     * Для CLIENT: только если installation.created_by_user_id == currentUserId.
     * Для ADMIN/ENGINEER: всегда true.
     */
    fun canCreateComponentUnderInstallation(
        installationCreatedByUserId: String?,
        currentUser: UserSession
    ): Boolean {
        return when {
            currentUser.isAdmin() || currentUser.isEngineer() -> true
            currentUser.isClient() -> installationCreatedByUserId == currentUser.userId
            else -> {
                Log.w(TAG, "Неизвестная роль пользователя: ${currentUser.role}")
                false
            }
        }
    }
}
