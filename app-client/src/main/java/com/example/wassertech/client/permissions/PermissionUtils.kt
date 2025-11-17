package ru.wassertech.client.permissions

import ru.wassertech.client.data.entities.*
import ru.wassertech.core.auth.LocalPermissionChecker
import ru.wassertech.core.auth.UserSession

/**
 * Утилиты для проверки прав доступа к сущностям в app-client.
 * 
 * Обёртки над LocalPermissionChecker для удобства использования в UI.
 * Все проверки делегируются в LocalPermissionChecker из core:auth.
 */

/**
 * Проверяет, может ли пользователь редактировать объект.
 */
fun canEditSite(user: UserSession?, site: SiteEntity): Boolean {
    if (user == null) return false
    val siteClientId = site.effectiveOwnerClientId()
    return LocalPermissionChecker.canEditSite(user, siteClientId, site.getOriginType())
}

/**
 * Проверяет, может ли пользователь удалять объект.
 */
fun canDeleteSite(user: UserSession?, site: SiteEntity): Boolean {
    if (user == null) return false
    val siteClientId = site.effectiveOwnerClientId()
    return LocalPermissionChecker.canDeleteSite(user, siteClientId, site.getOriginType())
}

/**
 * Проверяет, может ли пользователь редактировать установку.
 * Для проверки нужен clientId объекта, к которому относится установка.
 */
fun canEditInstallation(user: UserSession?, installation: InstallationEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canEditInstallation(user, siteClientId, installation.getOriginType())
}

/**
 * Проверяет, может ли пользователь удалять установку.
 */
fun canDeleteInstallation(user: UserSession?, installation: InstallationEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canDeleteInstallation(user, siteClientId, installation.getOriginType())
}

/**
 * Проверяет, может ли пользователь редактировать компонент.
 * Для проверки нужен clientId объекта, к которому относится компонент (через установку).
 */
fun canEditComponent(user: UserSession?, component: ComponentEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canEditComponent(user, siteClientId, component.getOriginType())
}

/**
 * Проверяет, может ли пользователь удалять компонент.
 */
fun canDeleteComponent(user: UserSession?, component: ComponentEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canDeleteComponent(user, siteClientId, component.getOriginType())
}

/**
 * Проверяет, может ли пользователь редактировать шаблон компонента.
 */
fun canEditTemplate(user: UserSession?, template: ComponentTemplateEntity): Boolean {
    if (user == null) return false
    return LocalPermissionChecker.canEditComponentTemplate(user, template.getOriginType())
}

/**
 * Проверяет, может ли пользователь удалять шаблон компонента.
 */
fun canDeleteTemplate(user: UserSession?, template: ComponentTemplateEntity): Boolean {
    if (user == null) return false
    return LocalPermissionChecker.canDeleteComponentTemplate(user, template.getOriginType())
}

/**
 * Проверяет, может ли пользователь создавать новые сущности.
 */
fun canCreateEntity(user: UserSession?): Boolean {
    if (user == null) return false
    return LocalPermissionChecker.canCreateEntity(user)
}

/**
 * Проверяет, может ли пользователь генерировать PDF-отчёты.
 * В app-client генерация PDF отключена для CLIENT.
 */
fun canGeneratePdf(user: UserSession?): Boolean {
    if (user == null) return false
    return LocalPermissionChecker.canGeneratePdf(user)
}

/**
 * Проверяет, может ли пользователь просматривать PDF-отчёты.
 */
fun canViewPdf(user: UserSession?): Boolean {
    if (user == null) return false
    return LocalPermissionChecker.canViewPdf(user)
}

/**
 * Проверяет, может ли пользователь просматривать объект.
 */
fun canViewSite(user: UserSession?, site: SiteEntity): Boolean {
    if (user == null) return false
    val siteClientId = site.effectiveOwnerClientId()
    return LocalPermissionChecker.canViewSite(user, siteClientId)
}

/**
 * Проверяет, может ли пользователь просматривать установку.
 */
fun canViewInstallation(user: UserSession?, installation: InstallationEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canViewInstallation(user, siteClientId)
}

/**
 * Проверяет, может ли пользователь просматривать компонент.
 */
fun canViewComponent(user: UserSession?, component: ComponentEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canViewComponent(user, siteClientId)
}

/**
 * Проверяет, может ли пользователь просматривать шаблон компонента.
 */
fun canViewTemplate(user: UserSession?): Boolean {
    if (user == null) return false
    return LocalPermissionChecker.canViewComponentTemplate(user)
}

/**
 * Проверяет, может ли пользователь менять иконку объекта.
 */
fun canChangeIconForSite(user: UserSession?, site: SiteEntity): Boolean {
    if (user == null) return false
    val siteClientId = site.effectiveOwnerClientId()
    return LocalPermissionChecker.canChangeIconForSite(user, siteClientId, site.getOriginType())
}

/**
 * Проверяет, может ли пользователь менять иконку установки.
 */
fun canChangeIconForInstallation(user: UserSession?, installation: InstallationEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canChangeIconForInstallation(user, siteClientId, installation.getOriginType())
}

/**
 * Проверяет, может ли пользователь менять иконку компонента.
 */
fun canChangeIconForComponent(user: UserSession?, component: ComponentEntity, site: SiteEntity?): Boolean {
    if (user == null) return false
    val siteClientId = site?.effectiveOwnerClientId() ?: return false
    return LocalPermissionChecker.canChangeIconForComponent(user, siteClientId, component.getOriginType())
}