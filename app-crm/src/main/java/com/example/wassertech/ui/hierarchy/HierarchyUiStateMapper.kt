package ru.wassertech.ui.hierarchy

import ru.wassertech.data.entities.*
import ru.wassertech.core.screens.hierarchy.ui.*
import ru.wassertech.data.repository.IconRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Вспомогательные функции для преобразования Entity в UI State для shared-экранов.
 * Используется в app-crm для интеграции с core:screens.
 */
object HierarchyUiStateMapper {
    
    /**
     * Преобразует SiteEntity в SiteItemUi.
     * Для CRM все права доступа устанавливаются в true (ADMIN/ENGINEER имеют полный доступ).
     */
    suspend fun SiteEntity.toSiteItemUi(
        iconRepository: IconRepository,
        icon: IconEntity? = null
    ): SiteItemUi {
        val localImagePath = icon?.id?.let { 
            withContext(Dispatchers.IO) { 
                iconRepository.getLocalIconPath(it) 
            } 
        }
        
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
            // В CRM все права доступа разрешены
            canEdit = true,
            canDelete = true,
            canChangeIcon = true,
            canReorder = true
        )
    }
    
    /**
     * Преобразует InstallationEntity в InstallationItemUi.
     */
    suspend fun InstallationEntity.toInstallationItemUi(
        iconRepository: IconRepository,
        icon: IconEntity? = null
    ): InstallationItemUi {
        val localImagePath = icon?.id?.let { 
            withContext(Dispatchers.IO) { 
                iconRepository.getLocalIconPath(it) 
            } 
        }
        
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
            // В CRM все права доступа разрешены
            canEdit = true,
            canDelete = true,
            canChangeIcon = true,
            canReorder = true,
            canStartMaintenance = true, // В CRM можно проводить ТО
            canViewMaintenanceHistory = true
        )
    }
    
    /**
     * Преобразует ComponentEntity в ComponentItemUi.
     */
    suspend fun ComponentEntity.toComponentItemUi(
        iconRepository: IconRepository,
        icon: IconEntity? = null,
        templateName: String? = null
    ): ComponentItemUi {
        val localImagePath = icon?.id?.let { 
            withContext(Dispatchers.IO) { 
                iconRepository.getLocalIconPath(it) 
            } 
        }
        
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
            // В CRM все права доступа разрешены
            canEdit = true,
            canDelete = true,
            canChangeIcon = true,
            canReorder = true
        )
    }
}

