package ru.wassertech.client.permissions

import ru.wassertech.client.auth.OriginType
import ru.wassertech.client.auth.UserSession
import ru.wassertech.client.data.entities.*
import ru.wassertech.core.auth.UserRole

/**
 * Утилиты для проверки прав доступа к сущностям в app-client.
 * 
 * Правила прав:
 * - Пользователь с ролью CLIENT может редактировать/удалять ТОЛЬКО сущности,
 *   которые созданы самим клиентом (origin == OriginType.CLIENT).
 * - Сущности, созданные инженером в CRM (origin == OriginType.CRM), доступны
 *   только для просмотра (read-only).
 * - Все сущности должны принадлежать текущему клиенту (ownerClientId == user.clientId).
 */

/**
 * Проверяет, может ли пользователь редактировать объект.
 * 
 * @param user Сессия текущего пользователя
 * @param site Объект для проверки
 * @return true, если пользователь может редактировать объект
 */
fun canEditSite(user: UserSession, site: SiteEntity): Boolean {
    // В app-client только пользователи с ролью CLIENT могут редактировать
    if (user.role != UserRole.CLIENT) {
        return false
    }
    
    // Объект должен принадлежать текущему клиенту
    if (site.effectiveOwnerClientId() != user.clientId) {
        return false
    }
    
    // Можно редактировать только объекты, созданные самим клиентом
    return site.getOriginType() == OriginType.CLIENT
}

/**
 * Проверяет, может ли пользователь удалять объект.
 */
fun canDeleteSite(user: UserSession, site: SiteEntity): Boolean {
    // Те же правила, что и для редактирования
    return canEditSite(user, site)
}

/**
 * Проверяет, может ли пользователь редактировать установку.
 */
fun canEditInstallation(user: UserSession, installation: InstallationEntity): Boolean {
    if (user.role != UserRole.CLIENT) {
        return false
    }
    
    // TODO: После добавления поля ownerClientId в InstallationEntity использовать его
    // Временно проверяем через site (нужно получить site по installation.siteId)
    // Для упрощения временно разрешаем редактирование, если origin == CLIENT
    // В реальной реализации нужно проверить ownerClientId через связанный site
    
    // Можно редактировать только установки, созданные самим клиентом
    return installation.getOriginType() == OriginType.CLIENT
}

/**
 * Проверяет, может ли пользователь удалять установку.
 */
fun canDeleteInstallation(user: UserSession, installation: InstallationEntity): Boolean {
    return canEditInstallation(user, installation)
}

/**
 * Проверяет, может ли пользователь редактировать компонент.
 */
fun canEditComponent(user: UserSession, component: ComponentEntity): Boolean {
    if (user.role != UserRole.CLIENT) {
        return false
    }
    
    // TODO: После добавления поля ownerClientId в ComponentEntity использовать его
    // Временно проверяем только origin
    
    // Можно редактировать только компоненты, созданные самим клиентом
    return component.getOriginType() == OriginType.CLIENT
}

/**
 * Проверяет, может ли пользователь удалять компонент.
 */
fun canDeleteComponent(user: UserSession, component: ComponentEntity): Boolean {
    return canEditComponent(user, component)
}

/**
 * Проверяет, может ли пользователь редактировать шаблон компонента.
 */
fun canEditTemplate(user: UserSession, template: ComponentTemplateEntity): Boolean {
    if (user.role != UserRole.CLIENT) {
        return false
    }
    
    // TODO: После добавления поля ownerClientId в ComponentTemplateEntity использовать его
    // Временно проверяем только origin
    
    // Можно редактировать только шаблоны, созданные самим клиентом
    return template.getOriginType() == OriginType.CLIENT
}

/**
 * Проверяет, может ли пользователь удалять шаблон компонента.
 */
fun canDeleteTemplate(user: UserSession, template: ComponentTemplateEntity): Boolean {
    return canEditTemplate(user, template)
}

/**
 * Проверяет, может ли пользователь создавать новые сущности.
 * В app-client клиент всегда может создавать свои сущности (они будут помечены как CLIENT).
 */
fun canCreateEntity(user: UserSession): Boolean {
    // В app-client только пользователи с ролью CLIENT могут создавать сущности
    return user.role == UserRole.CLIENT
}

/**
 * Проверяет, может ли пользователь генерировать PDF-отчёты.
 * В app-client генерация PDF отключена.
 */
fun canGeneratePdf(user: UserSession): Boolean {
    return false // Генерация PDF недоступна в app-client
}

/**
 * Проверяет, может ли пользователь просматривать PDF-отчёты.
 * Клиент может просматривать уже сгенерированные отчёты.
 */
fun canViewPdf(user: UserSession): Boolean {
    return user.role == UserRole.CLIENT
}

/**
 * Проверяет, может ли пользователь просматривать сущность (read-only доступ).
 * Все сущности, принадлежащие клиенту, можно просматривать.
 */
fun canViewSite(user: UserSession, site: SiteEntity): Boolean {
    if (user.role != UserRole.CLIENT) {
        return false
    }
    
    // Можно просматривать объекты, принадлежащие текущему клиенту
    return site.effectiveOwnerClientId() == user.clientId
}

fun canViewInstallation(user: UserSession, installation: InstallationEntity): Boolean {
    // TODO: После добавления ownerClientId проверять его
    // Временно разрешаем просмотр всех установок для клиента
    return user.role == UserRole.CLIENT
}

fun canViewComponent(user: UserSession, component: ComponentEntity): Boolean {
    // TODO: После добавления ownerClientId проверять его
    return user.role == UserRole.CLIENT
}

fun canViewTemplate(user: UserSession, template: ComponentTemplateEntity): Boolean {
    // TODO: После добавления ownerClientId проверять его
    return user.role == UserRole.CLIENT
}
