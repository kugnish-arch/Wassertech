package ru.wassertech.client.ui.hierarchy

import ru.wassertech.client.data.entities.*
import ru.wassertech.core.screens.hierarchy.ui.*
import ru.wassertech.core.auth.*
import ru.wassertech.client.data.repository.IconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Mapper для преобразования Entity в UI State для shared-экранов в app-client.
 * Учитывает права доступа через HierarchyPermissionChecker и user_membership.
 */
object ClientHierarchyUiStateMapper {
    
    /**
     * Преобразует SiteEntity в SiteItemUi с учётом прав доступа.
     */
    suspend fun SiteEntity.toSiteItemUi(
        iconRepository: IconRepository,
        currentUser: UserSession,
        memberships: List<UserMembershipInfo>,
        icon: IconEntity? = null
    ): SiteItemUi? {
        // Проверяем, может ли пользователь просматривать объект
        if (!HierarchyPermissionChecker.canViewSite(
            siteId = this.id,
            siteClientId = this.clientId,
            currentUser = currentUser,
            memberships = memberships
        )) {
            return null // Не включаем в список, если нет прав на просмотр
        }
        
        val localImagePath = icon?.id?.let { 
            withContext(Dispatchers.IO) { 
                iconRepository.getLocalIconPath(it) 
            } 
        }
        
        // Проверяем права доступа
        val canEdit = HierarchyPermissionChecker.canEditSite(
            siteCreatedByUserId = this.createdByUserId,
            siteOrigin = this.origin,
            currentUser = currentUser
        )
        val canDelete = HierarchyPermissionChecker.canDeleteSite(
            siteCreatedByUserId = this.createdByUserId,
            siteOrigin = this.origin,
            currentUser = currentUser
        )
        val canChangeIcon = HierarchyPermissionChecker.canChangeIconForSite(
            siteCreatedByUserId = this.createdByUserId,
            siteOrigin = this.origin,
            currentUser = currentUser
        )
        val canCreateInstallation = HierarchyPermissionChecker.canCreateInstallationUnderSite(
            siteCreatedByUserId = this.createdByUserId,
            currentUser = currentUser
        )
        
        return SiteItemUi(
            id = this.id,
            name = this.name,
            address = this.address,
            iconId = this.iconId,
            iconAndroidResName = icon?.androidResName,
            iconCode = icon?.code,
            iconLocalImagePath = localImagePath,
            isArchived = this.isArchived,
            clientId = this.clientId,
            origin = this.origin,
            createdByUserId = this.createdByUserId,
            canEdit = canEdit,
            canDelete = canDelete,
            canChangeIcon = canChangeIcon,
            canReorder = canEdit // Можно переупорядочивать только если можно редактировать
        )
    }
    
    /**
     * Преобразует InstallationEntity в InstallationItemUi с учётом прав доступа.
     */
    suspend fun InstallationEntity.toInstallationItemUi(
        iconRepository: IconRepository,
        currentUser: UserSession,
        memberships: List<UserMembershipInfo>,
        site: SiteEntity?,
        icon: IconEntity? = null
    ): InstallationItemUi? {
        // Проверяем, может ли пользователь просматривать установку
        val siteClientId = site?.clientId ?: ""
        if (!HierarchyPermissionChecker.canViewInstallation(
            installationId = this.id,
            installationSiteId = this.siteId,
            siteClientId = siteClientId,
            currentUser = currentUser,
            memberships = memberships
        )) {
            return null // Не включаем в список, если нет прав на просмотр
        }
        
        val localImagePath = icon?.id?.let { 
            withContext(Dispatchers.IO) { 
                iconRepository.getLocalIconPath(it) 
            } 
        }
        
        // Проверяем права доступа
        val canEdit = HierarchyPermissionChecker.canEditInstallation(
            installationCreatedByUserId = this.createdByUserId,
            installationOrigin = this.origin,
            currentUser = currentUser
        )
        val canDelete = HierarchyPermissionChecker.canDeleteInstallation(
            installationCreatedByUserId = this.createdByUserId,
            installationOrigin = this.origin,
            currentUser = currentUser
        )
        val canChangeIcon = HierarchyPermissionChecker.canChangeIconForInstallation(
            installationCreatedByUserId = this.createdByUserId,
            installationOrigin = this.origin,
            currentUser = currentUser
        )
        val canCreateComponent = HierarchyPermissionChecker.canCreateComponentUnderInstallation(
            installationCreatedByUserId = this.createdByUserId,
            currentUser = currentUser
        )
        
        return InstallationItemUi(
            id = this.id,
            name = this.name,
            iconId = this.iconId,
            iconAndroidResName = icon?.androidResName,
            iconCode = icon?.code,
            iconLocalImagePath = localImagePath,
            isArchived = this.isArchived,
            siteId = this.siteId,
            origin = this.origin,
            createdByUserId = this.createdByUserId,
            canEdit = canEdit,
            canDelete = canDelete,
            canChangeIcon = canChangeIcon,
            canReorder = canEdit,
            canStartMaintenance = canEdit, // Можно проводить ТО только если можно редактировать
            canViewMaintenanceHistory = true // История ТО доступна всем, кто видит установку
        )
    }
    
    /**
     * Преобразует ComponentEntity в ComponentItemUi с учётом прав доступа.
     */
    suspend fun ComponentEntity.toComponentItemUi(
        iconRepository: IconRepository,
        currentUser: UserSession,
        memberships: List<UserMembershipInfo>,
        installation: InstallationEntity?,
        site: SiteEntity?,
        icon: IconEntity? = null,
        templateName: String? = null
    ): ComponentItemUi? {
        // Проверяем, может ли пользователь просматривать компонент
        val siteClientId = site?.clientId ?: ""
        val installationSiteId = installation?.siteId ?: ""
        if (!HierarchyPermissionChecker.canViewComponent(
            componentInstallationId = this.installationId,
            installationSiteId = installationSiteId,
            siteClientId = siteClientId,
            currentUser = currentUser,
            memberships = memberships
        )) {
            return null // Не включаем в список, если нет прав на просмотр
        }
        
        val localImagePath = icon?.id?.let { 
            withContext(Dispatchers.IO) { 
                iconRepository.getLocalIconPath(it) 
            } 
        }
        
        // Проверяем права доступа
        val canEdit = HierarchyPermissionChecker.canEditComponent(
            componentCreatedByUserId = this.createdByUserId,
            componentOrigin = this.origin,
            currentUser = currentUser
        )
        val canDelete = HierarchyPermissionChecker.canDeleteComponent(
            componentCreatedByUserId = this.createdByUserId,
            componentOrigin = this.origin,
            currentUser = currentUser
        )
        val canChangeIcon = HierarchyPermissionChecker.canChangeIconForComponent(
            componentCreatedByUserId = this.createdByUserId,
            componentOrigin = this.origin,
            currentUser = currentUser
        )
        
        return ComponentItemUi(
            id = this.id,
            name = this.name,
            type = this.type.name,
            templateName = templateName,
            iconId = this.iconId,
            iconAndroidResName = icon?.androidResName,
            iconCode = icon?.code,
            iconLocalImagePath = localImagePath,
            isArchived = this.isArchived,
            installationId = this.installationId,
            origin = this.origin,
            createdByUserId = this.createdByUserId,
            canEdit = canEdit,
            canDelete = canDelete,
            canChangeIcon = canChangeIcon,
            canReorder = canEdit
        )
    }
}

